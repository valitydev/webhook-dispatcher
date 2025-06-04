package dev.vality.webhook.dispatcher.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Component
public class IgnoreHttpErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    @Override
    public void handle(HttpRequest request,
                       ClientHttpResponse response) throws IOException {
        // ignore
    }
}
