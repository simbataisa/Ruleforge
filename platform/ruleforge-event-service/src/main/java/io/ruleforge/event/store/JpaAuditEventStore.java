package io.ruleforge.event.store;

import io.ruleforge.event.dto.AuditQueryFilter;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.repository.AuditEventRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class JpaAuditEventStore implements AuditEventStore {

    private final AuditEventRepository repository;

    public JpaAuditEventStore(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditEventEntity event) {
        repository.save(event);
    }

    @Override
    public Page<AuditEventEntity> findByFilter(AuditQueryFilter filter, Pageable pageable) {
        String eventTypePattern = filter.eventType() != null ? filter.eventType() + "%" : null;
        String searchPattern = filter.search() != null ? "%" + filter.search() + "%" : null;
        String actorIdStr = filter.actorId() != null ? filter.actorId().toString() : null;
        String fromStr = filter.from() != null ? filter.from().toString() : null;
        String toStr = filter.to() != null ? filter.to().toString() : null;

        return repository.findByAdvancedFilters(
                filter.orgId(),
                eventTypePattern,
                filter.resourceType(),
                filter.resourceId(),
                actorIdStr,
                searchPattern,
                fromStr,
                toStr,
                pageable
        );
    }

    @Override
    public long countByFilter(AuditQueryFilter filter) {
        String eventTypePattern = filter.eventType() != null ? filter.eventType() + "%" : null;
        String searchPattern = filter.search() != null ? "%" + filter.search() + "%" : null;
        String actorIdStr = filter.actorId() != null ? filter.actorId().toString() : null;
        String fromStr = filter.from() != null ? filter.from().toString() : null;
        String toStr = filter.to() != null ? filter.to().toString() : null;

        return repository.findByAdvancedFilters(
                filter.orgId(),
                eventTypePattern,
                filter.resourceType(),
                filter.resourceId(),
                actorIdStr,
                searchPattern,
                fromStr,
                toStr,
                Pageable.unpaged()
        ).getTotalElements();
    }
}
