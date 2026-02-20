package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.dto.AuditEventDto;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.repository.AuditEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
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
        entity.setTimestamp(event.timestamp());

        try {
            if (event.payload() != null) {
                entity.setChanges(objectMapper.writeValueAsString(event.payload()));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event payload for event {}: {}", event.eventId(), e.getMessage());
            entity.setChanges("{}");
        }

        auditEventRepository.save(entity);
        log.debug("Recorded audit event: type={}, aggregateId={}", event.eventType(), event.aggregateId());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getEvents(UUID orgId, String eventType, Pageable pageable) {
        Page<AuditEventEntity> page;
        if (eventType != null && !eventType.isBlank()) {
            page = auditEventRepository.findByOrgIdAndEventTypeOrderByTimestampDesc(orgId, eventType, pageable);
        } else {
            page = auditEventRepository.findByOrgIdOrderByTimestampDesc(orgId, pageable);
        }
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getResourceEvents(UUID orgId, String resourceType,
                                                          String resourceId, Pageable pageable) {
        Page<AuditEventEntity> page = auditEventRepository
                .findByOrgIdAndResourceTypeAndResourceIdOrderByTimestampDesc(orgId, resourceType, resourceId, pageable);
        return toPageResponse(page);
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
        return new AuditEventDto(
                entity.getId(),
                entity.getOrgId(),
                entity.getEventType(),
                entity.getActorId(),
                entity.getResourceType(),
                entity.getResourceId(),
                changes,
                entity.getTimestamp()
        );
    }
}
