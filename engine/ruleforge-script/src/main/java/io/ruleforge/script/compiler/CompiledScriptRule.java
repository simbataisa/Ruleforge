package io.ruleforge.script.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.script.sandbox.ScriptContextPool;
import io.ruleforge.script.sandbox.ScriptSandboxConfig;

/**
 * Compiled representation of a scripting rule.
 * Holds the pre-compiled script body and sandbox configuration.
 */
public final class CompiledScriptRule extends CompiledRule {

    private final String scriptBody;
    private final String language;
    private final long timeoutMs;
    private final int maxMemoryMb;
    private final ScriptContextPool contextPool;

    public CompiledScriptRule(String ruleId,
                               RuleMetadata metadata,
                               long compiledAt,
                               String scriptBody,
                               String language,
                               long timeoutMs,
                               int maxMemoryMb) {
        super(ruleId, RuleType.SCRIPTING_RULE, metadata, compiledAt);
        this.scriptBody = scriptBody;
        this.language = language;
        this.timeoutMs = timeoutMs;
        this.maxMemoryMb = maxMemoryMb;
        this.contextPool = new ScriptContextPool(new ScriptSandboxConfig(timeoutMs, maxMemoryMb, true));
    }

    public String scriptBody() { return scriptBody; }
    public String language() { return language; }
    public long timeoutMs() { return timeoutMs; }
    public int maxMemoryMb() { return maxMemoryMb; }
    public ScriptContextPool contextPool() { return contextPool; }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        // Delegated to ScriptingEngine which manages context lifecycle
        throw new UnsupportedOperationException("Use ScriptingEngine.evaluate() directly");
    }
}
