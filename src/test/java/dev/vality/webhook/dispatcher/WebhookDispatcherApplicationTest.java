package dev.vality.webhook.dispatcher;

import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.webhook.dispatcher.config.KafkaTest;
import dev.vality.webhook.dispatcher.config.PostgresSpingBootITest;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wiremock.spring.EnableWireMock;

import java.time.Instant;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KafkaTest
@PostgresSpingBootITest
@EnableWireMock
public class WebhookDispatcherApplicationTest {

    @Value("${wiremock.server.baseUrl}")
    private String wireMockUrl;

    @Value("${kafka.topic.webhook.forward}")
    private String forwardTopicName;
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
        webhook.setUrl(wireMockUrl);
        webhook.setContentType(APPLICATION_JSON);
        webhook.setRequestBody("\\{\\}".getBytes());
        webhook.setEventId(eventId);
        webhook.setAdditionalHeaders(new HashMap<>());
        webhook.setParentEventId(-1);
        return webhook;
    }

}
