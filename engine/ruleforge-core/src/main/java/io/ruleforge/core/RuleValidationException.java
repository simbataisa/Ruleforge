package io.ruleforge.core;

import io.ruleforge.core.model.ValidationResult;

/**
 * Thrown when a rule definition fails validation.
 */
public class RuleValidationException extends RuleEngineException {

    private final ValidationResult validationResult;

    public RuleValidationException(ValidationResult validationResult) {
        super("Rule validation failed with " + validationResult.errors().size() + " error(s)");
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
