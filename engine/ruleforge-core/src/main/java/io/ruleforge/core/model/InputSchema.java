package io.ruleforge.core.model;

import java.util.Map;
import java.util.Set;

/**
 * Describes the input contract for a rule unit.
 *
 * @param fields   Map of field name to its definition.
 * @param required Set of field names that must be present in the input.
 */
public record InputSchema(
        Map<String, FieldDefinition> fields,
        Set<String> required
) {

    /**
     * Definition of a single input field.
     *
     * @param name         Field name.
     * @param type         The logical fact type.
     * @param defaultValue Default value used when the field is absent.
     * @param description  Human-readable description.
     */
    public record FieldDefinition(
            String name,
            FactType type,
            Object defaultValue,
            String description
    ) {}
}
