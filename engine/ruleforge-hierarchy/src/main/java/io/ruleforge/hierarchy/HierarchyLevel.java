package io.ruleforge.hierarchy;

import java.util.Map;

/**
 * A single level in the hierarchy (e.g. "global", "region", "country", "customer").
 * Each level has a name and can carry rule definition overrides for that level.
 *
 * @param name          the level name (e.g. "global", "region")
 * @param depth         the depth in the hierarchy (0 = root)
 * @param overrideMode  how this level overrides its parent
 * @param definition    the rule definition overrides at this level (nullable)
 * @param contextKey    the key in EvaluationContext.getHierarchyContext() that identifies the value at this level
 */
public record HierarchyLevel(
        String name,
        int depth,
        OverrideMode overrideMode,
        Map<String, Object> definition,
        String contextKey
) {
    public HierarchyLevel {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Hierarchy level name must not be blank");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Hierarchy level depth must be >= 0");
        }
        if (overrideMode == null) {
            overrideMode = OverrideMode.REPLACE;
        }
    }
}
