package dev.vality.webhook.dispatcher.handler;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.filter.TimeDispatchFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryHandler {

    @Value("${retry.nack.seconds}")
    private long waitingPeriod;
    private final WebhookHandler handler;
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
                acknowledgment.nack(Duration.of(waitingPeriod, ChronoUnit.SECONDS));
                log.debug("Waiting timeout: {}", timeout);
            } catch (Exception e) {
                log.warn("Exception during seek aware", e);
            }
        }
    }

}
