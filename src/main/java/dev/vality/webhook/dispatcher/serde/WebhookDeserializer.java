package dev.vality.webhook.dispatcher.serde;

import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.webhook.dispatcher.WebhookMessage;

public class WebhookDeserializer extends AbstractThriftDeserializer<WebhookMessage> {

    @Override
    public WebhookMessage deserialize(String s, byte[] bytes) {
        return super.deserialize(bytes, new WebhookMessage());
    }

}
