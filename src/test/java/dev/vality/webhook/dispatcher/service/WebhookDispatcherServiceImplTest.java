package dev.vality.webhook.dispatcher.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import dev.vality.kafka.common.exception.RetryableException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class WebhookDispatcherServiceImplTest {

    private static final String ALG_RS_256 = "alg=RS256";
    private static final String DIGEST =
            "oOg_wGfM3esi5aAmu4fnik6DRISvusM2r99i7iyQapkL_5Q30riAD6jSr9LOearJea6053JjodQ7v" +
                    "VIPsTDb1pnZ4thSe7qLU_JzyL_q-LCQXWyGVBXpIyt5fN-1yRNr-Bl1hpnmc5JpNWuNvZdqpoPkvrW4vaNUmLgX" +
                    "qgtpgyHIxQDMZVLnAmz" +
                    "XBCvWggqORPpZ_6J1oNbh1QqEBC9CqDU94d8GthzqxH3V7nIPdpYmg8VxbR9k5SGXf8zbIDWxWMzVfKQF4B1B1CtO" +
                    "46loD70cmOX2kMl32" +
                    "WJa_XSV8Ep1ajDnouLyxk4eN-F-Fb1XkUWUJPw0JkKAVhp2F4NxzQ==";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    private WebhookDispatcherServiceImpl webhookDispatcherService;

    @Before
    public void init() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(1000).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        webhookDispatcherService = new WebhookDispatcherServiceImpl(httpClient);
    }


    @Test(expected = RetryableException.class)
    public void dispatchError() {
        stubFor(post(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(20000)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl("http://localhost:8089/test");
        webhookMessage.setRequestBody("{}".getBytes());
        webhookMessage.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        HashMap<String, String> additionalHeaders = new HashMap<>();
        webhookMessage.setAdditionalHeaders(additionalHeaders);
        webhookDispatcherService.dispatch(webhookMessage);
    }

    @Test
    public void dispatch() {
        stubFor(post(urlEqualTo("/test"))
                .withHeader(CONTENT_TYPE, new EqualToPattern(ContentType.APPLICATION_JSON.getMimeType()))
                .withHeader("Content-Signature", new EqualToPattern(ALG_RS_256))
                .withHeader("digest", new EqualToPattern(DIGEST))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"test\"}")));

        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setUrl("http://localhost:8089/test");
        webhookMessage.setRequestBody("{}".getBytes());
        webhookMessage.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        HashMap<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Content-Signature", ALG_RS_256);
        additionalHeaders.put("digest", DIGEST);
        webhookMessage.setAdditionalHeaders(additionalHeaders);
        webhookDispatcherService.dispatch(webhookMessage);
    }
}
