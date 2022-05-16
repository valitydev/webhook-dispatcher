package dev.vality.webhook.dispatcher.utils;

import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class WebhookLogUtils {

    public static final String WEBHOOK_TEMPLATE =
            " sourceId: {} eventId: {} webhookId: {} parentEventId: {} createdAt: {} retryCount: {}  \n" +
                    "url: {} \n" +
                    "headers: {} \n" +
                    "message: {} ";

    public static void info(String message, WebhookMessage webhookMessage) {
        log.info(message + WEBHOOK_TEMPLATE,
                webhookMessage.getSourceId(),
                webhookMessage.getEventId(),
                webhookMessage.getWebhookId(),
                webhookMessage.getParentEventId(),
                webhookMessage.getCreatedAt(),
                webhookMessage.getRetryCount(),
                webhookMessage.getUrl(),
                webhookMessage.getAdditionalHeaders(),
                webhookMessage.getRequestBody() != null
                        ? new String(webhookMessage.getRequestBody(), StandardCharsets.UTF_8)
                        : "");
    }

    public static void debug(String message, WebhookMessage webhookMessage) {
        log.debug(message + WEBHOOK_TEMPLATE,
                webhookMessage.getSourceId(),
                webhookMessage.getEventId(),
                webhookMessage.getWebhookId(),
                webhookMessage.getParentEventId(),
                webhookMessage.getCreatedAt(),
                webhookMessage.getRetryCount(),
                webhookMessage.getUrl(),
                webhookMessage.getAdditionalHeaders(),
                webhookMessage.getRequestBody() != null
                        ? new String(webhookMessage.getRequestBody(), StandardCharsets.UTF_8)
                        : "");
    }

    public static void warn(String message, WebhookMessage webhookMessage) {
        log.debug(message + WEBHOOK_TEMPLATE,
                webhookMessage.getSourceId(),
                webhookMessage.getEventId(),
                webhookMessage.getWebhookId(),
                webhookMessage.getParentEventId(),
                webhookMessage.getCreatedAt(),
                webhookMessage.getRetryCount(),
                webhookMessage.getUrl(),
                webhookMessage.getAdditionalHeaders(),
                webhookMessage.getRequestBody() != null
                        ? new String(webhookMessage.getRequestBody(), StandardCharsets.UTF_8)
                        : "");
    }

}
