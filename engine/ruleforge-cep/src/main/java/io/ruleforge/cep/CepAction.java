package io.ruleforge.cep;

import java.util.Map;

/**
 * An action to be triggered when a CEP pattern is matched.
 *
 * @param actionType  the type of action (e.g. "EMIT_EVENT", "SET_FACT", "INVOKE_RULE")
 * @param parameters  action-specific parameters
 */
public record CepAction(
        String actionType,
        Map<String, Object> parameters
) {
    public CepAction {
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("Action type must not be blank");
        }
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }
}
