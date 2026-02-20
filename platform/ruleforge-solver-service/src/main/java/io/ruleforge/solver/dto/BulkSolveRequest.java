package io.ruleforge.solver.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Request payload for bulk rule evaluation against multiple input sets.
 *
 * @param inputs   list of input fact maps to evaluate (at least one required)
 * @param strategy optional conflict resolution strategy name
 * @param options  optional evaluation options
 */
public record BulkSolveRequest(
        @NotEmpty List<Map<String, Object>> inputs,
        String strategy,
        Map<String, Object> options
) {}
