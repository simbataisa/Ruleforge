package io.ruleforge;

import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.RuleForgeEngineBuilder;

/**
 * Convenience factory for creating a {@link RuleForgeEngine} instance.
 * <p>
 * This class is the recommended entry point when using the all-in-one
 * embedded JAR ({@code ruleforge-engine-all.jar}). It bundles the core
 * engine together with the table, tree, scorecard, and ensemble modules
 * so that a single dependency is sufficient for most use cases.
 *
 * <pre>{@code
 * RuleForgeEngine engine = RuleForge.engine()
 *     .withTracing(true)
 *     .build();
 * }</pre>
 */
public final class RuleForge {

    private RuleForge() {
        // utility class
    }

    /**
     * Returns a new {@link RuleForgeEngineBuilder} pre-configured with
     * sensible defaults for embedded usage.
     *
     * @return a builder instance
     */
    public static RuleForgeEngineBuilder engine() {
        return RuleForgeEngine.builder();
    }

    /**
     * Returns the version of the RuleForge engine distribution.
     *
     * @return version string, or {@code "unknown"} if unavailable
     */
    public static String version() {
        Package pkg = RuleForge.class.getPackage();
        String v = (pkg != null) ? pkg.getImplementationVersion() : null;
        return (v != null) ? v : "unknown";
    }
}
