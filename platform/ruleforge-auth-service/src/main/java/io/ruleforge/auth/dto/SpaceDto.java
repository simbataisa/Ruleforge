package io.ruleforge.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SpaceDto(
        UUID id,
        String name,
        UUID orgId,
        Instant createdAt
) {}
