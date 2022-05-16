package dev.vality.webhook.dispatcher.dao;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.converter.CommitLogConverter;
import dev.vality.webhook.dispatcher.converter.DeadWebhookConverter;
import dev.vality.webhook.dispatcher.entity.CommitLogEntity;
import dev.vality.webhook.dispatcher.entity.DeadWebhookEntity;
import dev.vality.webhook.dispatcher.repository.CommitLogRepository;
import dev.vality.webhook.dispatcher.repository.DeadWebhookRepository;
import dev.vality.webhook.dispatcher.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDaoImpl implements WebhookDao {

    private final DeadWebhookConverter deadWebhookConverter;
    private final DeadWebhookRepository deadWebhookRepository;
    private final CommitLogConverter commitLogConverter;
    private final CommitLogRepository commitLogRepository;

    @Override
    public void commit(WebhookMessage webhookMessage) {
        CommitLogEntity commitLog = commitLogConverter.toEntity(webhookMessage);

        try {
            log.info("Commit webhook with id={}", commitLog.getId());
            commitLogRepository.save(commitLog);
        } catch (Exception e) {
            log.error("Exception during committing webhook with id={}", commitLog.getId(), e);
            throw new RetryableException(e);
        }
    }

    @Override
    public void bury(WebhookMessage webhookMessage) {
        DeadWebhookEntity deadHook = deadWebhookConverter.toEntity(webhookMessage);

        try {
            log.info("Bury webhook with id={}", deadHook.getId());
            deadWebhookRepository.save(deadHook);
        } catch (Exception e) {
            log.error("Exception during burying webhook with id={}", deadHook.getId(), e);
            throw new RetryableException(e);
        }
    }

    @Override
    public Boolean isParentCommitted(WebhookMessage webhookMessage) {
        return isCommitted(IdGenerator.generate(
                webhookMessage.getWebhookId(),
                webhookMessage.getSourceId(),
                webhookMessage.getParentEventId()));
    }

    @Override
    public Boolean isCommitted(WebhookMessage webhookMessage) {
        return isCommitted(IdGenerator.generate(
                webhookMessage.getWebhookId(),
                webhookMessage.getSourceId(),
                webhookMessage.getEventId()));
    }

    private Boolean isCommitted(String id) {
        try {
            Boolean isCommitted = commitLogRepository.existsById(id);
            log.info("Webhook with id={}: isCommitted={}", id, isCommitted);
            return isCommitted;
        } catch (Exception e) {
            log.error("Exception during looking for parent event with id={}", id, e);
            throw new RetryableException(e);
        }
    }
}
