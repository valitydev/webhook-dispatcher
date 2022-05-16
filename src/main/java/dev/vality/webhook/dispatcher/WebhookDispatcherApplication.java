package dev.vality.webhook.dispatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.PreDestroy;

@EnableKafka
@ServletComponentScan
@SpringBootApplication
public class WebhookDispatcherApplication extends SpringApplication {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public static void main(String[] args) {
        SpringApplication.run(WebhookDispatcherApplication.class, args);
    }

    @PreDestroy
    public void preDestroy() {
        registry.stop();
        kafkaTemplate.flush();
    }

}
