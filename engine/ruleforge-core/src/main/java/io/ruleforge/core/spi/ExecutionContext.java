package io.ruleforge.core.spi;

import io.ruleforge.core.model.ExecutionMode;

import java.time.Duration;

/**
 * Context available during conflict resolution and execution control.
 */
public interface ExecutionContext {

    ExecutionMode getExecutionMode();

    Duration getTimeout();

    int getCurrentIteration();

    int getMaxIterations();

    String getCorrelationId();
}
