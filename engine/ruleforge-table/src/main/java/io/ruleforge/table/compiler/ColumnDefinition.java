package io.ruleforge.table.compiler;

import io.ruleforge.core.model.FactType;

/**
 * Represents a single column in a decision table.
 *
 * @param name       column name matching an input or output schema field
 * @param columnType whether this column is a CONDITION or ACTION
 * @param operator   comparison operator (EQ, GT, GTE, LT, LTE, BETWEEN, IN, CONTAINS, REGEX, STARTS_WITH, IS_NULL, IS_NOT_NULL)
 * @param dataType   expected data type for values in this column
 * @param index      zero-based column position in the table
 */
public record ColumnDefinition(
        String name,
        ColumnType columnType,
        String operator,
        FactType dataType,
        int index
) {

    /**
     * Discriminator for whether a column defines a condition to match or an action to produce.
     */
    public enum ColumnType {
        CONDITION,
        ACTION
    }
}
