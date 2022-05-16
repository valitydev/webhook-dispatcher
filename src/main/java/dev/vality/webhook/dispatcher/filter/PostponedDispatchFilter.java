package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostponedDispatchFilter implements DispatchFilter {

    private static final long PARENT_NOT_EXIST_ID = -1;
    private final WebhookDao webhookDao;

    @Override
    public boolean filter(WebhookMessage webhookMessage) {
        return webhookMessage.getParentEventId() != PARENT_NOT_EXIST_ID
                && !webhookDao.isParentCommitted(webhookMessage);
    }
}
