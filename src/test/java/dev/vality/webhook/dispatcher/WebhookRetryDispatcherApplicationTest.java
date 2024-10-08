package dev.vality.webhook.dispatcher;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@KafkaTestcontainer(
        properties = {"merchant.timeout=1", "retry.first.seconds=1",
                "retry.second.seconds=2", "retry.third.seconds=3",
                "retry.last.seconds=4", "retry.dead.time.hours=1"},
        topicsKeys = {"kafka.topic.webhook.forward", "kafka.topic.webhook.first.retry",
                "kafka.topic.webhook.second.retry", "kafka.topic.webhook.third.retry",
                "kafka.topic.webhook.last.retry", "kafka.topic.webhook.dead.letter.queue"})
@KafkaSpringBootTest
@PostgresqlTestcontainerSingleton
@AutoConfigureWireMock(port = 8089)
class WebhookRetryDispatcherApplicationTest {

    @Value("${kafka.topic.webhook.forward}")
    private String forwardTopicName;

    private static final String URL = "http://localhost:8089";
    private static final String APPLICATION_JSON = "application/json";

    @MockBean
    private WebhookDispatcherService webhookDispatcherService;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenCreatedTimeout() throws InterruptedException, IOException {
        when(webhookDispatcherService.dispatch(any())).thenThrow(RetryableException.class);

        String sourceId = "123";
        WebhookMessage webhook = createWebhook(sourceId, Instant.now().toString(), 0);

        testThriftKafkaProducer.send(forwardTopicName, webhook);

        Thread.sleep(40000L);

        verify(webhookDispatcherService, atLeast(6)).dispatch(any());
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
