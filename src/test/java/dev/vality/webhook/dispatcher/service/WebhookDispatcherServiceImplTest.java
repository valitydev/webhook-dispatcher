package dev.vality.webhook.dispatcher.service;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.config.PostgresSpingBootITest;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wiremock.spring.EnableWireMock;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@EnableWireMock
@PostgresSpingBootITest
class WebhookDispatcherServiceImplTest {

    @Value("${wiremock.server.baseUrl}")
    private String wireMockUrl;

    private static final String ALG_RS_256 = "alg=RS256";
    private static final String DIGEST =
            "oOg_wGfM3esi5aAmu4fnik6DRISvusM2r99i7iyQapkL_5Q30riAD6jSr9LOearJea6053JjodQ7v" +
                    "VIPsTDb1pnZ4thSe7qLU_JzyL_q-LCQXWyGVBXpIyt5fN-1yRNr-Bl1hpnmc5JpNWuNvZdqpoPkvrW4vaNUmLgX" +
                    "qgtpgyHIxQDMZVLnAmz" +
                    "XBCvWggqORPpZ_6J1oNbh1QqEBC9CqDU94d8GthzqxH3V7nIPdpYmg8VxbR9k5SGXf8zbIDWxWMzVfKQF4B1B1CtO" +
                    "46loD70cmOX2kMl32" +
                    "WJa_XSV8Ep1ajDnouLyxk4eN-F-Fb1XkUWUJPw0JkKAVhp2F4NxzQ==";

    @Autowired
    private WebhookDispatcherService webhookDispatcherService;

    @Test
    void dispatchErrorWithTimeout() {
        int timeoutInMillis = 20000;
        stubFor(post(urlPathEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(timeoutInMillis)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl(wireMockUrl + "/test");
        webhookMessage.setRequestBody("{}".getBytes());
        webhookMessage.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        HashMap<String, String> additionalHeaders = new HashMap<>();
        webhookMessage.setAdditionalHeaders(additionalHeaders);
        assertThrows(RetryableException.class, () -> webhookDispatcherService.dispatch(webhookMessage));
    }

    @Test
    void dispatchSuccess() {
        stubFor(post(urlPathEqualTo("/test"))
                .withHeader(CONTENT_TYPE, new EqualToPattern(ContentType.APPLICATION_JSON.getMimeType()))
                .withHeader("Content-Signature", new EqualToPattern(ALG_RS_256))
                .withHeader("digest", new EqualToPattern(DIGEST))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"test\"}")));

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl(wireMockUrl + "/test");
        webhookMessage.setRequestBody("{}".getBytes());
        webhookMessage.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        HashMap<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Content-Signature", ALG_RS_256);
        additionalHeaders.put("digest", DIGEST);
        webhookMessage.setAdditionalHeaders(additionalHeaders);
        assertDoesNotThrow(() -> webhookDispatcherService.dispatch(webhookMessage));
    }
}
