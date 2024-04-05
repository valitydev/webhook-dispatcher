package dev.vality.webhook.dispatcher;

import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static dev.vality.webhook.dispatcher.WebhookDispatcherApplicationTest.WEBHOOK_FORWARD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = WebhookDispatcherApplication.class)
//@TestPropertySource(properties = {
//        "merchant.timeout=1",
//        "retry.first.seconds=1",
//        "retry.second.seconds=2",
//        "retry.third.seconds=3",
//        "retry.last.seconds=4",
//        "retry.dead.time.hours=20"
//})

@KafkaTestcontainer(
        properties = {"merchant.timeout=1", "retry.first.seconds=1",
                "retry.second.seconds=2", "retry.third.seconds=3",
                "retry.last.seconds=4", "retry.dead.time.hours=20"},
        topicsKeys = {"kafka.topic.webhook.forward", "kafka.topic.webhook.first.retry",
                "kafka.topic.webhook.second.retry", "kafka.topic.webhook.third.retry",
                "kafka.topic.webhook.last.retry", "kafka.topic.webhook.dead.letter.queue"})
@KafkaSpringBootTest
@PostgresqlTestcontainerSingleton
@Import(KafkaProducerConfig.class)
//@AutoConfigureWireMock(port = 8089)
class WebhookFlowDispatcherApplicationTest {

    private static final String URL = "http://localhost:8089";
    private static final String APPLICATION_JSON = "application/json";
    private static final String SOURCE_ID = "23";

    @MockBean
    private WebhookDispatcherService webhookDispatcherService;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenCreatedTimeout() throws InterruptedException, IOException {
        when(webhookDispatcherService.dispatch(any())).thenReturn(200);

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);

        //check duplicates
        testThriftKafkaProducer.send(WEBHOOK_FORWARD, webhook);
        testThriftKafkaProducer.send(WEBHOOK_FORWARD, webhook);
        Thread.sleep(5000L);
        verify(webhookDispatcherService, times(1)).dispatch(any());

        //check waiting parent
        Mockito.clearInvocations(webhookDispatcherService);
        webhook.setParentEventId(0);
        webhook.setEventId(1);
        webhook.setSourceId(SOURCE_ID);
        testThriftKafkaProducer.send(WEBHOOK_FORWARD, webhook);

        Thread.sleep(5000L);
        verify(webhookDispatcherService, times(0)).dispatch(any());
        webhook.setParentEventId(-1);
        webhook.setEventId(0);
        testThriftKafkaProducer.send(WEBHOOK_FORWARD, webhook);

        Thread.sleep(5000L);

        verify(webhookDispatcherService, times(2)).dispatch(any());
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
