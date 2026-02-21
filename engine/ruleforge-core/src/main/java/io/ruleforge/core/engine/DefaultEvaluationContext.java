package io.ruleforge.core.engine;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.SubRuleInvoker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link EvaluationContext} providing access to
 * working memory, correlation tracking, hierarchy context, and custom properties
 * during rule evaluation.
 */
public class DefaultEvaluationContext implements EvaluationContext {

    private final WorkingMemory workingMemory;
    private final String correlationId;
    private final Map<String, String> hierarchyContext;
    private final Map<String, Object> properties;
    private final int depth;
    private final SubRuleInvoker subRuleInvoker;

    /**
     * Creates a new evaluation context.
     *
     * @param workingMemory    the working memory for this evaluation
     * @param correlationId    external correlation identifier for distributed tracing
     * @param hierarchyContext context keys for cascading/hierarchical evaluation
     * @param properties       custom configuration properties
     * @param depth            current evaluation depth (for nested rule invocations)
     * @param subRuleInvoker   optional invoker for sub-rule delegation (may be null)
     */
    public DefaultEvaluationContext(WorkingMemory workingMemory,
                                    String correlationId,
                                    Map<String, String> hierarchyContext,
                                    Map<String, Object> properties,
                                    int depth,
                                    SubRuleInvoker subRuleInvoker) {
        this.workingMemory = Objects.requireNonNull(workingMemory, "workingMemory must not be null");
        this.correlationId = correlationId;
        this.hierarchyContext = hierarchyContext == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(hierarchyContext));
        this.properties = properties == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(properties));
        this.depth = depth;
        this.subRuleInvoker = subRuleInvoker;
    }

    /**
     * Creates a new evaluation context without a sub-rule invoker.
     */
    public DefaultEvaluationContext(WorkingMemory workingMemory,
                                    String correlationId,
                                    Map<String, String> hierarchyContext,
                                    Map<String, Object> properties,
                                    int depth) {
        this(workingMemory, correlationId, hierarchyContext, properties, depth, null);
    }

    /**
     * Convenience constructor for simple evaluations at depth 0 with no hierarchy or properties.
     */
    public DefaultEvaluationContext(WorkingMemory workingMemory, String correlationId) {
        this(workingMemory, correlationId, Map.of(), Map.of(), 0, null);
    }

    @Override
    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public Map<String, String> getHierarchyContext() {
        return hierarchyContext;
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public SubRuleInvoker getSubRuleInvoker() {
        return subRuleInvoker;
    }

    /**
     * Returns a new context with the depth incremented by one, suitable for nested invocations.
     */
    public DefaultEvaluationContext withIncrementedDepth() {
        return new DefaultEvaluationContext(
                workingMemory, correlationId, hierarchyContext, properties, depth + 1, subRuleInvoker);
    }
}
