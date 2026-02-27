package io.ruleforge.event.repository;

import io.ruleforge.event.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId, Instant since);

    List<NotificationEntity> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    List<NotificationEntity> findByOrgIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID orgId, Instant since);
}
