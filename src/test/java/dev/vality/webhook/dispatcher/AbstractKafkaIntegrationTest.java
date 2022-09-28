package dev.vality.webhook.dispatcher;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.webhook.dispatcher.dao.DaoTestBase;
import dev.vality.webhook.dispatcher.serde.WebhookDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Slf4j
@RunWith(SpringRunner.class)
@DirtiesContext
@ContextConfiguration(initializers = AbstractKafkaIntegrationTest.Initializer.class)
public abstract class AbstractKafkaIntegrationTest extends DaoTestBase {

    private static final String KAFKA_DOCKER_VERSION = "5.0.1";

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(KAFKA_DOCKER_VERSION).withEmbeddedZookeeper();

    public static <T> Consumer<String, T> createConsumer(Class clazz) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, clazz);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    public static <T> Producer<String, T> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "CLIENT");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        return new KafkaProducer<>(props);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public static final String WEBHOOK_FORWARD = "webhook-forward";
        public static final String WEBHOOK_FIRST_RETRY = "webhook-first-retry";
        public static final String WEBHOOK_SECOND_RETRY = "webhook-second-retry";
        public static final String WEBHOOK_THIRD_RETRY = "webhook-third-retry";
        public static final String WEBHOOK_LAST_RETRY = "webhook-last-retry";
        public static final String WEBHOOK_DLQ = "webhook-dead-letter-queue";

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues
                    .of("spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers())
                    .applyTo(configurableApplicationContext.getEnvironment());
            initTopic(WEBHOOK_FORWARD);
            initTopic(WEBHOOK_FIRST_RETRY);
            initTopic(WEBHOOK_SECOND_RETRY);
            initTopic(WEBHOOK_THIRD_RETRY);
            initTopic(WEBHOOK_LAST_RETRY);
            initTopic(WEBHOOK_DLQ);
        }

        private <T> void initTopic(String topicName) {
            Consumer<String, T> consumer = createConsumer(WebhookDeserializer.class);
            try {
                consumer.subscribe(Collections.singletonList(topicName));
                consumer.poll(Duration.ofMillis(100L));
            } catch (Exception e) {
                log.error("KafkaAbstractTest initialize e: ", e);
            }
            consumer.close();
        }
    }

}
