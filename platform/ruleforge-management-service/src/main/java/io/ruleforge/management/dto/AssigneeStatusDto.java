package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.UUID;

public record AssigneeStatusDto(
        UUID memberId,
        String name,
        String status,
        Instant respondedAt
) {}
