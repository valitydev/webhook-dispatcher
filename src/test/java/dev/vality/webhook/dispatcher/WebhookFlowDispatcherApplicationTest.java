package dev.vality.webhook.dispatcher;

import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.webhook.dispatcher.config.PostgresSpingBootITest;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@KafkaTestcontainerSingleton(
        properties = {"merchant.timeout=1", "retry.first.seconds=1",
                "retry.second.seconds=2", "retry.third.seconds=3",
                "retry.last.seconds=4", "retry.dead.time.hours=20"},
        topicsKeys = {"kafka.topic.webhook.forward", "kafka.topic.webhook.first.retry",
                "kafka.topic.webhook.second.retry", "kafka.topic.webhook.third.retry",
                "kafka.topic.webhook.last.retry", "kafka.topic.webhook.dead.letter.queue"})
@KafkaConfig
@PostgresSpingBootITest
class WebhookFlowDispatcherApplicationTest {

    @Value("${kafka.topic.webhook.forward}")
    private String forwardTopicName;

    private static final String APPLICATION_JSON = "application/json";
    private static final String SOURCE_ID = "23";

    @MockitoBean
    private WebhookDispatcherService webhookDispatcherService;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenCreatedTimeout() throws InterruptedException, IOException {
        when(webhookDispatcherService.dispatch(any())).thenReturn(200);

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);

        //check duplicates
        testThriftKafkaProducer.send(forwardTopicName, webhook);
        testThriftKafkaProducer.send(forwardTopicName, webhook);
        Thread.sleep(5000L);
        verify(webhookDispatcherService, times(1)).dispatch(any());

        //check waiting parent
        Mockito.clearInvocations(webhookDispatcherService);
        webhook.setParentEventId(0);
        webhook.setEventId(1);
        webhook.setSourceId(SOURCE_ID);
        testThriftKafkaProducer.send(forwardTopicName, webhook);

        Thread.sleep(5000L);
        verify(webhookDispatcherService, times(0)).dispatch(any());
        webhook.setParentEventId(-1);
        webhook.setEventId(0);
        testThriftKafkaProducer.send(forwardTopicName, webhook);

        Thread.sleep(5000L);

        verify(webhookDispatcherService, times(2)).dispatch(any());
    }

    private WebhookMessage createWebhook(String sourceId, String createdAt, long eventId) {
        WebhookMessage webhook = new WebhookMessage();
        webhook.setSourceId(sourceId);
        webhook.setCreatedAt(createdAt);
        webhook.setUrl("http://localhost:8080");
        webhook.setContentType(APPLICATION_JSON);
        webhook.setRequestBody("\\{\\}".getBytes());
        webhook.setEventId(eventId);
        webhook.setAdditionalHeaders(new HashMap<>());
        webhook.setParentEventId(-1);
        return webhook;
    }

}
