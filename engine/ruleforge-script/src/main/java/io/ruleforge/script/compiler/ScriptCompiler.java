package io.ruleforge.script.compiler;

import io.ruleforge.core.model.RuleDefinition;

import java.util.Map;

/**
 * Compiles a script rule definition into a {@link CompiledScriptRule}.
 */
public final class ScriptCompiler {

    /**
     * Parses the definition payload and creates a compiled script rule.
     * Expected payload format:
     * <pre>
     * {
     *   "scriptBody": "function evaluate(input) { return { approved: input.age >= 18 }; }",
     *   "language": "js",
     *   "timeout": 5000,
     *   "maxMemoryMb": 64
     * }
     * </pre>
     */
    public CompiledScriptRule compile(RuleDefinition definition) {
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            throw new IllegalArgumentException("Script definition payload is null for rule: " + definition.id());
        }

        String scriptBody = (String) payload.get("scriptBody");
        if (scriptBody == null || scriptBody.isBlank()) {
            throw new IllegalArgumentException("Script definition must have a non-empty 'scriptBody'");
        }

        String language = (String) payload.getOrDefault("language", "js");
        long timeoutMs = ((Number) payload.getOrDefault("timeout", 5000)).longValue();
        int maxMemoryMb = ((Number) payload.getOrDefault("maxMemoryMb", 64)).intValue();

        return new CompiledScriptRule(
                definition.id(),
                definition.metadata(),
                System.currentTimeMillis(),
                scriptBody,
                language,
                timeoutMs,
                maxMemoryMb
        );
    }
}
