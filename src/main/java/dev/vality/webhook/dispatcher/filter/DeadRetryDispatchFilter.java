package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import dev.vality.webhook.dispatcher.utils.TimeoutUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DeadRetryDispatchFilter implements DispatchFilter {

    private final WebhookDao webhookDao;

    @Value("${retry.dead.time.hours:24}")
    private int deadRetryTimeout;

    @Override
    public boolean filter(WebhookMessage webhookMessage) {
        return TimeoutUtils.calculateTimeFromCreated(webhookMessage.getCreatedAt()) >
                TimeUnit.HOURS.toMillis(deadRetryTimeout)
                || webhookDao.isCommitted(webhookMessage);
    }
}
