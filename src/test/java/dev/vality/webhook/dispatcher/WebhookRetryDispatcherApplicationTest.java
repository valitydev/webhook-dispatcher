package dev.vality.webhook.dispatcher;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WebhookDispatcherApplication.class)
@TestPropertySource(properties = {
        "merchant.timeout=1",
        "retry.first.seconds=1",
        "retry.second.seconds=2",
        "retry.third.seconds=3",
        "retry.last.seconds=4",
        "retry.dead.time.hours=1"
})
public class WebhookRetryDispatcherApplicationTest extends AbstractKafkaIntegrationTest {

    private static final String URL = "http://localhost:8089";
    private static final String APPLICATION_JSON = "application/json";

    @MockBean
    private WebhookDispatcherService webhookDispatcherService;

    @Test
    public void listenCreatedTimeout() throws ExecutionException, InterruptedException, IOException {
        when(webhookDispatcherService.dispatch(any())).thenThrow(RetryableException.class);

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);
        ProducerRecord producerRecord = new ProducerRecord<>(Initializer.WEBHOOK_FORWARD, webhook.source_id, webhook);
        Producer<String, WebhookMessage> producer = createProducer();

        producer.send(producerRecord).get();
        producer.close();

        Thread.sleep(20000L);

        verify(webhookDispatcherService, times(7)).dispatch(any());
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
