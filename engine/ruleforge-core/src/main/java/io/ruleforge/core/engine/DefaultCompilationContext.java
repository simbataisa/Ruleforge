package io.ruleforge.core.engine;

import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.spi.CompilationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link CompilationContext} that collects errors and warnings
 * during rule compilation phases.
 */
public class DefaultCompilationContext implements CompilationContext {

    private final RuleDefinition definition;
    private final List<ValidationResult.ValidationError> errors = new ArrayList<>();
    private final List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();

    /**
     * Creates a new compilation context for the given rule definition.
     *
     * @param definition the rule definition being compiled
     */
    public DefaultCompilationContext(RuleDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
    }

    @Override
    public RuleDefinition getDefinition() {
        return definition;
    }

    @Override
    public void addWarning(String code, String message) {
        warnings.add(new ValidationResult.ValidationWarning(
                definition.id(), code, message));
    }

    @Override
    public void addError(String code, String message) {
        errors.add(new ValidationResult.ValidationError(
                definition.id(), code, message));
    }

    /**
     * Returns {@code true} if any compilation errors have been recorded.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns an unmodifiable view of the collected errors.
     */
    public List<ValidationResult.ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Returns an unmodifiable view of the collected warnings.
     */
    public List<ValidationResult.ValidationWarning> getWarnings() {
        return List.copyOf(warnings);
    }

    /**
     * Builds a {@link ValidationResult} from the collected errors and warnings.
     *
     * @return a validation result that is valid only if no errors were recorded
     */
    public ValidationResult toValidationResult() {
        return new ValidationResult(
                errors.isEmpty(),
                List.copyOf(errors),
                List.copyOf(warnings));
    }
}
