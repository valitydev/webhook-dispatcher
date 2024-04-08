package dev.vality.webhook.dispatcher.repository;

import dev.vality.webhook.dispatcher.entity.DeadWebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeadWebhookRepository extends JpaRepository<DeadWebhookEntity, String> {

    Optional<DeadWebhookEntity> findByWebhookIdAndSourceIdAndEventId(long webhookId, String sourceId, long eventId);

    void deleteByWebhookIdAndSourceIdAndEventId(long webhookId, String sourceId, long eventId);
}
