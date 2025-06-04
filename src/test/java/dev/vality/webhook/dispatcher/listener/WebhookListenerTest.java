package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.config.IgnoreHttpErrorHandler;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import dev.vality.webhook.dispatcher.filter.DeadRetryDispatchFilter;
import dev.vality.webhook.dispatcher.filter.PostponedDispatchFilter;
import dev.vality.webhook.dispatcher.handler.WebhookHandlerImpl;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookListenerTest {

    private WebhookListener webhookListener;

    @Mock
    private WebhookDao webhookDao;
    @Mock
    private KafkaTemplate<String, WebhookMessage> kafkaTemplate;
    @Mock
    private Acknowledgment acknowledgment;
    @Mock
    private CompletableFuture<SendResult<String, WebhookMessage>> result;

    @Mock
    private RestClient client;

    @BeforeEach
    void init() {
        WebhookDispatcherService webhookDispatcherService =
                new WebhookDispatcherServiceImpl(client, new IgnoreHttpErrorHandler());

        DeadRetryDispatchFilter deadRetryDispatchFilter = new DeadRetryDispatchFilter(webhookDao);
        ReflectionTestUtils.setField(deadRetryDispatchFilter, "deadRetryTimeout", 24);
        webhookListener = new WebhookListener(
                new WebhookHandlerImpl(
                        webhookDispatcherService,
                        new PostponedDispatchFilter(webhookDao),
                        deadRetryDispatchFilter, webhookDao, kafkaTemplate));
    }

    @Test
    void listenAndDispatchWebhook() {
        when(webhookDao.isCommitted(any())).thenReturn(false);
        mockRestClient();

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl("https://localhost:8080");
        webhookMessage.setCreatedAt(Instant.now().toString());
        webhookMessage.setSourceId("547839");
        HashMap<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Content-Signature", "alg=RS256");
        additionalHeaders
                .put("digest", "AKIYypDF5jWuNT4aO6OvsWNuzS7e1ztUEVmLwSwaq2Q4j2ckwVJxxz6L1nFQbWZr9Bh8p-hkuKf7Mh" +
                        "KZlOKLkhClzDseW-GpJpyhrGnzFHFO78dxbjB8Z82zC5CVJk8PZa-ZxZ2MvoQWTAsPPWVXxJ64A7_tgYiIrSZkjy" +
                        "ROwraj1-MG0iRA" +
                        "_a9bkXiwRelNj8mZIv38PneVPl1UAwpMaGs7pQmwaBv-M64Jm8rTd80WiRdOkp8G_hwPQdFo9lOhOxtUk9K5SoBfjKX" +
                        "Q0Dku7X2TpK" +
                        "fTQxHfB1mqm9L8DkK0NXopowqtZI4UB7TTFUIOOSI3SGpv3hyC2uYeTvnJWw==");
        webhookMessage.setAdditionalHeaders(additionalHeaders);
        webhookMessage.setContentType("application/json");
        webhookMessage.setParentEventId(-1L);
        webhookMessage.setEventId(5189508L);
        webhookMessage.setWebhookId(1L);
        webhookMessage.setRequestBody("ok".getBytes());

        webhookListener.listen(webhookMessage, acknowledgment);
        verify(webhookDao).isCommitted(any(WebhookMessage.class));
        verify(webhookDao).commit(any(WebhookMessage.class));
    }

    private void mockRestClient() {
        RestClient.RequestBodyUriSpec requestUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(client.post()).thenReturn(requestUriSpec);
        when(requestUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(org.springframework.http.MediaType.class)))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(byte[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok("{\"response\" : {\"status\" : \"ok\"  }}"));
    }
}
