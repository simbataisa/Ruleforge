package io.ruleforge.script.sandbox;

import org.graalvm.polyglot.Context;

/**
 * Applies resource limits to a GraalVM execution context.
 */
public final class ResourceLimits {

    private ResourceLimits() {}

    /**
     * Builds a GraalVM ResourceLimits object from sandbox config.
     * Uses statement count as a proxy for CPU time.
     */
    public static org.graalvm.polyglot.ResourceLimits build(ScriptSandboxConfig config) {
        // Approximate CPU limit: ~100M statements per second, so timeoutMs * 100_000
        long statementLimit = Math.max(1_000_000L, config.timeoutMs() * 100_000L);
        return org.graalvm.polyglot.ResourceLimits.newBuilder()
                .statementLimit(statementLimit, null)
                .build();
    }
}
