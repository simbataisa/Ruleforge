package io.ruleforge.core.spi;

import java.util.Map;

/**
 * SPI for executing callable actions during rule evaluation.
 * Each executor handles a specific protocol (e.g., "http", "rule", "script", "log").
 * Follows the same pattern as {@link DataEnricherExtension}.
 */
public interface ActionExecutor {

    /**
     * The protocol this executor handles (e.g., "http", "rule", "script", "log").
     */
    String protocol();

    /**
     * Execute the action and return the result as key-value pairs.
     *
     * @param target the action target (URL, rule alias, script body, message template)
     * @param params parameters for the action
     * @param ctx    evaluation context
     * @return action result as key-value pairs
     */
    Map<String, Object> execute(String target, Map<String, Object> params, EvaluationContext ctx);
}
