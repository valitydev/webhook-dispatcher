package dev.vality.webhook.dispatcher.handler;

import dev.vality.webhook.dispatcher.WebhookMessage;

public interface WebhookHandler {

    void handle(String postponedTopic, WebhookMessage webhookMessage);

}
