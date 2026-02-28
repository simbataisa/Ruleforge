package io.ruleforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditQueryFilter(
        UUID orgId,
        String eventType,
        String resourceType,
        String resourceId,
        UUID actorId,
        String search,
        Instant from,
        Instant to
) {}
