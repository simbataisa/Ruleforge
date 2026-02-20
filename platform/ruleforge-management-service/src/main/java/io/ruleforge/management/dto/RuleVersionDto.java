package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.UUID;

public record RuleVersionDto(
        UUID id,
        UUID ruleId,
        int version,
        String definition,
        String inputModel,
        String outputModel,
        String status,
        String changeNote,
        Instant createdAt,
        Instant publishedAt
) {}
