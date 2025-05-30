package dev.vality.webhook.dispatcher;

import dev.vality.kafka.common.exception.RetryableException;
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
class WebhookRetryDispatcherApplicationTest {

    @Value("${kafka.topic.webhook.forward}")
    private String forwardTopicName;

    private static final String URL = "http://localhost:8089";
    private static final String APPLICATION_JSON = "application/json";

    @MockitoBean
    private WebhookDispatcherService webhookDispatcherService;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenCreatedTimeout() throws IOException {
        when(webhookDispatcherService.dispatch(any())).thenThrow(RetryableException.class);

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        verify(webhookDispatcherService, timeout(90000L).atLeast(6)).dispatch(any());
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
