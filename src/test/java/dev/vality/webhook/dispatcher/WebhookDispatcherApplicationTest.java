package dev.vality.webhook.dispatcher;

import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.time.Instant;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KafkaTestcontainer(
        properties = {"merchant.timeout=1", "kafka.topic.concurrency.forward=1"},
        topicsKeys = {"kafka.topic.webhook.forward", "kafka.topic.webhook.first.retry",
                "kafka.topic.webhook.second.retry", "kafka.topic.webhook.third.retry",
                "kafka.topic.webhook.last.retry", "kafka.topic.webhook.dead.letter.queue"})
@KafkaSpringBootTest
@PostgresqlTestcontainerSingleton
@AutoConfigureWireMock(port = 8089)
public class WebhookDispatcherApplicationTest {

    @Value("${kafka.topic.webhook.forward}")
    private String forwardTopicName;

    public static final String URL = "http://localhost:8089";
    public static final String APPLICATION_JSON = "application/json";
    @Autowired
    private WebhookDao webhookDao;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenCreatedTimeout() throws InterruptedException {
        String response = "{}";
        stubFor(
                post(urlEqualTo("/"))
                        .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                        .willReturn(aResponse().withFixedDelay(15000)
                                .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                                .withBody(response)));

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);
        webhook.setRequestBody("{\"test\":\"test\"}".getBytes());

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        Thread.sleep(4500L);

        stubFor(
                post(urlEqualTo("/"))
                        .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                                .withBody(response)));

        webhook = createWebhook(sourceId, Instant.now().toString(), 1);
        webhook.setParentEventId(1);

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        Thread.sleep(4500L);

        assertFalse(webhookDao.isParentCommitted(webhook));

    }

    private WebhookMessage createWebhook(String sourceId, String createdAt, long eventId) {
        WebhookMessage webhook = new WebhookMessage();
        webhook.setSourceId(sourceId);
        webhook.setCreatedAt(createdAt);
        webhook.setUrl(URL);
        webhook.setContentType(APPLICATION_JSON);
        webhook.setRequestBody("\\{\\}".getBytes());
        webhook.setEventId(eventId);
        webhook.setAdditionalHeaders(new HashMap<>());
        webhook.setParentEventId(-1);
        return webhook;
    }

}
