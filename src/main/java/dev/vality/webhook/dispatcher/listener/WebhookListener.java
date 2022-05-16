package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.handler.WebhookHandler;
import dev.vality.webhook.dispatcher.utils.WebhookLogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookListener {

    private final WebhookHandler handler;

    @Value("${kafka.topic.webhook.first.retry}")
    private String postponedTopic;

    @KafkaListener(topics = "${kafka.topic.webhook.forward}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(WebhookMessage webhookMessage, Acknowledgment acknowledgment) {
        WebhookLogUtils.info("WebhookListener", webhookMessage);
        handler.handle(postponedTopic, webhookMessage);
        acknowledgment.acknowledge();
    }

}
