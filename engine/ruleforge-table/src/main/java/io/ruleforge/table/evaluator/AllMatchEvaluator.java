package io.ruleforge.table.evaluator;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.table.compiler.CompiledDecisionTable;
import io.ruleforge.table.compiler.CompiledRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns all matching rows with their outputs merged (later rows overwrite
 * earlier rows for the same key).
 */
public final class AllMatchEvaluator extends AbstractTableEvaluator {

    @Override
    public RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        List<CompiledRow> matched = findMatchingRows(table, wm);

        if (matched.isEmpty()) {
            return buildResult(table, Map.of(), List.of(), startNanos, "ALL");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        List<MatchedRule> matchedRules = new ArrayList<>();

        for (CompiledRow row : matched) {
            output.putAll(row.actionMap());
            matchedRules.add(toMatchedRule(table, row));
        }

        return buildResult(table, output, matchedRules, startNanos, "ALL");
    }
}
