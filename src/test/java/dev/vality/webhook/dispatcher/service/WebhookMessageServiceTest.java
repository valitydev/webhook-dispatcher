package dev.vality.webhook.dispatcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.webhook.dispatcher.WebhookNotFound;
import dev.vality.webhook.dispatcher.config.PostgresqlSpringBootITest;
import dev.vality.webhook.dispatcher.entity.DeadWebhookEntity;
import dev.vality.webhook.dispatcher.repository.DeadWebhookRepository;
import dev.vality.webhook.dispatcher.utils.IdGenerator;
import org.apache.http.entity.ContentType;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.time.LocalDateTime;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureWireMock(port = 8089)
@PostgresqlSpringBootITest
class WebhookMessageServiceTest {

    @Autowired
    private WebhookMessageService webhookMessageService;

    @Autowired
    private DeadWebhookRepository deadWebhookRepository;

    @Test
    void emptyResend() {
        assertThrows(WebhookNotFound.class, () ->
                webhookMessageService.resend(
                        1L,
                        "sourceId",
                        1L)
        );
    }

    @Test
    void failedResend() throws Exception {
        stubFor(post(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));
        DeadWebhookEntity deadWebhook = new DeadWebhookEntity();
        deadWebhook.setWebhookId(1L);
        deadWebhook.setEventId(2L);
        deadWebhook.setSourceId("sourceId");
        deadWebhook.setUrl("http://localhost:8089/test");
        deadWebhook.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        deadWebhook.setParentEventId(-1L);
        deadWebhook.setAdditionalHeaders(new ObjectMapper().writeValueAsString(new HashMap<>()));
        deadWebhook.setId(IdGenerator.generate(
                deadWebhook.getWebhookId(),
                deadWebhook.getSourceId(),
                deadWebhook.getEventId()));
        deadWebhook.setRequestBody("Test".getBytes());
        deadWebhook.setCreatedAt(LocalDateTime.now());
        deadWebhookRepository.save(deadWebhook);

        assertThrows(TException.class, () ->
                webhookMessageService.resend(
                        deadWebhook.getWebhookId(),
                        deadWebhook.getSourceId(),
                        deadWebhook.getEventId())
        );

        assertTrue(deadWebhookRepository.findByWebhookIdAndSourceIdAndEventId(
                deadWebhook.getWebhookId(),
                deadWebhook.getSourceId(),
                deadWebhook.getEventId()
        ).isPresent());
    }

    @Test
    void successResend() throws Exception {
        stubFor(post(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));
        DeadWebhookEntity deadWebhook = new DeadWebhookEntity();
        deadWebhook.setWebhookId(1L);
        deadWebhook.setEventId(2L);
        deadWebhook.setSourceId("sourceId");
        deadWebhook.setUrl("http://localhost:8089/test");
        deadWebhook.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        deadWebhook.setParentEventId(-1L);
        deadWebhook.setAdditionalHeaders(new ObjectMapper().writeValueAsString(new HashMap<>()));
        deadWebhook.setId(IdGenerator.generate(
                deadWebhook.getWebhookId(),
                deadWebhook.getSourceId(),
                deadWebhook.getEventId()));
        deadWebhook.setRequestBody("Test".getBytes());
        deadWebhook.setCreatedAt(LocalDateTime.now());
        deadWebhookRepository.save(deadWebhook);

        assertDoesNotThrow(() ->
                webhookMessageService.resend(
                        deadWebhook.getWebhookId(),
                        deadWebhook.getSourceId(),
                        deadWebhook.getEventId())
        );

        assertTrue(deadWebhookRepository.findByWebhookIdAndSourceIdAndEventId(
                deadWebhook.getWebhookId(),
                deadWebhook.getSourceId(),
                deadWebhook.getEventId()
        ).isEmpty());
    }
}