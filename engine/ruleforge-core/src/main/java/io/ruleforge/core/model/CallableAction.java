package io.ruleforge.core.model;

import java.util.Map;

/**
 * Specification for a callable action that can be executed by an {@link io.ruleforge.core.spi.ActionExecutor}.
 * Used in action nodes of decision trees, flows, and other rule types to invoke
 * functions (HTTP calls, sub-rule invocations, scripts, logging) beyond static outputs.
 *
 * @param actionId      unique identifier for this action within the node
 * @param protocol      executor protocol ("http", "rule", "script", "log")
 * @param target        action target (URL, rule alias, script body, message template)
 * @param parameters    static and expression parameters for the action
 * @param outputMapping optional key to store the action result under in working memory
 */
public record CallableAction(
        String actionId,
        String protocol,
        String target,
        Map<String, Object> parameters,
        String outputMapping
) {
    public CallableAction {
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("actionId must not be blank");
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("protocol must not be blank");
        }
        if (target == null) {
            target = "";
        }
        if (parameters == null) {
            parameters = Map.of();
        }
    }
}
