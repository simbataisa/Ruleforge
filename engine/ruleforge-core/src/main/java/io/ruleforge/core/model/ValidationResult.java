package io.ruleforge.core.model;

import java.util.List;

/**
 * Result of validating a rule unit's structural integrity.
 *
 * @param valid    {@code true} if no errors were found.
 * @param errors   List of validation errors (empty when valid).
 * @param warnings List of non-fatal warnings.
 */
public record ValidationResult(
        boolean valid,
        List<ValidationError> errors,
        List<ValidationWarning> warnings
) {

    /**
     * A validation error that prevents the rule from being compiled or executed.
     *
     * @param path    JSON-path or logical path to the problematic element.
     * @param code    Machine-readable error code.
     * @param message Human-readable error description.
     */
    public record ValidationError(String path, String code, String message) {}

    /**
     * A non-fatal warning about a potential issue.
     *
     * @param path    JSON-path or logical path to the element.
     * @param code    Machine-readable warning code.
     * @param message Human-readable warning description.
     */
    public record ValidationWarning(String path, String code, String message) {}

    /**
     * Returns a successful validation result with no errors or warnings.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), List.of());
    }

    /**
     * Returns a failed validation result with the given errors and no warnings.
     *
     * @param errors the list of validation errors
     * @return a failed {@link ValidationResult}
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, List.copyOf(errors), List.of());
    }
}
