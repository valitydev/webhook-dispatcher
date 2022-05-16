package dev.vality.webhook.dispatcher.repository;

import dev.vality.webhook.dispatcher.entity.CommitLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitLogRepository extends JpaRepository<CommitLogEntity, String> {
}
