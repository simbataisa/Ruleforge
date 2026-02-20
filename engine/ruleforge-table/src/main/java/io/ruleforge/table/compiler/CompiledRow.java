package io.ruleforge.table.compiler;

import io.ruleforge.table.compiler.RowDefinition.CellValue;

import java.util.List;
import java.util.Map;

/**
 * A compiled row with pre-parsed condition values and action mappings ready
 * for evaluation.
 *
 * @param rowNumber       the original 1-based row number
 * @param priority        priority for conflict resolution
 * @param conditionValues the raw condition cell values (positional, matching condition columns)
 * @param actionMap       map of action column name to output value
 */
public record CompiledRow(
        int rowNumber,
        int priority,
        List<CellValue> conditionValues,
        Map<String, Object> actionMap
) {

    public CompiledRow {
        conditionValues = conditionValues == null ? List.of() : List.copyOf(conditionValues);
        actionMap = actionMap == null ? Map.of() : Map.copyOf(actionMap);
    }
}
