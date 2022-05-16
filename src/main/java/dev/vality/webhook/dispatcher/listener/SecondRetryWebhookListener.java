package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.handler.RetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecondRetryWebhookListener
        implements AcknowledgingMessageListener<String, WebhookMessage>, ConsumerSeekAware {

    private final RetryHandler handler;
    @Value("${retry.second.seconds}")
    private long timeout;
    @Value("${retry.first.seconds}")
    private long prevTimeout;
    @Value("${kafka.topic.webhook.third.retry}")
    private String postponedTopic;

    @KafkaListener(topics = "${kafka.topic.webhook.second.retry}",
            containerFactory = "kafkaSecondRetryListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, WebhookMessage> consumerRecord, Acknowledgment acknowledgment) {
        handler.handle(postponedTopic, acknowledgment, consumerRecord, timeout + prevTimeout);
    }

}
