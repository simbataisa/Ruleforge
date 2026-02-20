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
 * Aggregates numeric outputs across all matching rows using SUM, MIN, MAX, AVG, COUNT.
 * Non-numeric outputs are taken from the last matching row.
 */
public final class AggregateEvaluator extends AbstractTableEvaluator {

    @Override
    public RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        List<CompiledRow> matched = findMatchingRows(table, wm);

        if (matched.isEmpty()) {
            return buildResult(table, Map.of(), List.of(), startNanos, "AGGREGATE");
        }

        // Collect values per output key
        Map<String, List<Number>> numericCollector = new LinkedHashMap<>();
        Map<String, Object> nonNumeric = new LinkedHashMap<>();
        List<MatchedRule> matchedRules = new ArrayList<>();

        for (CompiledRow row : matched) {
            matchedRules.add(toMatchedRule(table, row));
            for (Map.Entry<String, Object> entry : row.actionMap().entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Number n) {
                    numericCollector.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(n);
                } else {
                    nonNumeric.put(entry.getKey(), val);
                }
            }
        }

        // Build aggregated output
        Map<String, Object> output = new LinkedHashMap<>(nonNumeric);
        for (Map.Entry<String, List<Number>> entry : numericCollector.entrySet()) {
            List<Number> values = entry.getValue();
            double sum = values.stream().mapToDouble(Number::doubleValue).sum();
            double avg = sum / values.size();
            double min = values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
            double max = values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);

            Map<String, Object> aggregated = new LinkedHashMap<>();
            aggregated.put("SUM", sum);
            aggregated.put("AVG", avg);
            aggregated.put("MIN", min);
            aggregated.put("MAX", max);
            aggregated.put("COUNT", values.size());
            output.put(entry.getKey(), aggregated);
        }

        return buildResult(table, output, matchedRules, startNanos, "AGGREGATE");
    }
}
