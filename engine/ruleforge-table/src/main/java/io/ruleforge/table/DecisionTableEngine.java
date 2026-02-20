package io.ruleforge.table;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.InputSchema;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import io.ruleforge.core.model.ValidationResult.ValidationWarning;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.table.compiler.CompiledDecisionTable;
import io.ruleforge.table.compiler.TableCompiler;
import io.ruleforge.table.evaluator.AggregateEvaluator;
import io.ruleforge.table.evaluator.AllMatchEvaluator;
import io.ruleforge.table.evaluator.FirstMatchEvaluator;
import io.ruleforge.table.evaluator.HitPolicy;
import io.ruleforge.table.evaluator.PriorityEvaluator;
import io.ruleforge.table.evaluator.RankedEvaluator;
import io.ruleforge.table.evaluator.TableEvaluator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine for evaluating decision table rules.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for {@link RuleType#DECISION_TABLE}.
 * Supports indexed condition matching and multiple hit policies (FIRST, ALL,
 * PRIORITY, AGGREGATE, RANKED).
 */
public final class DecisionTableEngine implements RuleTypeEngine {

    private final TableCompiler compiler = new TableCompiler();
    private final Map<HitPolicy, TableEvaluator> evaluators;

    public DecisionTableEngine() {
        evaluators = new EnumMap<>(HitPolicy.class);
        evaluators.put(HitPolicy.FIRST, new FirstMatchEvaluator());
        evaluators.put(HitPolicy.ALL, new AllMatchEvaluator());
        evaluators.put(HitPolicy.PRIORITY, new PriorityEvaluator());
        evaluators.put(HitPolicy.AGGREGATE, new AggregateEvaluator());
        evaluators.put(HitPolicy.RANKED, new RankedEvaluator());
    }

    @Override
    public RuleType supportedType() {
        return RuleType.DECISION_TABLE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        if (definition.type() != RuleType.DECISION_TABLE) {
            throw new RuleCompilationException(definition.id(),
                    "Expected DECISION_TABLE but got " + definition.type());
        }
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledDecisionTable table)) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Expected CompiledDecisionTable but got " + rule.getClass().getSimpleName());
        }

        TableEvaluator evaluator = evaluators.get(table.hitPolicy());
        if (evaluator == null) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "No evaluator registered for hit policy: " + table.hitPolicy());
        }

        return evaluator.evaluate(table, workingMemory, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (definition.type() != RuleType.DECISION_TABLE) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected DECISION_TABLE but got " + definition.type()));
            return new ValidationResult(false, errors, warnings);
        }

        Map<String, Object> def = definition.definition();
        if (def == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Definition payload is null"));
            return new ValidationResult(false, errors, warnings);
        }

        // Validate hitPolicy
        Object hitPolicyRaw = def.get("hitPolicy");
        if (hitPolicyRaw != null) {
            try {
                HitPolicy.valueOf(hitPolicyRaw.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationError("definition.hitPolicy", "INVALID_HIT_POLICY",
                        "Unknown hit policy: " + hitPolicyRaw +
                                ". Valid values: FIRST, ALL, PRIORITY, AGGREGATE, RANKED"));
            }
        }

        // Validate columns
        Object columnsRaw = def.get("columns");
        if (columnsRaw == null) {
            errors.add(new ValidationError("definition.columns", "MISSING_COLUMNS",
                    "Decision table must have a 'columns' list"));
        } else if (!(columnsRaw instanceof List<?> columnList)) {
            errors.add(new ValidationError("definition.columns", "INVALID_COLUMNS",
                    "'columns' must be a list"));
        } else {
            validateColumns(columnList, definition, errors, warnings);
        }

        // Validate rows
        Object rowsRaw = def.get("rows");
        if (rowsRaw == null) {
            errors.add(new ValidationError("definition.rows", "MISSING_ROWS",
                    "Decision table must have a 'rows' list"));
        } else if (!(rowsRaw instanceof List<?> rowList)) {
            errors.add(new ValidationError("definition.rows", "INVALID_ROWS",
                    "'rows' must be a list"));
        } else {
            if (rowList.isEmpty()) {
                warnings.add(new ValidationWarning("definition.rows", "EMPTY_ROWS",
                        "Decision table has no rows"));
            }
            validateRows(rowList, errors, warnings);
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    // -----------------------------------------------------------------------
    // Validation helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void validateColumns(List<?> columnList, RuleDefinition definition,
                                  List<ValidationError> errors, List<ValidationWarning> warnings) {
        boolean hasCondition = false;
        boolean hasAction = false;
        Set<String> columnNames = new HashSet<>();
        Set<String> validOperators = Set.of(
                "EQ", "NEQ", "GT", "GTE", "LT", "LTE", "BETWEEN", "IN", "NOT_IN",
                "CONTAINS", "STARTS_WITH", "REGEX", "IS_NULL", "IS_NOT_NULL"
        );

        for (int i = 0; i < columnList.size(); i++) {
            Object item = columnList.get(i);
            if (!(item instanceof Map<?, ?> colMap)) {
                errors.add(new ValidationError("definition.columns[" + i + "]", "INVALID_COLUMN",
                        "Column must be a map"));
                continue;
            }
            Map<String, Object> col = (Map<String, Object>) colMap;

            // Validate name
            Object name = col.get("name");
            if (name == null || name.toString().isBlank()) {
                errors.add(new ValidationError("definition.columns[" + i + "].name",
                        "MISSING_NAME", "Column name is required"));
            } else {
                String nameStr = name.toString();
                if (!columnNames.add(nameStr)) {
                    errors.add(new ValidationError("definition.columns[" + i + "].name",
                            "DUPLICATE_NAME", "Duplicate column name: " + nameStr));
                }
            }

            // Validate columnType
            Object columnType = col.get("columnType");
            if (columnType == null) {
                errors.add(new ValidationError("definition.columns[" + i + "].columnType",
                        "MISSING_COLUMN_TYPE", "Column type (CONDITION or ACTION) is required"));
            } else {
                String typeStr = columnType.toString().toUpperCase();
                if ("CONDITION".equals(typeStr)) {
                    hasCondition = true;
                } else if ("ACTION".equals(typeStr)) {
                    hasAction = true;
                } else {
                    errors.add(new ValidationError("definition.columns[" + i + "].columnType",
                            "INVALID_COLUMN_TYPE",
                            "Invalid column type: " + typeStr + ". Must be CONDITION or ACTION"));
                }
            }

            // Validate operator (only for condition columns)
            if (columnType != null && "CONDITION".equalsIgnoreCase(columnType.toString())) {
                Object operator = col.get("operator");
                if (operator != null && !validOperators.contains(operator.toString().toUpperCase())) {
                    warnings.add(new ValidationWarning("definition.columns[" + i + "].operator",
                            "UNKNOWN_OPERATOR",
                            "Unknown operator: " + operator + ". Valid operators: " + validOperators));
                }
            }

            // Validate dataType
            Object dataType = col.get("dataType");
            if (dataType != null) {
                try {
                    FactType.valueOf(dataType.toString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new ValidationError("definition.columns[" + i + "].dataType",
                            "INVALID_DATA_TYPE", "Unknown data type: " + dataType));
                }
            }

            // Cross-reference with input schema for condition columns
            if (name != null && definition.inputSchema() != null
                    && columnType != null && "CONDITION".equalsIgnoreCase(columnType.toString())) {
                InputSchema schema = definition.inputSchema();
                if (schema.fields() != null && !schema.fields().containsKey(name.toString())) {
                    warnings.add(new ValidationWarning("definition.columns[" + i + "].name",
                            "UNMATCHED_INPUT",
                            "Condition column '" + name + "' does not match any input schema field"));
                }
            }
        }

        if (!hasCondition) {
            errors.add(new ValidationError("definition.columns", "NO_CONDITION_COLUMNS",
                    "Decision table must have at least one CONDITION column"));
        }
        if (!hasAction) {
            errors.add(new ValidationError("definition.columns", "NO_ACTION_COLUMNS",
                    "Decision table must have at least one ACTION column"));
        }
    }

    @SuppressWarnings("unchecked")
    private void validateRows(List<?> rowList, List<ValidationError> errors,
                               List<ValidationWarning> warnings) {
        Set<Integer> rowNumbers = new HashSet<>();
        for (int i = 0; i < rowList.size(); i++) {
            Object item = rowList.get(i);
            if (!(item instanceof Map<?, ?> rowMap)) {
                errors.add(new ValidationError("definition.rows[" + i + "]", "INVALID_ROW",
                        "Row must be a map"));
                continue;
            }
            Map<String, Object> row = (Map<String, Object>) rowMap;

            // Validate rowNumber uniqueness
            Object rowNumberRaw = row.get("rowNumber");
            if (rowNumberRaw instanceof Number n) {
                int rowNumber = n.intValue();
                if (!rowNumbers.add(rowNumber)) {
                    errors.add(new ValidationError("definition.rows[" + i + "].rowNumber",
                            "DUPLICATE_ROW_NUMBER", "Duplicate row number: " + rowNumber));
                }
            }

            // Validate conditions and actions presence
            Object conditions = row.get("conditions");
            if (conditions != null && !(conditions instanceof List<?>)) {
                errors.add(new ValidationError("definition.rows[" + i + "].conditions",
                        "INVALID_CONDITIONS", "Row conditions must be a list"));
            }

            Object actions = row.get("actions");
            if (actions != null && !(actions instanceof List<?>)) {
                errors.add(new ValidationError("definition.rows[" + i + "].actions",
                        "INVALID_ACTIONS", "Row actions must be a list"));
            }
        }
    }
}
