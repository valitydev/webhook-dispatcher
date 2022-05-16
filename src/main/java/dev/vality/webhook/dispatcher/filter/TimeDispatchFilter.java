package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.utils.TimeoutUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TimeDispatchFilter {

    public boolean filter(WebhookMessage webhookMessage, long timeout) {
        return TimeoutUtils.calculateTimeFromCreated(webhookMessage.getCreatedAt()) >
                TimeUnit.SECONDS.toMillis(timeout);
    }

}
