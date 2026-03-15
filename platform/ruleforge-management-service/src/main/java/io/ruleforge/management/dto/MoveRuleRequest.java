package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveRuleRequest(
        @NotNull(message = "targetSpaceId is required")
        UUID targetSpaceId
) {}
