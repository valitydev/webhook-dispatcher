package dev.vality.webhook.dispatcher.filter;

import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.dao.WebhookDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeadRetryDispatchFilter.class)
public class DeadRetryDispatchFilterTest {

    @MockBean
    private WebhookDao webhookDao;

    @Autowired
    private DeadRetryDispatchFilter dispatchFilter;

    @Test
    public void filter() {
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
