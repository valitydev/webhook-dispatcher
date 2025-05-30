package dev.vality.webhook.dispatcher;

import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.webhook.dispatcher.config.KafkaTest;
import dev.vality.webhook.dispatcher.config.PostgresSpingBootITest;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@KafkaTest
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
    void listenCreatedDuplicate() throws IOException {
        when(webhookDispatcherService.dispatch(any())).thenReturn(200);
        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);

        //check duplicates
        testThriftKafkaProducer.send(forwardTopicName, webhook);
        testThriftKafkaProducer.send(forwardTopicName, webhook);

        verify(webhookDispatcherService, timeout(5000L).times(1)).dispatch(any());
    }

    @Test
    void listenCreatedWaitingParent() throws IOException {
        when(webhookDispatcherService.dispatch(any())).thenReturn(200);
        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);
        webhook.setParentEventId(0);
        webhook.setEventId(1);
        webhook.setSourceId(SOURCE_ID);

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        verify(webhookDispatcherService, timeout(5000L).times(0)).dispatch(any());

        webhook.setParentEventId(-1);
        webhook.setEventId(0);

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        verify(webhookDispatcherService, timeout(15000L).times(2)).dispatch(any());
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
