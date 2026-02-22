package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateRuleRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Pattern(regexp = "^[a-z0-9_-]+$", message = "Alias must contain only lowercase letters, numbers, hyphens, and underscores")
        String alias,

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "^(TABLE|TREE|SCRIPT|FLOW|SCORECARD|DMN|CEP|ENSEMBLE)$",
                message = "Type must be one of: TABLE, TREE, SCRIPT, FLOW, SCORECARD, DMN, CEP, ENSEMBLE")
        String type,

        String description,

        UUID folderId,

        String definition,

        String inputModel,

        String outputModel,

        String changeNote
) {}
