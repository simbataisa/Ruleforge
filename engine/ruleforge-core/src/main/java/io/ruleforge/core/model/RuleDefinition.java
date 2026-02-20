package io.ruleforge.core.model;

import java.util.Map;

/**
 * Raw rule definition as deserialized from JSON/YAML.
 * <p>
 * The {@code definition} map holds type-specific payload (e.g., table rows,
 * tree nodes, script body) that a {@link RuleUnit} factory will interpret.
 *
 * @param id           Unique identifier (UUID string).
 * @param name         Human-readable rule name.
 * @param alias        Short alias for cross-referencing.
 * @param type         The rule type discriminator.
 * @param version      Version number.
 * @param metadata     Scheduling and conflict-resolution metadata.
 * @param inputSchema  Input contract.
 * @param outputSchema Output contract.
 * @param definition   Type-specific definition payload (table rows, tree nodes, etc.).
 */
public record RuleDefinition(
        String id,
        String name,
        String alias,
        RuleType type,
        int version,
        RuleMetadata metadata,
        InputSchema inputSchema,
        OutputSchema outputSchema,
        Map<String, Object> definition
) {}
