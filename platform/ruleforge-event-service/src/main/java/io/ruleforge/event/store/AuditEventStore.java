package io.ruleforge.event.store;

import io.ruleforge.event.dto.AuditQueryFilter;
import io.ruleforge.event.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditEventStore {
    void save(AuditEventEntity event);
    Page<AuditEventEntity> findByFilter(AuditQueryFilter filter, Pageable pageable);
    long countByFilter(AuditQueryFilter filter);
}
