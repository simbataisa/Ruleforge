package io.ruleforge.core.memory;

import io.ruleforge.core.model.FactType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The central fact store for a single rule-engine execution.
 *
 * <p>Working memory holds all facts as {@link Fact} records keyed by a dot-path,
 * records every mutation in a {@link FactChange} changelog for explainability,
 * and notifies registered {@link FactChangeListener}s on every change.
 *
 * <p>This class is <b>not</b> thread-safe; it is designed to be used within a
 * single engine execution thread. The listener set uses {@link CopyOnWriteArraySet}
 * to allow safe iteration while listeners are added or removed during notification.
 */
public class WorkingMemory {

    private final Map<String, Fact<?>> facts = new LinkedHashMap<>();
    private final List<FactChange> changelog = new ArrayList<>();
    private final Set<FactChangeListener> listeners = new CopyOnWriteArraySet<>();
    private boolean modified = false;

    // =================================================================
    // Fact operations
    // =================================================================

    /**
     * Asserts (adds) a new fact into working memory.
     *
     * @param path  the unique dot-path key for the fact
     * @param value the fact value
     * @param type  the declared {@link FactType}
     * @param <T>   the Java type of the value
     */
    public <T> void assertFact(String path, T value, FactType type) {
        Fact<T> fact = new Fact<>(path, value, type, Instant.now());
        facts.put(path, fact);
        changelog.add(new FactChange(ChangeType.ASSERT, path, null, value, Instant.now()));
        modified = true;
        notifyListeners(fact, ChangeType.ASSERT);
    }

    /**
     * Modifies an existing fact, preserving its declared type.
     *
     * @param path     the fact path (must already exist)
     * @param newValue the new value
     * @param <T>      the Java type of the new value
     * @throws IllegalArgumentException if no fact exists at the given path
     */
    public <T> void modifyFact(String path, T newValue) {
        Fact<?> existing = facts.get(path);
        if (existing == null) {
            throw new IllegalArgumentException("Fact not found: " + path);
        }
        Object oldValue = existing.value();
        FactType type = existing.type();
        Fact<T> updated = new Fact<>(path, newValue, type, Instant.now());
        facts.put(path, updated);
        changelog.add(new FactChange(ChangeType.MODIFY, path, oldValue, newValue, Instant.now()));
        modified = true;
        notifyListeners(updated, ChangeType.MODIFY);
    }

    /**
     * Retracts (removes) a fact from working memory. If the path does not exist
     * this method is a no-op.
     *
     * @param path the fact path to remove
     */
    public void retractFact(String path) {
        Fact<?> removed = facts.remove(path);
        if (removed != null) {
            changelog.add(new FactChange(ChangeType.RETRACT, path, removed.value(), null, Instant.now()));
            modified = true;
            notifyListeners(removed, ChangeType.RETRACT);
        }
    }

    // =================================================================
    // Fact access
    // =================================================================

    /**
     * Resolves a dot-path (e.g. {@code "customer.address.city"}) and returns
     * the value found at that path. Supports nested Map traversal and array
     * index notation.
     *
     * @param dotPath the dot-separated path
     * @param <T>     expected return type (caller is responsible for type safety)
     * @return the resolved value, or {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String dotPath) {
        return (T) PathResolver.resolve(facts, dotPath);
    }

    /**
     * Returns the {@link Fact} record for the given top-level path, or {@code null}.
     */
    public Fact<?> getFact(String path) {
        return facts.get(path);
    }

    /**
     * Checks whether a top-level fact exists at the given path.
     */
    public boolean hasFact(String path) {
        return facts.containsKey(path);
    }

    /**
     * Returns an unmodifiable view of all top-level fact paths.
     */
    public Set<String> getFactPaths() {
        return Collections.unmodifiableSet(facts.keySet());
    }

    // =================================================================
    // Snapshot and hashing (for cycle detection in forward chaining)
    // =================================================================

    /**
     * Computes a hash over all current facts for quick equality / cycle detection.
     */
    public long factHash() {
        return facts.values().stream()
            .mapToLong(f -> Objects.hash(f.path(), f.value()))
            .reduce(0L, (a, b) -> a * 31 + b);
    }

    /**
     * Creates a shallow snapshot of this working memory. The snapshot contains the
     * same {@link Fact} records (which are immutable) but has an independent fact
     * map and an empty changelog/listener set.
     */
    public WorkingMemory snapshot() {
        WorkingMemory copy = new WorkingMemory();
        for (Map.Entry<String, Fact<?>> entry : facts.entrySet()) {
            copy.facts.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    // =================================================================
    // Modification tracking
    // =================================================================

    /**
     * Returns {@code true} if any fact has been asserted, modified, or retracted
     * since the last call to {@link #clearModificationFlag()}.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Resets the modification flag. Typically called at the start of each
     * forward-chaining iteration to detect whether any rule fired.
     */
    public void clearModificationFlag() {
        modified = false;
    }

    // =================================================================
    // Changelog (explainability)
    // =================================================================

    /**
     * Returns an unmodifiable view of the full changelog.
     */
    public List<FactChange> getChangelog() {
        return Collections.unmodifiableList(changelog);
    }

    // =================================================================
    // Listener management
    // =================================================================

    /**
     * Registers a listener that will be notified on every fact change.
     */
    public void addListener(FactChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     */
    public void removeListener(FactChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Fact<?> fact, ChangeType changeType) {
        for (FactChangeListener listener : listeners) {
            listener.onFactChanged(fact, changeType);
        }
    }

    // =================================================================
    // Bulk operations
    // =================================================================

    /**
     * Convenience method to bulk-load facts from a plain {@code Map<String, Object>}.
     * The {@link FactType} for each entry is inferred from its Java runtime type.
     *
     * @param input a map of path to value
     */
    public void loadFromMap(Map<String, Object> input) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            FactType type = inferType(entry.getValue());
            assertFact(entry.getKey(), entry.getValue(), type);
        }
    }

    /**
     * Exports all current facts as a plain {@code Map<String, Object>}, preserving
     * insertion order.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Fact<?>> entry : facts.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }
        return result;
    }

    // =================================================================
    // Internal helpers
    // =================================================================

    /**
     * Infers a {@link FactType} from the runtime type of the given Java object.
     */
    private FactType inferType(Object value) {
        if (value == null)                     return FactType.ANY;
        if (value instanceof String)           return FactType.STRING;
        if (value instanceof Double
            || value instanceof Float)         return FactType.NUMBER;
        if (value instanceof Long
            || value instanceof Integer)       return FactType.INTEGER;
        if (value instanceof Boolean)          return FactType.BOOLEAN;
        if (value instanceof LocalDate)        return FactType.DATE;
        if (value instanceof Instant)          return FactType.DATETIME;
        if (value instanceof BigDecimal)       return FactType.MONEY;
        if (value instanceof List)             return FactType.ARRAY;
        if (value instanceof Map)              return FactType.OBJECT;
        return FactType.ANY;
    }
}
