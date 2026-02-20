package io.ruleforge.core.spi;

import io.ruleforge.core.memory.WorkingMemory;

import java.util.Map;

/**
 * Context available during rule evaluation, providing access to
 * working memory, configuration, and runtime services.
 */
public interface EvaluationContext {

    WorkingMemory getWorkingMemory();

    String getCorrelationId();

    Map<String, String> getHierarchyContext();

    /**
     * Get a named configuration property.
     */
    Object getProperty(String key);

    /**
     * Get the current evaluation depth (for nested rule invocations).
     */
    int getDepth();
}
