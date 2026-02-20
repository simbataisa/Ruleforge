package io.ruleforge.table.compiler;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.table.compiler.ColumnDefinition.ColumnType;
import io.ruleforge.table.compiler.RowDefinition.CellValue;
import io.ruleforge.table.evaluator.HitPolicy;
import io.ruleforge.table.index.ConditionIndex;
import io.ruleforge.table.index.ConditionIndexBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link RuleDefinition} of type {@code DECISION_TABLE} into a
 * {@link CompiledDecisionTable}.
 * <p>
 * Expected keys in {@link RuleDefinition#definition()}:
 * <ul>
 *   <li>{@code "columns"} - List of maps with keys: name, columnType, operator, dataType, index</li>
 *   <li>{@code "rows"} - List of maps with keys: rowNumber, priority, conditions (list), actions (list)</li>
 *   <li>{@code "hitPolicy"} - String: "FIRST", "ALL", "PRIORITY", "AGGREGATE", "RANKED"</li>
 * </ul>
 */
public final class TableCompiler {

    /**
     * Compiles a rule definition into a {@link CompiledDecisionTable}.
     *
     * @param definition the rule definition (must be of type DECISION_TABLE)
     * @return the compiled decision table
     * @throws RuleCompilationException if the definition is malformed
     */
    public CompiledDecisionTable compile(RuleDefinition definition) {
        Map<String, Object> def = definition.definition();
        if (def == null) {
            throw new RuleCompilationException(definition.id(), "Definition payload is null");
        }

        // Parse hit policy
        HitPolicy hitPolicy = parseHitPolicy(definition.id(), def);

        // Parse columns
        List<ColumnDefinition> allColumns = parseColumns(definition.id(), def);
        List<ColumnDefinition> conditionColumns = allColumns.stream()
                .filter(c -> c.columnType() == ColumnType.CONDITION)
                .toList();
        List<ColumnDefinition> actionColumns = allColumns.stream()
                .filter(c -> c.columnType() == ColumnType.ACTION)
                .toList();

        // Parse rows
        List<RowDefinition> rowDefinitions = parseRows(definition.id(), def);

        // Compile rows into CompiledRow instances
        List<CompiledRow> compiledRows = new ArrayList<>();
        for (RowDefinition rowDef : rowDefinitions) {
            Map<String, Object> actionMap = new LinkedHashMap<>();
            for (int i = 0; i < actionColumns.size() && i < rowDef.actions().size(); i++) {
                CellValue cell = rowDef.actions().get(i);
                if (!cell.isWildcard()) {
                    actionMap.put(actionColumns.get(i).name(), cell.value());
                }
            }
            compiledRows.add(new CompiledRow(
                    rowDef.rowNumber(),
                    rowDef.priority(),
                    rowDef.conditions(),
                    actionMap
            ));
        }

        // Build indexes for each condition column
        Map<Integer, ConditionIndex> columnIndexes = buildIndexes(conditionColumns, compiledRows);

        return new CompiledDecisionTable(
                definition.id(),
                definition.name(),
                definition.version(),
                definition.metadata(),
                System.currentTimeMillis(),
                conditionColumns,
                actionColumns,
                compiledRows,
                hitPolicy,
                columnIndexes
        );
    }

    // -----------------------------------------------------------------------
    // Parsing helpers
    // -----------------------------------------------------------------------

    private HitPolicy parseHitPolicy(String ruleId, Map<String, Object> def) {
        Object hitPolicyRaw = def.get("hitPolicy");
        if (hitPolicyRaw == null) {
            return HitPolicy.FIRST; // default
        }
        try {
            return HitPolicy.valueOf(hitPolicyRaw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleCompilationException(ruleId,
                    "Unknown hit policy: " + hitPolicyRaw + ". Valid values: FIRST, ALL, PRIORITY, AGGREGATE, RANKED");
        }
    }

    @SuppressWarnings("unchecked")
    private List<ColumnDefinition> parseColumns(String ruleId, Map<String, Object> def) {
        Object columnsRaw = def.get("columns");
        if (columnsRaw == null) {
            throw new RuleCompilationException(ruleId, "Missing 'columns' in definition");
        }
        if (!(columnsRaw instanceof List<?> columnList)) {
            throw new RuleCompilationException(ruleId, "'columns' must be a list");
        }

        List<ColumnDefinition> columns = new ArrayList<>();
        for (int i = 0; i < columnList.size(); i++) {
            Object item = columnList.get(i);
            if (!(item instanceof Map<?, ?> colMap)) {
                throw new RuleCompilationException(ruleId, "Column at index " + i + " must be a map");
            }
            Map<String, Object> col = (Map<String, Object>) colMap;

            String name = requireString(col, "name", ruleId, "columns[" + i + "]");
            ColumnType columnType = parseColumnType(
                    requireString(col, "columnType", ruleId, "columns[" + i + "]"), ruleId, i);
            String operator = col.containsKey("operator")
                    ? col.get("operator").toString().toUpperCase()
                    : "EQ";
            FactType dataType = parseFactType(
                    col.containsKey("dataType") ? col.get("dataType").toString() : "ANY", ruleId, i);
            int index = col.containsKey("index")
                    ? ((Number) col.get("index")).intValue()
                    : i;

            columns.add(new ColumnDefinition(name, columnType, operator, dataType, index));
        }
        return columns;
    }

    @SuppressWarnings("unchecked")
    private List<RowDefinition> parseRows(String ruleId, Map<String, Object> def) {
        Object rowsRaw = def.get("rows");
        if (rowsRaw == null) {
            throw new RuleCompilationException(ruleId, "Missing 'rows' in definition");
        }
        if (!(rowsRaw instanceof List<?> rowList)) {
            throw new RuleCompilationException(ruleId, "'rows' must be a list");
        }

        List<RowDefinition> rows = new ArrayList<>();
        for (int i = 0; i < rowList.size(); i++) {
            Object item = rowList.get(i);
            if (!(item instanceof Map<?, ?> rowMap)) {
                throw new RuleCompilationException(ruleId, "Row at index " + i + " must be a map");
            }
            Map<String, Object> row = (Map<String, Object>) rowMap;

            int rowNumber = row.containsKey("rowNumber")
                    ? ((Number) row.get("rowNumber")).intValue()
                    : i + 1;
            int priority = row.containsKey("priority")
                    ? ((Number) row.get("priority")).intValue()
                    : 0;

            List<CellValue> conditions = parseCellValues(row.get("conditions"), ruleId, i, "conditions");
            List<CellValue> actions = parseCellValues(row.get("actions"), ruleId, i, "actions");

            rows.add(new RowDefinition(rowNumber, priority, conditions, actions));
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<CellValue> parseCellValues(Object raw, String ruleId, int rowIndex, String field) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId,
                    "rows[" + rowIndex + "]." + field + " must be a list");
        }

        List<CellValue> cellValues = new ArrayList<>();
        for (Object cell : list) {
            if (cell == null) {
                cellValues.add(new CellValue(null, null));
            } else if (cell instanceof Map<?, ?> cellMap) {
                Map<String, Object> map = (Map<String, Object>) cellMap;
                cellValues.add(new CellValue(map.get("value"), map.get("secondValue")));
            } else {
                // Simple value
                cellValues.add(new CellValue(cell, null));
            }
        }
        return cellValues;
    }

    private ColumnType parseColumnType(String value, String ruleId, int index) {
        try {
            return ColumnType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleCompilationException(ruleId,
                    "Invalid columnType at columns[" + index + "]: " + value);
        }
    }

    private FactType parseFactType(String value, String ruleId, int index) {
        try {
            return FactType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleCompilationException(ruleId,
                    "Invalid dataType at columns[" + index + "]: " + value);
        }
    }

    private String requireString(Map<String, Object> map, String key, String ruleId, String path) {
        Object val = map.get(key);
        if (val == null) {
            throw new RuleCompilationException(ruleId, "Missing required field '" + key + "' at " + path);
        }
        return val.toString();
    }

    // -----------------------------------------------------------------------
    // Index building
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link ConditionIndex} for each condition column from the compiled rows.
     */
    private Map<Integer, ConditionIndex> buildIndexes(List<ColumnDefinition> conditionColumns,
                                                       List<CompiledRow> compiledRows) {
        Map<Integer, ConditionIndex> indexes = new LinkedHashMap<>();

        for (int colIdx = 0; colIdx < conditionColumns.size(); colIdx++) {
            ColumnDefinition col = conditionColumns.get(colIdx);
            ConditionIndex index = ConditionIndexBuilder.create(col.operator());

            List<ConditionIndex.IndexEntry> entries = new ArrayList<>();
            for (CompiledRow row : compiledRows) {
                if (colIdx < row.conditionValues().size()) {
                    CellValue cell = row.conditionValues().get(colIdx);
                    entries.add(new ConditionIndex.IndexEntry(
                            row.rowNumber(),
                            cell.value(),
                            cell.secondValue()
                    ));
                }
            }

            index.build(entries);
            indexes.put(col.index(), index);
        }

        return indexes;
    }
}
