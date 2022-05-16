package dev.vality.webhook.dispatcher.config;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.handler.RetryHandler;
import dev.vality.webhook.dispatcher.serde.WebhookDeserializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private static final String GROUP_ID = "WebHookDispatcherListener";
    private static final String EARLIEST = "earliest";
    private static final String PKCS_12 = "PKCS12";

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    @Value("${kafka.consumer.max-poll-interval-ms}")
    private int maxPollIntervalMs;
    @Value("${kafka.consumer.session-timeout-ms}")
    private int sessionTimeoutMs;
    @Value("${kafka.ssl.server-password}")
    private String serverStorePassword;
    @Value("${kafka.ssl.server-keystore-location}")
    private String serverStoreCertPath;
    @Value("${kafka.ssl.keystore-password}")
    private String keyStorePassword;
    @Value("${kafka.ssl.key-password}")
    private String keyPassword;
    @Value("${kafka.ssl.keystore-location}")
    private String clientStoreCertPath;
    @Value("${kafka.ssl.enable}")
    private boolean kafkaSslEnable;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        sslConfigure(props);
        return props;
    }

    @Bean
    public ConsumerFactory<String, WebhookMessage> consumerFactory() {
        Map<String, Object> configs = consumerConfigs();
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, WebhookDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    @SuppressWarnings("LineLength")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> kafkaListenerContainerFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory,
            @Value("${kafka.concurrency.forward}") int concurrency) {
        return createFactory(consumerFactory, concurrency);
    }

    @Bean
    @SuppressWarnings("LineLength")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory,
            @Value("${kafka.concurrency.first.retry}") int concurrency) {
        return createFactory(consumerFactory, concurrency);
    }

    @Bean
    @SuppressWarnings("LineLength")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> kafkaSecondRetryListenerContainerFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory,
            @Value("${kafka.concurrency.second.retry}") int concurrency) {
        return createFactory(consumerFactory, concurrency);
    }

    @Bean
    @SuppressWarnings("LineLength")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> kafkaThirdRetryListenerContainerFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory,
            @Value("${kafka.concurrency.third.retry}") int concurrency) {
        return createFactory(consumerFactory, concurrency);
    }

    @Bean
    @SuppressWarnings("LineLength")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> kafkaLastRetryListenerContainerFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory,
            @Value("${kafka.concurrency.last.retry}") int concurrency) {
        return createFactory(consumerFactory, concurrency);
    }

    private KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WebhookMessage>> createFactory(
            ConsumerFactory<String, WebhookMessage> consumerFactory, int concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, WebhookMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(RetryHandler.WAITING_PERIOD, 3)));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        return factory;
    }

    private void sslConfigure(Map<String, Object> configProps) {
        if (kafkaSslEnable) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, new File(serverStoreCertPath).getAbsolutePath());
            configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, serverStorePassword);
            configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS_12);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, PKCS_12);
            configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, new File(clientStoreCertPath).getAbsolutePath());
            configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStorePassword);
            configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        }
    }

    @Bean
    public ProducerFactory<String, WebhookMessage> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        sslConfigure(configProps);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, WebhookMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
