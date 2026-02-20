package io.ruleforge.script.sandbox;

/**
 * Configuration record for script sandbox resource limits.
 */
public record ScriptSandboxConfig(
        long timeoutMs,
        int maxMemoryMb,
        boolean denyHostAccess
) {
    public static ScriptSandboxConfig defaults() {
        return new ScriptSandboxConfig(5000, 64, true);
    }
}
