package dev.vality.webhook.dispatcher.converter;

import dev.vality.geck.common.util.TypeUtil;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.entity.CommitLogEntity;
import dev.vality.webhook.dispatcher.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CommitLogConverter {

    public CommitLogEntity toEntity(WebhookMessage webhookMessage) {
        return CommitLogEntity.builder()
                .id(IdGenerator.generate(
                        webhookMessage.getWebhookId(),
                        webhookMessage.getSourceId(),
                        webhookMessage.getEventId()))
                .creationTime(TypeUtil.toLocalDateTime(Instant.now()))
                .build();
    }
}
