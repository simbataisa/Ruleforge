package io.ruleforge.event.repository;

import io.ruleforge.event.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    Page<AuditEventEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

    Page<AuditEventEntity> findByOrgIdAndEventTypeOrderByCreatedAtDesc(UUID orgId, String eventType, Pageable pageable);

    Page<AuditEventEntity> findByOrgIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
            UUID orgId, String resourceType, String resourceId, Pageable pageable);

    List<AuditEventEntity> findByOrgIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID orgId, Instant since);

    @Query(value = """
            SELECT * FROM audit_events
            WHERE org_id = :orgId
              AND (:eventType IS NULL OR event_type LIKE :eventType)
              AND (:resourceType IS NULL OR resource_type = :resourceType)
              AND (:resourceId IS NULL OR resource_id = :resourceId)
              AND (:actorId IS NULL OR actor_id = CAST(:actorId AS uuid))
              AND (:search IS NULL OR (
                   event_type ILIKE :search OR
                   resource_type ILIKE :search OR
                   COALESCE(resource_id, '') ILIKE :search OR
                   COALESCE(actor_name, '') ILIKE :search
              ))
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS timestamptz))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM audit_events
            WHERE org_id = :orgId
              AND (:eventType IS NULL OR event_type LIKE :eventType)
              AND (:resourceType IS NULL OR resource_type = :resourceType)
              AND (:resourceId IS NULL OR resource_id = :resourceId)
              AND (:actorId IS NULL OR actor_id = CAST(:actorId AS uuid))
              AND (:search IS NULL OR (
                   event_type ILIKE :search OR
                   resource_type ILIKE :search OR
                   COALESCE(resource_id, '') ILIKE :search OR
                   COALESCE(actor_name, '') ILIKE :search
              ))
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS timestamptz))
            """,
            nativeQuery = true)
    Page<AuditEventEntity> findByAdvancedFilters(
            @Param("orgId") UUID orgId,
            @Param("eventType") String eventType,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("actorId") UUID actorId,
            @Param("search") String search,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
