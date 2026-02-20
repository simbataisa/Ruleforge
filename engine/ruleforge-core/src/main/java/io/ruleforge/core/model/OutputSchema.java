package io.ruleforge.core.model;

import java.util.Map;

/**
 * Describes the output contract for a rule unit.
 *
 * @param fields Map of field name to its definition.
 */
public record OutputSchema(
        Map<String, FieldDefinition> fields
) {

    /**
     * Definition of a single output field.
     *
     * @param name        Field name.
     * @param type        The logical fact type.
     * @param description Human-readable description.
     */
    public record FieldDefinition(
            String name,
            FactType type,
            String description
    ) {}
}
