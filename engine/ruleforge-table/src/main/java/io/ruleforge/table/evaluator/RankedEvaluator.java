package io.ruleforge.table.evaluator;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.table.compiler.CompiledDecisionTable;
import io.ruleforge.table.compiler.CompiledRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns top-N matches ranked by score (priority descending, then row number ascending).
 * <p>
 * The maximum number of results can be configured via the evaluation context property
 * {@code "rankedLimit"}. Defaults to all matching rows if not set.
 */
public final class RankedEvaluator extends AbstractTableEvaluator {

    private static final String RANKED_LIMIT_PROPERTY = "rankedLimit";
    private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

    @Override
    public RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        List<CompiledRow> matched = findMatchingRows(table, wm);

        if (matched.isEmpty()) {
            return buildResult(table, Map.of(), List.of(), startNanos, "RANKED");
        }

        // Determine limit
        int limit = DEFAULT_LIMIT;
        if (ctx != null) {
            Object limitProp = ctx.getProperty(RANKED_LIMIT_PROPERTY);
            if (limitProp instanceof Number n) {
                limit = n.intValue();
            }
        }

        // Sort by priority descending, then row number ascending
        List<CompiledRow> ranked = matched.stream()
                .sorted(Comparator.comparingInt(CompiledRow::priority).reversed()
                        .thenComparingInt(CompiledRow::rowNumber))
                .limit(limit)
                .toList();

        Map<String, Object> output = new LinkedHashMap<>();
        List<MatchedRule> matchedRules = new ArrayList<>();

        for (CompiledRow row : ranked) {
            output.putAll(row.actionMap());
            matchedRules.add(toMatchedRule(table, row));
        }

        return buildResult(table, output, matchedRules, startNanos, "RANKED");
    }
}
