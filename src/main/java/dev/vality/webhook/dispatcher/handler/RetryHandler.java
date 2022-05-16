package dev.vality.webhook.dispatcher.handler;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.filter.TimeDispatchFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryHandler {

    public static final long WAITING_PERIOD = 500L;
    private final WebhookHandlerImpl handler;
    private final TimeDispatchFilter timeDispatchFilter;

    public void handle(
            String topic,
            Acknowledgment acknowledgment,
            ConsumerRecord<String, WebhookMessage> consumerRecord,
            Long timeout) {
        WebhookMessage webhookMessage = consumerRecord.value();
        if (timeDispatchFilter.filter(webhookMessage, timeout)) {
            handler.handle(topic, webhookMessage);
            acknowledgment.acknowledge();
        } else {
            try {
                acknowledgment.nack(WAITING_PERIOD);
                log.debug("Waiting timeout: {}", timeout);
            } catch (Exception e) {
                log.warn("Exception during seek aware", e);
            }
        }
    }

}
