package io.ruleforge.core;

/**
 * Thrown when a rule evaluation fails at runtime.
 */
public class RuleEvaluationException extends RuleEngineException {

    private final String ruleId;

    public RuleEvaluationException(String ruleId, String message) {
        super("Evaluation failed for rule '" + ruleId + "': " + message);
        this.ruleId = ruleId;
    }

    public RuleEvaluationException(String ruleId, String message, Throwable cause) {
        super("Evaluation failed for rule '" + ruleId + "': " + message, cause);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
