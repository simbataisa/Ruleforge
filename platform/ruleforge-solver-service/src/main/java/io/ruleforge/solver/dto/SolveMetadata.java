package io.ruleforge.solver.dto;

/**
 * Metadata about a rule evaluation execution.
 *
 * @param ruleId          the evaluated rule's ID
 * @param ruleName        the evaluated rule's name
 * @param ruleType        the rule type (e.g. DECISION_TABLE, DECISION_TREE)
 * @param version         the rule version that was evaluated
 * @param executionTimeMs wall-clock execution time in milliseconds
 * @param rulesEvaluated  number of rules evaluated during execution
 * @param rulesMatched    number of rules that matched (fired)
 * @param strategy        the conflict resolution strategy used
 */
public record SolveMetadata(
        String ruleId,
        String ruleName,
        String ruleType,
        int version,
        long executionTimeMs,
        int rulesEvaluated,
        int rulesMatched,
        String strategy
) {}
