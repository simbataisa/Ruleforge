package io.ruleforge.solver.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request payload for evaluating a single rule against a set of input facts.
 *
 * @param input    the facts to evaluate (required)
 * @param strategy optional conflict resolution strategy name
 * @param options  optional evaluation options
 */
public record SolveRequest(
        @NotNull Map<String, Object> input,
        String strategy,
        Map<String, Object> options
) {}
