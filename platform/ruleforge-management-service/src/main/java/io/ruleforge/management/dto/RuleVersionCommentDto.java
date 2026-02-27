package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.UUID;

public record RuleVersionCommentDto(
        UUID id,
        UUID ruleId,
        int version,
        UUID authorId,
        String body,
        UUID parentId,
        Instant createdAt
) {}
