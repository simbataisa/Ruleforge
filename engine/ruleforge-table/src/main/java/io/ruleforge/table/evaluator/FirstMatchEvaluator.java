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
 * Returns the first matching row, ordered by priority descending then row number ascending.
 */
public final class FirstMatchEvaluator extends AbstractTableEvaluator {

    @Override
    public RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        List<CompiledRow> matched = findMatchingRows(table, wm);

        if (matched.isEmpty()) {
            return buildResult(table, Map.of(), List.of(), startNanos, "FIRST");
        }

        // Sort by priority desc, then row number asc
        CompiledRow first = matched.stream()
                .min(Comparator.comparingInt(CompiledRow::priority).reversed()
                        .thenComparingInt(CompiledRow::rowNumber))
                .orElseThrow();

        Map<String, Object> output = new LinkedHashMap<>(first.actionMap());
        List<MatchedRule> matchedRules = List.of(toMatchedRule(table, first));
        return buildResult(table, output, matchedRules, startNanos, "FIRST");
    }
}
