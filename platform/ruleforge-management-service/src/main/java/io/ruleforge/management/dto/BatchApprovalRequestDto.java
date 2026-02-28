package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BatchApprovalRequestDto(
        @NotEmpty(message = "ruleIds must not be empty")
        List<UUID> ruleIds,
        List<UUID> assigneeIds,
        String note
) {}
