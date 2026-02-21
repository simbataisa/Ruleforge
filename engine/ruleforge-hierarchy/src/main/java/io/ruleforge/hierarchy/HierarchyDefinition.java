package io.ruleforge.hierarchy;

import java.util.List;

/**
 * Defines a complete hierarchy structure for rule resolution.
 * Levels are ordered from root (index 0) to leaf (last index).
 *
 * @param hierarchyId   unique identifier for this hierarchy definition
 * @param name          human-readable name
 * @param levels        ordered list of hierarchy levels (root first)
 * @param defaultMode   default override mode when a level does not specify one
 */
public record HierarchyDefinition(
        String hierarchyId,
        String name,
        List<HierarchyLevel> levels,
        OverrideMode defaultMode
) {
    public HierarchyDefinition {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("Hierarchy must have at least one level");
        }
        levels = List.copyOf(levels);
        if (defaultMode == null) {
            defaultMode = OverrideMode.REPLACE;
        }
    }
}
