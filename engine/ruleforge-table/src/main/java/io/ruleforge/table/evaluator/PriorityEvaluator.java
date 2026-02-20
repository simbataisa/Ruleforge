package io.ruleforge.table.evaluator;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.table.compiler.CompiledDecisionTable;
import io.ruleforge.table.compiler.CompiledRow;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns the highest-priority matching row. If multiple rows share the same
 * priority, the one with the lowest row number wins.
 */
public final class PriorityEvaluator extends AbstractTableEvaluator {

    @Override
    public RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        List<CompiledRow> matched = findMatchingRows(table, wm);

        if (matched.isEmpty()) {
            return buildResult(table, Map.of(), List.of(), startNanos, "PRIORITY");
        }

        CompiledRow best = matched.stream()
                .max(Comparator.comparingInt(CompiledRow::priority)
                        .thenComparing(Comparator.comparingInt(CompiledRow::rowNumber).reversed()))
                .orElseThrow();

        Map<String, Object> output = new LinkedHashMap<>(best.actionMap());
        List<MatchedRule> matchedRules = List.of(toMatchedRule(table, best));
        return buildResult(table, output, matchedRules, startNanos, "PRIORITY");
    }
}
