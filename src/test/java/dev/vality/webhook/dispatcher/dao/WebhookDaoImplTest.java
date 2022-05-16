package dev.vality.webhook.dispatcher.dao;

import dev.vality.webhook.dispatcher.WebhookDispatcherApplication;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.repository.DeadWebhookRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WebhookDispatcherApplication.class)
public class WebhookDaoImplTest extends DaoTestBase {

    @Autowired
    private WebhookDao webhookDao;

    @Autowired
    private DeadWebhookRepository deadWebhookRepository;

    @Test
    public void shouldCommitWebhooks() {
        // Given
        WebhookMessage webhook = new WebhookMessage();
        webhook.setSourceId("test");
        webhook.setEventId(1);
        webhook.setUrl("/test");
        webhook.setParentEventId(-1);

        // When
        webhookDao.commit(webhook);

        // Then
        webhook.setParentEventId(1);
        boolean commitParent = webhookDao.isParentCommitted(webhook);
        assertTrue(commitParent);

        boolean commit = webhookDao.isCommitted(webhook);
        assertTrue(commit);

        webhook.setEventId(666L);
        assertFalse(webhookDao.isCommitted(webhook));
    }

    @Test
    public void shouldBuryWebhooks() {
        // Given
        WebhookMessage webhook = new WebhookMessage();
        webhook.setWebhookId(0L);
        webhook.setSourceId("source");
        webhook.setEventId(1L);
        webhook.setParentEventId(2L);
        webhook.setCreatedAt("2016-03-22T06:12:27Z");
        webhook.setUrl("/url");
        webhook.setContentType("contentType");
        webhook.setAdditionalHeaders(Map.of("a", "b"));
        webhook.setRequestBody("body".getBytes());
        webhook.setRetryCount(3L);

        // When
        webhookDao.bury(webhook);

        // Then
        assertTrue(deadWebhookRepository.findById("0_source_1").isPresent());
    }
}
