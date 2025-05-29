package dev.vality.webhook.dispatcher.service;

import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.config.IgnoreHttpErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Map;

//import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcherServiceImpl implements WebhookDispatcherService {

    private final RestClient restClient;
    private final IgnoreHttpErrorHandler ignoreHttpErrorHandler;

    @Override
    public int dispatch(WebhookMessage webhookMessage) {
        try {
            long executionTimeStart = System.currentTimeMillis();
            MediaType contentType = MediaType.parseMediaType(webhookMessage.getContentType());
            URI uri = URI.create(webhookMessage.getUrl());
            ResponseEntity<String> response = restClient.post()
                    .uri(uri)
                    .contentType(contentType)
                    .headers(httpHeaders -> httpHeaders.addAll(buildHeaders(webhookMessage.getAdditionalHeaders())))
                    .body(webhookMessage.getRequestBody())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ignoreHttpErrorHandler)
                    .toEntity(String.class);
            long executionTimeEnd = System.currentTimeMillis();
            HttpStatusCode responseStatusCode = response.getStatusCode();
            log.info("Response from hook: sourceId: {}, eventId: {}, code: {}; executionTimeMs: {} body: {}",
                    webhookMessage.getSourceId(),
                    webhookMessage.getEventId(),
                    responseStatusCode,
                    executionTimeEnd - executionTimeStart,
                    response.getBody());
            if (responseStatusCode.is2xxSuccessful()) {
                return response.getStatusCode().value();
            } else {
                log.warn("Timeout error when send webhook: {} statusCode: {}", webhookMessage.getSourceId(),
                        responseStatusCode);
                throw new RetryableException(HttpStatus.REQUEST_TIMEOUT.getReasonPhrase());
            }
        } catch (RestClientException e) {
            log.warn("Timeout error when send webhook: {}, errorMessage: {}", webhookMessage, e.getMessage());
            throw new RetryableException(e);

        }
    }

    private LinkedMultiValueMap<String, String> buildHeaders(Map<String, String> headers) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        headers.forEach(map::add);
        return map;
    }
}
