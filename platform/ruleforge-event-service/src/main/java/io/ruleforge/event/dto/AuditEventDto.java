package io.ruleforge.event.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventDto(
        UUID id,
        UUID orgId,
        UUID spaceId,
        String eventType,
        UUID actorId,
        String actorName,
        String actorType,
        String resourceType,
        String resourceId,
        String resourceName,
        Map<String, Object> changes,
        Map<String, Object> metadata,
        Instant createdAt
) {}
