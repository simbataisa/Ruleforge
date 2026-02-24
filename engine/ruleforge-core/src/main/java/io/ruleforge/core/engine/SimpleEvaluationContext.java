package io.ruleforge.core.engine;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;

import java.util.Collections;
import java.util.Map;

/**
 * Minimal evaluation context for direct engine evaluation without a full
 * solver-service context. Provides working memory access only.
 */
final class SimpleEvaluationContext implements EvaluationContext {

    private final WorkingMemory workingMemory;
    private final ActionExecutorRegistry actionExecutorRegistry;

    SimpleEvaluationContext(WorkingMemory workingMemory) {
        this(workingMemory, null);
    }

    SimpleEvaluationContext(WorkingMemory workingMemory, ActionExecutorRegistry actionExecutorRegistry) {
        this.workingMemory = workingMemory;
        this.actionExecutorRegistry = actionExecutorRegistry;
    }

    @Override
    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    @Override
    public String getCorrelationId() {
        return null;
    }

    @Override
    public Map<String, String> getHierarchyContext() {
        return Collections.emptyMap();
    }

    @Override
    public Object getProperty(String key) {
        return null;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public ActionExecutor getActionExecutor(String protocol) {
        return actionExecutorRegistry != null ? actionExecutorRegistry.get(protocol) : null;
    }
}
