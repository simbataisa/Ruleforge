package io.ruleforge.core.engine;

import io.ruleforge.core.memory.FactChange;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.TraceNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link DecisionTrace} trees for explainability.
 *
 * <p>Collects condition evaluations, action executions, rule invocations,
 * and fact changes that occur during rule evaluation, then assembles them
 * into a complete trace structure.
 */
public class DecisionTraceBuilder {

    private final String ruleId;
    private final long startTimeNanos;
    private final List<TraceNode> nodes = new ArrayList<>();
    private final List<FactChange> factChanges = new ArrayList<>();

    /**
     * Creates a new trace builder for the given rule.
     *
     * @param ruleId the identifier of the rule being traced
     */
    public DecisionTraceBuilder(String ruleId) {
        this.ruleId = ruleId;
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Records a condition evaluation.
     *
     * @param expression the condition expression as a string
     * @param left       the left operand value
     * @param op         the operator (e.g., "==", ">", "CONTAINS")
     * @param right      the right operand value
     * @param result     whether the condition evaluated to true or false
     */
    public void addConditionEval(String expression, Object left, String op, Object right, boolean result) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startTimeNanos);
        nodes.add(new TraceNode.ConditionEval(expression, left, op, right, result, elapsed));
    }

    /**
     * Records an action execution (fact modification).
     *
     * @param action the action type (e.g., "SET", "MODIFY", "RETRACT")
     * @param path   the fact path affected
     * @param oldVal the previous value
     * @param newVal the new value
     */
    public void addActionExec(String action, String path, Object oldVal, Object newVal) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startTimeNanos);
        nodes.add(new TraceNode.ActionExec(action, path, oldVal, newVal, elapsed));
    }

    /**
     * Records a nested rule invocation.
     *
     * @param ruleId   the invoked rule's identifier
     * @param ruleName the invoked rule's name
     * @param result   the result produced by the invoked rule
     */
    public void addRuleInvocation(String ruleId, String ruleName, RuleResult result) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startTimeNanos);
        // Extract child trace nodes from the invoked result if available
        List<TraceNode> children = new ArrayList<>();
        if (result.trace() != null && result.trace().rootNode() != null) {
            children.add(result.trace().rootNode());
        }
        nodes.add(new TraceNode.RuleInvocation(ruleId, ruleName, result, elapsed, children));
    }

    /**
     * Records fact changes that occurred during evaluation.
     *
     * @param changes the list of fact changes to record
     */
    public void addFactChanges(List<FactChange> changes) {
        if (changes != null) {
            factChanges.addAll(changes);
        }
    }

    /**
     * Builds the final {@link DecisionTrace} from all recorded nodes and fact changes.
     *
     * <p>If multiple trace nodes were recorded, they are wrapped in a single
     * {@link TraceNode.RuleInvocation} as the root. If exactly one node was recorded,
     * it becomes the root directly.
     *
     * @return the completed decision trace
     */
    public DecisionTrace build() {
        Duration totalTime = Duration.ofNanos(System.nanoTime() - startTimeNanos);

        TraceNode rootNode;
        if (nodes.isEmpty()) {
            rootNode = null;
        } else if (nodes.size() == 1) {
            rootNode = nodes.getFirst();
        } else {
            // Wrap multiple nodes as children of a root RuleInvocation
            rootNode = new TraceNode.RuleInvocation(
                    ruleId,
                    ruleId,
                    null,
                    totalTime,
                    List.copyOf(nodes));
        }

        return new DecisionTrace(ruleId, rootNode, List.copyOf(factChanges), totalTime);
    }
}
