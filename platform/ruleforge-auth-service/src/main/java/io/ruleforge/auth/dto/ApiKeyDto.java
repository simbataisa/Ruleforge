package io.ruleforge.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyDto(
        UUID id,
        String name,
        String keyPrefix,
        String keyType,
        List<String> permissions,
        boolean active,
        Instant expiresAt,
        Instant createdAt
) {}
