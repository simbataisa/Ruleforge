package io.ruleforge.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SpaceDto(
        UUID id,
        String name,
        String slug,
        String description,
        long ruleCount,
        UUID orgId,
        Instant createdAt,
        Instant updatedAt
) {}
