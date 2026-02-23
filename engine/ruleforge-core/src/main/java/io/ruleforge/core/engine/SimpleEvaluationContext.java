package io.ruleforge.core.engine;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.EvaluationContext;

import java.util.Collections;
import java.util.Map;

/**
 * Minimal evaluation context for direct engine evaluation without a full
 * solver-service context. Provides working memory access only.
 */
final class SimpleEvaluationContext implements EvaluationContext {

    private final WorkingMemory workingMemory;

    SimpleEvaluationContext(WorkingMemory workingMemory) {
        this.workingMemory = workingMemory;
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
}
