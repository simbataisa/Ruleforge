package io.ruleforge.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationDto(
        UUID id,
        String name,
        String slug,
        Instant createdAt
) {}
