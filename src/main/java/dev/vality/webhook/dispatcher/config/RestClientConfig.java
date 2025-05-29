package dev.vality.webhook.dispatcher.config;

import dev.vality.webhook.dispatcher.config.properties.HttpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Configuration
public class RestClientConfig {

    private final HttpProperties httpProperties;

    @Bean
    public RestClient restClient(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory requestFactory(HttpClient httpClient) {
        final var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return requestFactory;
    }

    @Bean
    public SSLContext sslContext() throws NoSuchAlgorithmException {
        return SSLContext.getDefault();
    }

    @Bean
    public PoolingHttpClientConnectionManager connectionManager(SSLContext sslContext) {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(
                        DefaultClientTlsStrategy.createDefault()
                )
                .setDefaultSocketConfig(
                        SocketConfig.custom()
                                .setSoTimeout(Timeout.ofMilliseconds(httpProperties.getTimeout()))
                                .build()
                )
                .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(httpProperties.getTimeout()))
                                .build()
                )
                .setMaxConnTotal(httpProperties.getMaxTotal())
                .setMaxConnPerRoute(httpProperties.getMaxPerRoute())
                .build();
    }

    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(httpProperties.getTimeout(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(httpProperties.getTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager,
                                          RequestConfig requestConfig) {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .setConnectionManagerShared(true)
                .build();
    }
}
