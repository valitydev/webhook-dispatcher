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
public class ThirdRetryWebhookListener
        implements AcknowledgingMessageListener<String, WebhookMessage>, ConsumerSeekAware {

    private final RetryHandler handler;
    @Value("${retry.third.seconds}")
    private long timeout;
    @Value("${retry.first.seconds}")
    private long firstTimeout;
    @Value("${retry.second.seconds}")
    private long secondTimeout;
    @Value("${kafka.topic.webhook.last.retry}")
    private String postponedTopic;

    @KafkaListener(topics = "${kafka.topic.webhook.third.retry}",
            containerFactory = "kafkaThirdRetryListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, WebhookMessage> consumerRecord, Acknowledgment acknowledgment) {
        handler.handle(postponedTopic, acknowledgment, consumerRecord, timeout + firstTimeout + secondTimeout);
    }

}
