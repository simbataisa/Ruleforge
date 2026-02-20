package io.ruleforge.core;

/**
 * Thrown when a rule definition fails to compile.
 */
public class RuleCompilationException extends RuleEngineException {

    private final String ruleId;

    public RuleCompilationException(String ruleId, String message) {
        super("Compilation failed for rule '" + ruleId + "': " + message);
        this.ruleId = ruleId;
    }

    public RuleCompilationException(String ruleId, String message, Throwable cause) {
        super("Compilation failed for rule '" + ruleId + "': " + message, cause);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
