package dev.vality.webhook.dispatcher.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class HttpClientConfig {

    @Value("${merchant.max.per.route}")
    private int maxPerRoute;

    @Value("${merchant.max.total}")
    private int maxTotal;

    @Bean
    public CloseableHttpClient httpClient(@Value("${merchant.timeout}") int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .setMaxConnPerRoute(maxPerRoute)
                .setMaxConnTotal(maxTotal)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                .build();
    }

}
