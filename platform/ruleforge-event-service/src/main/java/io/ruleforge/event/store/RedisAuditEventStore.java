package io.ruleforge.event.store;

import io.ruleforge.event.dto.AuditQueryFilter;
import io.ruleforge.event.entity.AuditEventEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.storage.backend", havingValue = "redis")
public class RedisAuditEventStore implements AuditEventStore {

    @Override
    public void save(AuditEventEntity event) {
        // TODO: implement Redis stream write
        throw new UnsupportedOperationException("Redis audit event store not yet implemented");
    }

    @Override
    public Page<AuditEventEntity> findByFilter(AuditQueryFilter filter, Pageable pageable) {
        // TODO: implement Redis stream query
        throw new UnsupportedOperationException("Redis audit event store not yet implemented");
    }

    @Override
    public long countByFilter(AuditQueryFilter filter) {
        // TODO: implement Redis stream count
        throw new UnsupportedOperationException("Redis audit event store not yet implemented");
    }
}
