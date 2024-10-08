package dev.vality.webhook.dispatcher.service;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcherServiceImpl implements WebhookDispatcherService {

    private final CloseableHttpClient client;

    @Override
    public int dispatch(WebhookMessage webhookMessage) {
        HttpPost post = new HttpPost(webhookMessage.getUrl());
        post.setEntity(new ByteArrayEntity(webhookMessage.getRequestBody()));
        post.setHeader(CONTENT_TYPE, webhookMessage.getContentType());
        webhookMessage.getAdditionalHeaders().forEach(post::addHeader);
        long executionTimeStart = System.currentTimeMillis();
        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            log.info("Response from hook: sourceId: {}, eventId: {}, code: {}; executionTimeMs: {} body: {}",
                    webhookMessage.getSourceId(),
                    webhookMessage.getEventId(),
                    statusCode,
                    executionTimeStart - System.currentTimeMillis(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            if (httpStatus != null && HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                return statusCode;
            } else {
                log.warn("Timeout error when send webhook: {} statusCode: {} reason: {}", webhookMessage.getSourceId(),
                        statusCode,
                        response.getStatusLine().getReasonPhrase());
                throw new RetryableException(HttpStatus.REQUEST_TIMEOUT.getReasonPhrase());
            }
        } catch (IOException e) {
            log.warn("Timeout error when send webhook: {} ", webhookMessage, e);
            throw new RetryableException(e);
        }
    }

}
