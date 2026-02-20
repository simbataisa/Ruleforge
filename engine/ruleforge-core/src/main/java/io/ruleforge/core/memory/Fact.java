package io.ruleforge.core.memory;

import io.ruleforge.core.model.FactType;

import java.time.Instant;

/**
 * An immutable fact stored in working memory.
 *
 * @param path         the dot-separated path that uniquely identifies this fact
 * @param value        the current value of the fact
 * @param type         the declared {@link FactType} of the fact
 * @param lastModified the timestamp when this fact was last created or modified
 * @param <T>          the Java type of the value
 */
public record Fact<T>(
    String path,
    T value,
    FactType type,
    Instant lastModified
) {}
