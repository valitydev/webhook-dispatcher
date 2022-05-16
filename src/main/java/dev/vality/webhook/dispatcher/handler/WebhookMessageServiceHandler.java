package dev.vality.webhook.dispatcher.handler;

import dev.vality.webhook.dispatcher.WebhookMessageServiceSrv;
import dev.vality.webhook.dispatcher.service.WebhookMessageService;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookMessageServiceHandler implements WebhookMessageServiceSrv.Iface {

    private final WebhookMessageService webhookMessageService;

    @Override
    public void resend(
            long webhookId,
            String sourceId,
            long eventId) throws TException {
        webhookMessageService.resend(webhookId, sourceId, eventId);
    }
}
