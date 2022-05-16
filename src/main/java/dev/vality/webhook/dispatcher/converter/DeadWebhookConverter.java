package dev.vality.webhook.dispatcher.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.entity.DeadWebhookEntity;
import dev.vality.webhook.dispatcher.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeadWebhookConverter {

    private final ObjectMapper objectMapper;

    public DeadWebhookEntity toEntity(WebhookMessage webhookMessage) {
        return DeadWebhookEntity.builder()
                .id(IdGenerator.generate(
                        webhookMessage.getWebhookId(),
                        webhookMessage.getSourceId(),
                        webhookMessage.getEventId()))
                .webhookId(webhookMessage.getWebhookId())
                .sourceId(webhookMessage.getSourceId())
                .eventId(webhookMessage.getEventId())
                .parentEventId(webhookMessage.getParentEventId())
                .createdAt(TypeUtil.stringToLocalDateTime(webhookMessage.getCreatedAt()))
                .url(webhookMessage.getUrl())
                .contentType(webhookMessage.getContentType())
                .additionalHeaders(toJson(webhookMessage.getAdditionalHeaders()))
                .requestBody(webhookMessage.getRequestBody())
                .retryCount(webhookMessage.getRetryCount())
                .build();
    }

    public WebhookMessage toDomain(DeadWebhookEntity entity) {
        return new WebhookMessage()
                .setWebhookId(entity.getWebhookId())
                .setSourceId(entity.getSourceId())
                .setEventId(entity.getEventId())
                .setParentEventId(entity.getParentEventId())
                .setCreatedAt(TypeUtil.temporalToString(entity.getCreatedAt()))
                .setUrl(entity.getUrl())
                .setContentType(entity.getContentType())
                .setAdditionalHeaders(toMap(entity.getAdditionalHeaders()))
                .setRequestBody(entity.getRequestBody())
                .setRetryCount(Optional.ofNullable(entity.getRetryCount()).orElse(0L));
    }

    @SneakyThrows
    private String toJson(Map<String, String> map) {
        return objectMapper.writeValueAsString(map);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Map<String, String> toMap(String json) {
        return objectMapper.readValue(json, Map.class);
    }
}
