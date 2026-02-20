package io.ruleforge.solver.dto;

import java.util.Map;

/**
 * Represents a single rule that matched during evaluation.
 *
 * @param ruleId   the matched rule's identifier
 * @param priority the rule's priority value
 * @param output   the output produced by this matched rule
 */
public record MatchedRuleDto(
        String ruleId,
        int priority,
        Map<String, Object> output
) {}
