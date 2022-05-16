package dev.vality.webhook.dispatcher;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WebhookDispatcherApplication.class)
@TestPropertySource(properties = "merchant.timeout=1")
public class WebhookDispatcherApplicationTest extends AbstractKafkaIntegrationTest {

    public static final String URL = "http://localhost:8089";
    public static final String APPLICATION_JSON = "application/json";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    @Autowired
    private WebhookDao webhookDao;

    @Test
    public void listenCreatedTimeout() throws ExecutionException, InterruptedException {
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

        ProducerRecord producerRecord = new ProducerRecord<>(Initializer.WEBHOOK_FORWARD, webhook.source_id, webhook);
        Producer<String, WebhookMessage> producer = createProducer();

        producer.send(producerRecord).get();
        producer.close();

        Thread.sleep(4500L);

        stubFor(
                post(urlEqualTo("/"))
                        .withHeader("Content-Type", equalTo(APPLICATION_JSON))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                                .withBody(response)));

        producer = createProducer();

        webhook = createWebhook(sourceId, Instant.now().toString(), 1);
        webhook.setParentEventId(1);
        producerRecord = new ProducerRecord<>(Initializer.WEBHOOK_FORWARD, webhook.source_id, webhook);
        producer.send(producerRecord).get();
        producer.close();

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
