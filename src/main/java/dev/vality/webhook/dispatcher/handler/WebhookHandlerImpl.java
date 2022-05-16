package dev.vality.webhook.dispatcher.handler;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import dev.vality.webhook.dispatcher.filter.DispatchFilter;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static dev.vality.webhook.dispatcher.utils.WebhookLogUtils.info;
import static dev.vality.webhook.dispatcher.utils.WebhookLogUtils.warn;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookHandlerImpl implements WebhookHandler {

    private final WebhookDispatcherService webhookDispatcherService;
    private final DispatchFilter postponedDispatchFilter;
    private final DispatchFilter deadRetryDispatchFilter;
    private final WebhookDao webhookDao;
    private final KafkaTemplate<String, WebhookMessage> kafkaTemplate;

    @Override
    public void handle(String postponedTopic, WebhookMessage webhookMessage) {
        try {
            if (deadRetryDispatchFilter.filter(webhookMessage)) {
                warn("Retry time has ended for", webhookMessage);
                webhookDao.bury(webhookMessage);
            } else if (postponedDispatchFilter.filter(webhookMessage)) {
                long retryCount = webhookMessage.getRetryCount();
                webhookMessage.setRetryCount(++retryCount);
                info("Resend to topic: " + postponedTopic, webhookMessage);
                kafkaTemplate.send(postponedTopic, webhookMessage.source_id, webhookMessage).get();
            } else {
                long retryCount = webhookMessage.getRetryCount();
                webhookMessage.setRetryCount(++retryCount);
                info("Dispatch", webhookMessage);
                webhookDispatcherService.dispatch(webhookMessage);
                webhookDao.commit(webhookMessage);
            }
        } catch (RetryableException e) {
            log.warn("RetryableException during webhook handling", e);
            syncSendMessage(postponedTopic, webhookMessage);
            info("Send to retry topic: " + postponedTopic, webhookMessage);
        } catch (Exception e) {
            log.error("Exception during webhook handling", e);
            throw new RuntimeException("Exception during webhook handling", e);
        }
    }

    private void syncSendMessage(String postponedTopic, WebhookMessage webhookMessage) {
        try {
            kafkaTemplate.send(postponedTopic, webhookMessage.source_id, webhookMessage).get();
        } catch (Exception e) {
            throw new RuntimeException("Problem with kafkaTemplate send message!", e);
        }
    }

}
