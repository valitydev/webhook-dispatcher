package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.handler.RetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirstRetryWebhookListener implements AcknowledgingMessageListener<String, WebhookMessage> {

    private final RetryHandler handler;
    @Value("${retry.first.seconds}")
    private long timeout;
    @Value("${kafka.topic.webhook.second.retry}")
    private String postponedTopic;

    @KafkaListener(topics = "${kafka.topic.webhook.first.retry}",
            containerFactory = "kafkaRetryListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, WebhookMessage> consumerRecord, Acknowledgment acknowledgment) {
        handler.handle(postponedTopic, acknowledgment, consumerRecord, timeout);
    }

}
