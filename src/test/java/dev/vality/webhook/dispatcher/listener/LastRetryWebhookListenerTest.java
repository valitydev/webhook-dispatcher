package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import dev.vality.webhook.dispatcher.filter.DeadRetryDispatchFilter;
import dev.vality.webhook.dispatcher.filter.PostponedDispatchFilter;
import dev.vality.webhook.dispatcher.filter.TimeDispatchFilter;
import dev.vality.webhook.dispatcher.handler.RetryHandler;
import dev.vality.webhook.dispatcher.handler.WebhookHandlerImpl;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LastRetryWebhookListenerTest {

    private static final long DEFAULT_TIMEOUT = 1L;
    private static final String TOPIC = "test";
    private static final String SOURCE_ID = "test";

    private RetryHandler retryHandler;

    @Mock
    private TimeDispatchFilter timeDispatchFilter;
    @Mock
    private WebhookDispatcherService webhookDispatcherService;
    @Mock
    private DeadRetryDispatchFilter deadRetryDispatchFilter;
    @Mock
    private PostponedDispatchFilter postponedDispatchFilter;
    @Mock
    private KafkaTemplate<String, WebhookMessage> kafkaTemplate;
    @Mock
    private Acknowledgment acknowledgment;
    @Mock
    private WebhookDao webhookDao;
    @Mock
    private ConsumerSeekAware.ConsumerSeekCallback consumerSeekCallback;

    @BeforeEach
    void init() {
        WebhookHandlerImpl webhookHandler = new WebhookHandlerImpl(
                webhookDispatcherService,
                postponedDispatchFilter,
                deadRetryDispatchFilter,
                webhookDao,
                kafkaTemplate);

        this.retryHandler = new RetryHandler(webhookHandler, timeDispatchFilter);
    }

    @Test
    void listen() throws IOException {
        LastRetryWebhookListener lastRetryWebhookListener = new LastRetryWebhookListener(TOPIC, DEFAULT_TIMEOUT,
                DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, retryHandler);
        lastRetryWebhookListener.registerSeekCallback(consumerSeekCallback);

        WebhookMessage webhookMessage = new WebhookMessage()
                .setSourceId(SOURCE_ID);
        when(timeDispatchFilter.filter(webhookMessage, 4L)).thenReturn(true);
        when(deadRetryDispatchFilter.filter(webhookMessage)).thenReturn(false);
        when(postponedDispatchFilter.filter(webhookMessage)).thenReturn(false);
        doNothing().when(webhookDao).commit(webhookMessage);

        ConsumerRecord<String, WebhookMessage> consumerRecord = new ConsumerRecord<>(
                "key", 0, 0, "d", webhookMessage);
        lastRetryWebhookListener.onMessage(consumerRecord, acknowledgment);

        assertEquals(1L, webhookMessage.getRetryCount());
        verify(webhookDispatcherService, times(1)).dispatch(any());
        verify(acknowledgment, times(1)).acknowledge();

        when(timeDispatchFilter.filter(webhookMessage, 4L)).thenReturn(false);

        ConsumerRecord<String, WebhookMessage> consumerRecord1 = new ConsumerRecord<>(
                "key", 0, 0, "d", webhookMessage);
        lastRetryWebhookListener.onMessage(consumerRecord1, acknowledgment);
    }

}
