package io.ruleforge.core.spi;

import io.ruleforge.core.model.FactType;

/**
 * SPI for registering custom operators (e.g., PHONETIC_MATCH, GEO_WITHIN).
 */
public interface OperatorExtension {

    /**
     * Operator name as used in rule conditions (e.g., "PHONETIC_MATCH").
     */
    String name();

    /**
     * Evaluate this operator against two operands.
     *
     * @param left the left operand (fact value)
     * @param right the right operand (condition value)
     * @param ctx evaluation context
     * @return true if the condition is satisfied
     */
    boolean evaluate(Object left, Object right, EvaluationContext ctx);

    /**
     * Whether this operator supports the given fact type.
     */
    boolean supportsType(FactType type);
}
