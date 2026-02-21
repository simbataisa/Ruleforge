package io.ruleforge.dmn.model;

import io.ruleforge.dmn.feel.FeelType;

/**
 * An input data element in a DMN definition.
 *
 * @param id   unique identifier
 * @param name human-readable name
 * @param type the FEEL type of this input
 */
public record DmnInputData(
        String id,
        String name,
        FeelType type
) {}
