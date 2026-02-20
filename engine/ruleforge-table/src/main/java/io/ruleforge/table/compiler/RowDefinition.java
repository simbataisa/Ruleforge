package io.ruleforge.table.compiler;

import java.util.List;

/**
 * Represents a single row in a decision table before compilation.
 *
 * @param rowNumber  the 1-based row number in the table
 * @param priority   priority for conflict resolution (higher = more important)
 * @param conditions list of cell values, one per condition column (positional)
 * @param actions    list of cell values, one per action column (positional)
 */
public record RowDefinition(
        int rowNumber,
        int priority,
        List<CellValue> conditions,
        List<CellValue> actions
) {

    public RowDefinition {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /**
     * A single cell value in a decision table row.
     *
     * @param value       the primary value (or the lower bound for BETWEEN)
     * @param secondValue the secondary value (upper bound for BETWEEN); null otherwise
     */
    public record CellValue(Object value, Object secondValue) {

        /**
         * Convenience constructor for single-valued cells.
         */
        public CellValue(Object value) {
            this(value, null);
        }

        /**
         * Returns {@code true} if this cell has no value specified (wildcard / any match).
         */
        public boolean isWildcard() {
            return value == null && secondValue == null;
        }
    }
}
