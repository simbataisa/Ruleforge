package io.ruleforge.script.sandbox;

import org.graalvm.polyglot.Context;

/**
 * Creates GraalVM execution contexts with sandbox constraints applied.
 * <p>
 * GraalVM contexts are NOT thread-safe; each evaluation creates a fresh context
 * and closes it in a finally block to prevent resource leaks.
 */
public final class ScriptContextPool {

    private final ScriptSandboxConfig config;

    public ScriptContextPool(ScriptSandboxConfig config) {
        this.config = config;
    }

    /**
     * Creates a new sandboxed GraalVM context for the given language.
     * Caller is responsible for closing the returned context.
     */
    public Context createContext(String language) {
        Context.Builder builder = Context.newBuilder(language)
                .allowAllAccess(false)
                .allowIO(org.graalvm.polyglot.io.IOAccess.NONE)
                .option("engine.WarnInterpreterOnly", "false");

        if (config.denyHostAccess()) {
            builder.allowHostAccess(org.graalvm.polyglot.HostAccess.NONE);
            builder.allowNativeAccess(false);
            builder.allowCreateThread(false);
        }

        org.graalvm.polyglot.ResourceLimits limits = ResourceLimits.build(config);
        builder.resourceLimits(limits);

        return builder.build();
    }
}
