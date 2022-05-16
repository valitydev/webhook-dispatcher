package dev.vality.webhook.dispatcher.service;


import dev.vality.webhook.dispatcher.WebhookMessage;

import java.io.IOException;

public interface WebhookDispatcherService {

    int dispatch(WebhookMessage webhookMessage) throws IOException;

}
