package io.ruleforge.event.repository;

import io.ruleforge.event.entity.WebhookDeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, UUID> {

    Page<WebhookDeliveryEntity> findByWebhookIdOrderByDeliveredAtDesc(UUID webhookId, Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                date_trunc('day', attempted_at AT TIME ZONE 'UTC')::DATE AS date,
                COUNT(*) FILTER (WHERE status = 'SUCCESS') AS success,
                COUNT(*) FILTER (WHERE status != 'SUCCESS') AS failed
            FROM webhook_deliveries
            WHERE webhook_id = :webhookId
              AND org_id = :orgId
              AND attempted_at >= :since
              AND attempted_at IS NOT NULL
            GROUP BY date_trunc('day', attempted_at AT TIME ZONE 'UTC')
            ORDER BY 1
            """)
    List<DailyDeliveryStats> findDailyStats(
            @Param("webhookId") UUID webhookId,
            @Param("orgId") UUID orgId,
            @Param("since") Instant since);

    int countByWebhookIdAndOrgIdAndStatus(UUID webhookId, UUID orgId, String status);

    @Query(nativeQuery = true, value = """
            SELECT AVG(
                EXTRACT(EPOCH FROM (delivered_at - attempted_at)) * 1000
            )::INT
            FROM webhook_deliveries
            WHERE webhook_id = :webhookId
              AND org_id = :orgId
              AND status = 'SUCCESS'
              AND attempted_at >= :since
              AND attempted_at IS NOT NULL
              AND delivered_at IS NOT NULL
            """)
    Integer avgLatencyMs(
            @Param("webhookId") UUID webhookId,
            @Param("orgId") UUID orgId,
            @Param("since") Instant since);
}
