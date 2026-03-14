package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.dto.AuditEventDto;
import io.ruleforge.event.dto.AuditQueryFilter;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.store.AuditEventStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventStore auditEventStore;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventStore auditEventStore, ObjectMapper objectMapper) {
        this.auditEventStore = auditEventStore;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordEvent(DomainEvent event) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setOrgId(event.orgId());
        entity.setEventType(event.eventType());
        entity.setActorId(event.actorId());
        entity.setResourceType(event.aggregateType());
        entity.setResourceId(event.aggregateId());
        entity.setCreatedAt(event.timestamp());
        entity.setActorType("USER");

        Map<String, Object> payload = event.payload();
        if (payload != null) {
            Object actorName = payload.get("actorName");
            if (actorName != null) entity.setActorName(actorName.toString());

            Object resourceName = payload.get("name");
            if (resourceName != null) entity.setResourceName(resourceName.toString());

            Object actorType = payload.get("actorType");
            if (actorType != null) entity.setActorType(actorType.toString());

            Object spaceId = payload.get("spaceId");
            if (spaceId != null) {
                try {
                    entity.setSpaceId(UUID.fromString(spaceId.toString()));
                } catch (IllegalArgumentException ignored) {}
            }

            try {
                entity.setChanges(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize event payload for event {}: {}", event.eventId(), e.getMessage());
                entity.setChanges("{}");
            }
        }

        auditEventStore.save(entity);
        log.debug("Recorded audit event: type={}, aggregateId={}", event.eventType(), event.aggregateId());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getEvents(AuditQueryFilter filter, Pageable pageable) {
        // Strip sort from pageable — the native query already has ORDER BY created_at DESC hardcoded.
        // Passing a sorted Pageable causes Spring Data JPA to append ORDER BY "createdAt" (camelCase)
        // which PostgreSQL rejects with "column createdAt does not exist".
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditEventEntity> page = auditEventStore.findByFilter(filter, unsorted);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getEvents(UUID orgId, String eventType, Pageable pageable) {
        AuditQueryFilter filter = new AuditQueryFilter(
                orgId, eventType, null, null, null, null, null, null
        );
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditEventEntity> result = auditEventStore.findByFilter(filter, unsorted);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getResourceEvents(UUID orgId, String resourceType,
                                                          String resourceId, Pageable pageable) {
        AuditQueryFilter filter = new AuditQueryFilter(
                orgId, null, resourceType, resourceId, null, null, null, null
        );
        Page<AuditEventEntity> result = auditEventStore.findByFilter(filter, pageable);
        return toPageResponse(result);
    }

    private PageResponse<AuditEventDto> toPageResponse(Page<AuditEventEntity> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private AuditEventDto toDto(AuditEventEntity entity) {
        Map<String, Object> changes = Collections.emptyMap();
        if (entity.getChanges() != null && !entity.getChanges().isBlank()) {
            try {
                changes = objectMapper.readValue(entity.getChanges(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize changes for audit event {}: {}", entity.getId(), e.getMessage());
            }
        }
        Map<String, Object> metadata = Collections.emptyMap();
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize metadata for audit event {}: {}", entity.getId(), e.getMessage());
            }
        }
        return new AuditEventDto(
                entity.getId(),
                entity.getOrgId(),
                entity.getSpaceId(),
                entity.getEventType(),
                entity.getActorId(),
                entity.getActorName(),
                entity.getActorType(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getResourceName(),
                changes,
                metadata,
                entity.getCreatedAt()
        );
    }
}
