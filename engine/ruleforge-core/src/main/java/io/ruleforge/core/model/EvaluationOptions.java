package io.ruleforge.core.model;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Options that control how a rule evaluation is performed.
 *
 * @param mode             The execution mode.
 * @param strategyName     Name of the conflict resolution strategy to use (String to avoid circular dependency).
 * @param timeout          Maximum time allowed for the evaluation.
 * @param traceEnabled     Whether to collect execution trace information.
 * @param auditEnabled     Whether to generate audit log entries.
 * @param excludeColumns   Column names to exclude from decision-table evaluation.
 * @param hierarchyContext Context keys for cascading/hierarchical evaluation.
 * @param correlationId    External correlation identifier for tracing across services.
 */
public record EvaluationOptions(
        ExecutionMode mode,
        String strategyName,
        Duration timeout,
        boolean traceEnabled,
        boolean auditEnabled,
        Set<String> excludeColumns,
        Map<String, String> hierarchyContext,
        String correlationId
) {

    /**
     * Returns an {@link EvaluationOptions} instance with sensible defaults.
     */
    public static EvaluationOptions defaults() {
        return new EvaluationOptions(
                ExecutionMode.FIRST_MATCH,  // mode
                null,                       // strategyName
                Duration.ofSeconds(30),     // timeout
                false,                      // traceEnabled
                false,                      // auditEnabled
                Set.of(),                   // excludeColumns
                Map.of(),                   // hierarchyContext
                null                        // correlationId
        );
    }
}
