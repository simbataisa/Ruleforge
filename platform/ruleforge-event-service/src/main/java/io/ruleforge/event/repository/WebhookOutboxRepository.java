package io.ruleforge.event.repository;

import io.ruleforge.event.entity.WebhookOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookOutboxRepository extends JpaRepository<WebhookOutboxEntity, UUID> {

    @Query(value = """
        SELECT * FROM webhook_outbox
        WHERE (status = 'PENDING' OR status = 'FAILED')
        AND next_attempt_at <= :now
        ORDER BY next_attempt_at
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<WebhookOutboxEntity> findPendingForDelivery(
        @Param("now") OffsetDateTime now,
        @Param("batchSize") int batchSize
    );

    List<WebhookOutboxEntity> findByWebhookIdOrderByCreatedAtDesc(UUID webhookId, Pageable pageable);

    List<WebhookOutboxEntity> findByWebhookIdAndStatusOrderByCreatedAtDesc(UUID webhookId, String status, Pageable pageable);

    @Modifying
    @Query("DELETE FROM WebhookOutboxEntity o WHERE o.status = 'DLQ' AND o.createdAt < :cutoff")
    int deleteOldDlqEntries(@Param("cutoff") OffsetDateTime cutoff);
}
