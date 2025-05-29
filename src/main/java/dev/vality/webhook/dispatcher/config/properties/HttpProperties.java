package dev.vality.webhook.dispatcher.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("http-client")
public class HttpProperties {

    @NotNull
    private int maxTotal;
    @NotNull
    private int maxPerRoute;
    @NotNull
    private int timeout;


}
