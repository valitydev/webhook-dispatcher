package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;

public interface DispatchFilter {

    boolean filter(WebhookMessage t);

}
