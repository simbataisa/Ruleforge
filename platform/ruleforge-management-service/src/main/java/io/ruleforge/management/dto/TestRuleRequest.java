package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TestRuleRequest(
        @NotNull(message = "Input is required")
        Map<String, Object> input,

        Map<String, Object> options
) {}
