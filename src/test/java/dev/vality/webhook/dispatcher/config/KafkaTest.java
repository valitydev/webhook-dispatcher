package dev.vality.webhook.dispatcher.config;


import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaConfig
@KafkaTestcontainerSingleton(
        properties = {"http-client.timeout=1", "kafka.topic.concurrency.forward=1", "retry.nack.seconds=1",
                "retry.first.seconds=2", "retry.second.seconds=3", "retry.third.seconds=4",
                "retry.last.seconds=1", "retry.dead.time.hours=1"},
        topicsKeys = {"kafka.topic.webhook.forward", "kafka.topic.webhook.first.retry",
                "kafka.topic.webhook.second.retry", "kafka.topic.webhook.third.retry",
                "kafka.topic.webhook.last.retry", "kafka.topic.webhook.dead.letter.queue"})
public @interface KafkaTest {
}