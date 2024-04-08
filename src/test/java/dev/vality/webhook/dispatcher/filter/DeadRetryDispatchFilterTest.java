package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DeadRetryDispatchFilter.class)
class DeadRetryDispatchFilterTest {

    @MockBean
    private WebhookDao webhookDao;

    @Autowired
    private DeadRetryDispatchFilter dispatchFilter;

    @Test
    void filter() {
        WebhookMessage webhookMessage = new WebhookMessage();
        webhookMessage.setCreatedAt(Instant.now().minusSeconds(60 * 60 * 24 + 1).toString());
        when(webhookDao.isCommitted(webhookMessage)).thenReturn(true);
        boolean filter = dispatchFilter.filter(webhookMessage);

        assertTrue(filter);

        webhookMessage = new WebhookMessage();
        webhookMessage.setCreatedAt(Instant.now().minusSeconds(60 * 60 * 24 + 1).toString());
        when(webhookDao.isCommitted(webhookMessage)).thenReturn(false);
        filter = dispatchFilter.filter(webhookMessage);

        assertTrue(filter);

        webhookMessage = new WebhookMessage();
        webhookMessage.setCreatedAt(Instant.now().minusSeconds(60 * 60 * 23).toString());
        when(webhookDao.isCommitted(webhookMessage)).thenReturn(false);
        filter = dispatchFilter.filter(webhookMessage);

        assertFalse(filter);
    }
}
