package io.ruleforge.event.repository;

import io.ruleforge.event.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    Page<AuditEventEntity> findByOrgIdOrderByTimestampDesc(UUID orgId, Pageable pageable);

    Page<AuditEventEntity> findByOrgIdAndEventTypeOrderByTimestampDesc(UUID orgId, String eventType, Pageable pageable);

    Page<AuditEventEntity> findByOrgIdAndResourceTypeAndResourceIdOrderByTimestampDesc(
            UUID orgId, String resourceType, String resourceId, Pageable pageable);

    List<AuditEventEntity> findByOrgIdAndTimestampAfterOrderByTimestampDesc(UUID orgId, Instant since);
}
