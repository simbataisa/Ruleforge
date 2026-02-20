package io.ruleforge.core.engine;

import io.ruleforge.core.model.ExecutionMode;
import io.ruleforge.core.spi.ExecutionContext;

import java.time.Duration;
import java.util.Objects;

/**
 * Default implementation of {@link ExecutionContext} providing execution mode,
 * timeout, iteration tracking, and correlation information for conflict resolution
 * and execution control.
 */
public class DefaultExecutionContext implements ExecutionContext {

    private final ExecutionMode executionMode;
    private final Duration timeout;
    private int currentIteration;
    private final int maxIterations;
    private final String correlationId;

    /**
     * Creates a new execution context.
     *
     * @param executionMode    the execution mode governing how matched rules are processed
     * @param timeout          maximum time allowed for the evaluation
     * @param maxIterations    maximum number of forward-chaining iterations
     * @param correlationId    external correlation identifier for distributed tracing
     */
    public DefaultExecutionContext(ExecutionMode executionMode,
                                   Duration timeout,
                                   int maxIterations,
                                   String correlationId) {
        this.executionMode = Objects.requireNonNull(executionMode, "executionMode must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
        this.correlationId = correlationId;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public int getCurrentIteration() {
        return currentIteration;
    }

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Advances the iteration counter by one.
     * Used by the forward-chaining controller to track loop progress.
     */
    public void incrementIteration() {
        this.currentIteration++;
    }

    /**
     * Sets the current iteration to a specific value.
     * Used by the forward-chaining controller when resetting or adjusting iteration state.
     *
     * @param currentIteration the iteration number to set
     */
    public void setCurrentIteration(int currentIteration) {
        this.currentIteration = currentIteration;
    }
}
