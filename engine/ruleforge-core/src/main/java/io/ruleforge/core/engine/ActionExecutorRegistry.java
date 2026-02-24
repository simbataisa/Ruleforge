package io.ruleforge.core.engine;

import io.ruleforge.core.spi.ActionExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link ActionExecutor} instances keyed by protocol.
 * Thread-safe for concurrent reads; registration is expected at startup.
 */
public final class ActionExecutorRegistry {

    private final Map<String, ActionExecutor> executors = new ConcurrentHashMap<>();

    /**
     * Register an action executor for a protocol.
     *
     * @param executor the executor to register
     * @return this registry for chaining
     */
    public ActionExecutorRegistry register(ActionExecutor executor) {
        executors.put(executor.protocol(), executor);
        return this;
    }

    /**
     * Get the executor for a protocol, or {@code null} if none registered.
     */
    public ActionExecutor get(String protocol) {
        return executors.get(protocol);
    }

    /**
     * Check if an executor is registered for a protocol.
     */
    public boolean hasExecutor(String protocol) {
        return executors.containsKey(protocol);
    }
}
