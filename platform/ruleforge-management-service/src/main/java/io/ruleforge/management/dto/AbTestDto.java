package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.UUID;

public record AbTestDto(
        UUID id,
        UUID spaceId,
        UUID ruleId,
        String name,
        int versionA,
        int versionB,
        int trafficSplit,
        String status,
        Instant startDate,
        Instant endDate,
        UUID createdBy,
        Instant createdAt
) {}
