package io.ruleforge.management.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalRequestDto(
        UUID id,
        UUID ruleId,
        String ruleName,
        String ruleType,
        int version,
        UUID requestedBy,
        String requestedByName,
        Instant requestedAt,
        String note,
        String status,
        UUID resolvedBy,
        Instant resolvedAt,
        Instant deadline,
        List<AssigneeStatusDto> assignees
) {}
