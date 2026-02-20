package io.ruleforge.core.memory;

import java.time.Instant;

/**
 * An immutable record of a single change applied to working memory.
 * These entries form the changelog used for explainability and audit.
 *
 * @param type      the kind of change (ASSERT, MODIFY, RETRACT)
 * @param path      the fact path that was affected
 * @param oldValue  the previous value ({@code null} for ASSERT)
 * @param newValue  the new value ({@code null} for RETRACT)
 * @param timestamp when the change occurred
 */
public record FactChange(
    ChangeType type,
    String path,
    Object oldValue,
    Object newValue,
    Instant timestamp
) {}
