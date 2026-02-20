package io.ruleforge.core.engine;

import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.InputSchema;
import io.ruleforge.core.model.ValidationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates input data against a rule's {@link InputSchema} before evaluation.
 *
 * <p>Checks that all required fields are present and that field values are
 * compatible with the types declared in the schema.
 */
public class InputValidator {

    /**
     * Validates the given input map against the provided schema.
     *
     * @param input  the input data to validate
     * @param schema the input schema defining expected fields
     * @return a {@link ValidationResult} containing any errors or warnings found
     */
    public ValidationResult validate(Map<String, Object> input, InputSchema schema) {
        if (schema == null) {
            return ValidationResult.success();
        }

        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();

        // Check required fields are present
        if (schema.required() != null) {
            for (String requiredField : schema.required()) {
                if (!input.containsKey(requiredField) || input.get(requiredField) == null) {
                    errors.add(new ValidationResult.ValidationError(
                            requiredField,
                            "MISSING_REQUIRED_FIELD",
                            "Required field '" + requiredField + "' is missing or null"));
                }
            }
        }

        // Check field types match schema
        if (schema.fields() != null) {
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                InputSchema.FieldDefinition fieldDef = schema.fields().get(fieldName);
                if (fieldDef == null) {
                    // Field not declared in schema -- warn but don't error
                    warnings.add(new ValidationResult.ValidationWarning(
                            fieldName,
                            "UNDECLARED_FIELD",
                            "Field '" + fieldName + "' is not declared in the input schema"));
                    continue;
                }

                if (value == null) {
                    // Null values are handled by the required check above
                    continue;
                }

                if (!isTypeCompatible(value, fieldDef.type())) {
                    errors.add(new ValidationResult.ValidationError(
                            fieldName,
                            "TYPE_MISMATCH",
                            "Field '" + fieldName + "' expects type " + fieldDef.type()
                                    + " but got " + value.getClass().getSimpleName()));
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Checks whether a value is compatible with the declared {@link FactType}.
     */
    private boolean isTypeCompatible(Object value, FactType expectedType) {
        if (expectedType == null || expectedType == FactType.ANY) {
            return true;
        }

        return switch (expectedType) {
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case INTEGER -> value instanceof Integer || value instanceof Long;
            case BOOLEAN -> value instanceof Boolean;
            case DATE -> value instanceof LocalDate;
            case DATETIME -> value instanceof Instant;
            case ARRAY -> value instanceof List;
            case OBJECT -> value instanceof Map;
            case MONEY -> value instanceof BigDecimal || value instanceof Number;
            case ENUM -> value instanceof String;
            case ANY -> true;
        };
    }
}
