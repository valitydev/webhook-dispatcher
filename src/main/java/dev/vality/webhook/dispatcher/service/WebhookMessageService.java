package dev.vality.webhook.dispatcher.service;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.WebhookNotFound;
import dev.vality.webhook.dispatcher.converter.DeadWebhookConverter;
import dev.vality.webhook.dispatcher.entity.DeadWebhookEntity;
import dev.vality.webhook.dispatcher.repository.DeadWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookMessageService {

    private final DeadWebhookRepository deadWebhookRepository;
    private final DeadWebhookConverter deadWebhookConverter;
    private final WebhookDispatcherService webhookDispatcherService;

    public void resend(
            long webhookId,
            String sourceId,
            long eventId) throws TException {
        Optional<DeadWebhookEntity> webhook = deadWebhookRepository.findByWebhookIdAndSourceIdAndEventId(
                webhookId,
                sourceId,
                eventId);
        if (webhook.isEmpty()) {
            log.warn("No dead webhook was found for webhookId={}, sourceId={} and eventId={}",
                    webhookId, sourceId, eventId);
            throw new WebhookNotFound();
        }
        WebhookMessage webhookMessage = deadWebhookConverter.toDomain(webhook.get());
        try {
            log.info("Try to resending webhook with webhookId={}, sourceId={} and eventId={} to url={}",
                    webhookId, sourceId, eventId, webhookMessage.getUrl());
            webhookDispatcherService.dispatch(webhookMessage);
            log.info("Successfully resending webhook with webhookId={}, sourceId={} and eventId={}",
                    webhookId, sourceId, eventId);
            deadWebhookRepository.deleteByWebhookIdAndSourceIdAndEventId(webhookId, sourceId, eventId);
            log.debug("Delete webhook with webhookId={}, sourceId={} and eventId={} from the dead",
                    webhookId, sourceId, eventId);
        } catch (Exception e) {
            log.error("Failed resending webhook with webhookId={}, sourceId={} and eventId={}",
                    webhookId, sourceId, eventId, e);
            throw new TException(e);
        }
    }
}
