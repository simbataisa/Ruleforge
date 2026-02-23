package io.ruleforge.core.engine;

import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.spi.RuleTypeEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registry of type-specific rule engines (table, tree, scorecard, etc.).
 * Maps {@link RuleType} to its corresponding {@link RuleTypeEngine} implementation.
 *
 * <p>Each rule type can have at most one registered engine. Attempting to register
 * a second engine for the same type will replace the previous one.
 */
public class RuleEngineRegistry {

    private final Map<RuleType, RuleTypeEngine> engines = new HashMap<>();

    /**
     * Registers a type-specific rule engine. The engine's
     * {@link RuleTypeEngine#supportedType()} determines the key.
     *
     * @param engine the rule type engine to register
     * @throws NullPointerException if engine is null
     */
    public void register(RuleTypeEngine engine) {
        Objects.requireNonNull(engine, "engine must not be null");
        engines.put(engine.supportedType(), engine);
    }

    /**
     * Retrieves the engine registered for the given rule type.
     *
     * @param type the rule type
     * @return the corresponding engine
     * @throws IllegalArgumentException if no engine is registered for the type
     */
    public RuleTypeEngine getEngine(RuleType type) {
        RuleTypeEngine engine = engines.get(type);
        if (engine == null) {
            throw new IllegalArgumentException(
                    "No RuleTypeEngine registered for type: " + type
                            + ". Registered types: " + engines.keySet());
        }
        return engine;
    }

    /**
     * Retrieves the engine registered for the given rule type, or null if none.
     */
    public RuleTypeEngine findEngine(RuleType type) {
        return engines.get(type);
    }

    /**
     * Checks whether an engine is registered for the given rule type.
     *
     * @param type the rule type
     * @return {@code true} if an engine exists for the type
     */
    public boolean hasEngine(RuleType type) {
        return engines.containsKey(type);
    }

    /**
     * Returns an unmodifiable view of all registered rule types.
     */
    public Set<RuleType> registeredTypes() {
        return Collections.unmodifiableSet(engines.keySet());
    }
}
