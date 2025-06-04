package dev.vality.webhook.dispatcher.config;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.kafka.common.util.ExponentialBackOffDefaultErrorHandlerFactory;
import dev.vality.webhook.dispatcher.WebhookMessage;
import dev.vality.webhook.dispatcher.serde.WebhookDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;


    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
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
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        return factory;
    }

    private DefaultErrorHandler kafkaErrorHandler() {
        return ExponentialBackOffDefaultErrorHandlerFactory.create();
    }

    @Bean
    public ProducerFactory<String, WebhookMessage> producerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, WebhookMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
