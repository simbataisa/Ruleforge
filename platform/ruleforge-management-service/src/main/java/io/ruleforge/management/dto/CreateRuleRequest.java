package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateRuleRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Alias is required")
        @Pattern(regexp = "^[a-z0-9_-]+$", message = "Alias must contain only lowercase letters, numbers, hyphens, and underscores")
        String alias,

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "^(DECISION_TABLE|DECISION_TREE|EXPRESSION|SCRIPT|FLOW)$",
                message = "Type must be one of: DECISION_TABLE, DECISION_TREE, EXPRESSION, SCRIPT, FLOW")
        String type,

        UUID folderId,

        String definition,

        String inputModel,

        String outputModel,

        String changeNote
) {}
