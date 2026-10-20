package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleDto(
        UUID id,
        UUID spaceId,
        String name,
        String alias,
        String description,
        String type,
        String status,
        int latestVersion,
        boolean locked,
        UUID lockedBy,
        UUID folderId,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags
) {}
