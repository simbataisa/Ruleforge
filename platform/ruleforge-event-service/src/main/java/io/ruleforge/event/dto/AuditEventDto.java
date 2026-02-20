package io.ruleforge.event.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventDto(
        UUID id,
        UUID orgId,
        String eventType,
        UUID actorId,
        String resourceType,
        String resourceId,
        Map<String, Object> changes,
        Instant timestamp
) {}
