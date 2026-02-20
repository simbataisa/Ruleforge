package io.ruleforge.core.spi;

import io.ruleforge.core.model.RuleDefinition;

/**
 * Context available during rule compilation phases.
 */
public interface CompilationContext {

    /**
     * The original rule definition being compiled.
     */
    RuleDefinition getDefinition();

    /**
     * Report a compilation warning.
     */
    void addWarning(String code, String message);

    /**
     * Report a compilation error (will cause compilation to fail).
     */
    void addError(String code, String message);
}
