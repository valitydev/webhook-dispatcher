package dev.vality.webhook.dispatcher.listener;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import dev.vality.webhook.dispatcher.filter.DeadRetryDispatchFilter;
import dev.vality.webhook.dispatcher.filter.PostponedDispatchFilter;
import dev.vality.webhook.dispatcher.handler.WebhookHandlerImpl;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherService;
import dev.vality.webhook.dispatcher.service.WebhookDispatcherServiceImpl;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookListenerTest {

    private WebhookListener webhookListener;

    @Mock
    private WebhookDao webhookDao;
    @Mock
    private KafkaTemplate<String, WebhookMessage> kafkaTemplate;
    @Mock
    private Acknowledgment acknowledgment;
    @Mock
    private ListenableFuture<SendResult<String, WebhookMessage>> result;

    @Mock
    private CloseableHttpClient client;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        WebhookDispatcherService webhookDispatcherService = new WebhookDispatcherServiceImpl(client);

        DeadRetryDispatchFilter deadRetryDispatchFilter = new DeadRetryDispatchFilter(webhookDao);
        ReflectionTestUtils.setField(deadRetryDispatchFilter, "deadRetryTimeout", 24);
        webhookListener = new WebhookListener(
                new WebhookHandlerImpl(
                        webhookDispatcherService,
                        new PostponedDispatchFilter(webhookDao),
                        deadRetryDispatchFilter, webhookDao, kafkaTemplate));
    }

    @Test
    void listen() throws IOException {
        when(webhookDao.isCommitted(any())).thenReturn(false);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(result);
        CloseableHttpResponse mockResponse = Mockito.mock(CloseableHttpResponse.class);
        when(client.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK"));
        HttpEntity entity = EntityBuilder.create().setContentEncoding("utf-8")
                .setContentType(ContentType.APPLICATION_JSON)
                .setText("{\"response\" : {\"status\" : \"ok\"  }}").build();
        when(mockResponse.getEntity()).thenReturn(entity);

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl("https://webhook.site/e312eefc-54fc-4bca-928e-26f0fc95fc80");
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
        webhookMessage.setRequestBody("{}".getBytes());

        webhookListener.listen(webhookMessage, acknowledgment);
        verify(webhookDao).isCommitted(any(WebhookMessage.class));
        verify(webhookDao).commit(any(WebhookMessage.class));
    }
}
