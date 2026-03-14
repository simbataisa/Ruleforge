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
              AND (CAST(:eventType AS TEXT) IS NULL OR event_type LIKE CAST(:eventType AS TEXT))
              AND (CAST(:resourceType AS TEXT) IS NULL OR resource_type = CAST(:resourceType AS TEXT))
              AND (CAST(:resourceId AS TEXT) IS NULL OR resource_id = CAST(:resourceId AS TEXT))
              AND (CAST(:actorId AS TEXT) IS NULL OR actor_id = CAST(:actorId AS uuid))
              AND (CAST(:search AS TEXT) IS NULL OR (
                   event_type ILIKE CAST(:search AS TEXT) OR
                   resource_type ILIKE CAST(:search AS TEXT) OR
                   COALESCE(resource_id, '') ILIKE CAST(:search AS TEXT) OR
                   COALESCE(actor_name, '') ILIKE CAST(:search AS TEXT) OR
                   COALESCE(resource_name, '') ILIKE CAST(:search AS TEXT)
              ))
              AND (CAST(:fromDate AS TEXT) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
              AND (CAST(:toDate AS TEXT) IS NULL OR created_at <= CAST(:toDate AS timestamptz))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM audit_events
            WHERE org_id = :orgId
              AND (CAST(:eventType AS TEXT) IS NULL OR event_type LIKE CAST(:eventType AS TEXT))
              AND (CAST(:resourceType AS TEXT) IS NULL OR resource_type = CAST(:resourceType AS TEXT))
              AND (CAST(:resourceId AS TEXT) IS NULL OR resource_id = CAST(:resourceId AS TEXT))
              AND (CAST(:actorId AS TEXT) IS NULL OR actor_id = CAST(:actorId AS uuid))
              AND (CAST(:search AS TEXT) IS NULL OR (
                   event_type ILIKE CAST(:search AS TEXT) OR
                   resource_type ILIKE CAST(:search AS TEXT) OR
                   COALESCE(resource_id, '') ILIKE CAST(:search AS TEXT) OR
                   COALESCE(actor_name, '') ILIKE CAST(:search AS TEXT) OR
                   COALESCE(resource_name, '') ILIKE CAST(:search AS TEXT)
              ))
              AND (CAST(:fromDate AS TEXT) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
              AND (CAST(:toDate AS TEXT) IS NULL OR created_at <= CAST(:toDate AS timestamptz))
            """,
            nativeQuery = true)
    Page<AuditEventEntity> findByAdvancedFilters(
            @Param("orgId") UUID orgId,
            @Param("eventType") String eventType,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("actorId") String actorId,
            @Param("search") String search,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            Pageable pageable);
}
