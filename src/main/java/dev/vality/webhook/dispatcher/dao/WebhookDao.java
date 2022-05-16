package dev.vality.webhook.dispatcher.dao;

import dev.vality.webhook.dispatcher.WebhookMessage;

public interface WebhookDao {

    void commit(WebhookMessage webhookMessage);

    void bury(WebhookMessage webhookMessage);

    Boolean isParentCommitted(WebhookMessage webhookMessage);

    Boolean isCommitted(WebhookMessage webhookMessage);

}
