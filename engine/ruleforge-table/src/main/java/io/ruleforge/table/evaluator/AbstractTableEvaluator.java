package io.ruleforge.table.evaluator;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.table.compiler.ColumnDefinition;
import io.ruleforge.table.compiler.CompiledDecisionTable;
import io.ruleforge.table.compiler.CompiledRow;
import io.ruleforge.table.compiler.RowDefinition.CellValue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for table evaluators that provides common functionality for
 * candidate selection, condition verification, and result building.
 */
abstract class AbstractTableEvaluator implements TableEvaluator {

    /**
     * Finds all rows that match the working memory by using indexes for candidate
     * selection and then verifying each candidate row's conditions.
     */
    protected List<CompiledRow> findMatchingRows(CompiledDecisionTable table, WorkingMemory wm) {
        Set<Integer> candidateRows = table.findCandidateRows(wm);
        List<CompiledRow> matched = new ArrayList<>();

        for (CompiledRow row : table.rows()) {
            if (candidateRows.contains(row.rowNumber()) && table.evaluateRowConditions(row, wm)) {
                matched.add(row);
            }
        }
        return matched;
    }

    /**
     * Builds a {@link MatchedRule} from a compiled row.
     */
    protected MatchedRule toMatchedRule(CompiledDecisionTable table, CompiledRow row) {
        Map<String, Object> matchedConditions = new LinkedHashMap<>();
        List<ColumnDefinition> condCols = table.conditionColumns();
        for (int i = 0; i < condCols.size() && i < row.conditionValues().size(); i++) {
            CellValue cv = row.conditionValues().get(i);
            if (!cv.isWildcard()) {
                matchedConditions.put(condCols.get(i).name(), cv.value());
            }
        }
        PriorityVector priority = new PriorityVector(
                row.priority(), condCols.size(), 1.0, Instant.now(), row.rowNumber()
        );
        return new MatchedRule(
                table.ruleId(), table.ruleName(), table.ruleVersion(),
                priority, matchedConditions, row.actionMap()
        );
    }

    /**
     * Builds a {@link RuleResult} from evaluation results.
     */
    protected RuleResult buildResult(CompiledDecisionTable table,
                                      Map<String, Object> output,
                                      List<MatchedRule> matchedRules,
                                      long startNanos,
                                      String strategy) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        DecisionTrace trace = DecisionTrace.builder(table.ruleId()).build();
        RuleResultMetadata metadata = new RuleResultMetadata(
                elapsed, table.rows().size(), matchedRules.size(), 0, strategy
        );
        return new RuleResult(output, metadata, matchedRules, trace);
    }
}
