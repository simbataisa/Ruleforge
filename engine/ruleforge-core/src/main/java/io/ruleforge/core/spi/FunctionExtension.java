package io.ruleforge.core.spi;

import io.ruleforge.core.model.FactType;

/**
 * SPI for registering custom functions usable in RFL expressions
 * (e.g., CREDIT_SCORE_BAND, GEO_DISTANCE).
 */
public interface FunctionExtension {

    /**
     * Function name as used in RFL expressions (e.g., "CREDIT_SCORE_BAND").
     */
    String name();

    /**
     * Invoke the function with the given arguments.
     *
     * @param args evaluated argument values
     * @param ctx evaluation context
     * @return the function result
     */
    Object invoke(Object[] args, EvaluationContext ctx);

    /**
     * The function's type signature for validation.
     */
    FunctionSignature signature();

    /**
     * Function signature describing parameter types and return type.
     */
    record FunctionSignature(
        FactType[] parameterTypes,
        FactType returnType,
        boolean varArgs
    ) {}
}
