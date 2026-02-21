package io.ruleforge.hierarchy;

import io.ruleforge.core.spi.EvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves the applicable hierarchy levels based on the evaluation context.
 * Filters the full hierarchy to only include levels whose context key
 * matches a value present in the evaluation context's hierarchy context map.
 */
public final class HierarchyResolver {

    /**
     * Given a compiled hierarchy and an evaluation context, returns the subset
     * of hierarchy levels that are active for the current evaluation.
     *
     * <p>A level is active if:
     * <ul>
     *   <li>It is the root level (depth 0), OR</li>
     *   <li>Its context key has a matching value in the evaluation context's hierarchy context map</li>
     * </ul>
     *
     * @param compiled the compiled hierarchy
     * @param ctx      the evaluation context
     * @return the active levels, ordered from root to most-specific
     */
    public List<HierarchyLevel> resolveActiveLevels(CompiledHierarchy compiled, EvaluationContext ctx) {
        Map<String, String> hierarchyContext = ctx.getHierarchyContext();
        List<HierarchyLevel> activeLevels = new ArrayList<>();

        for (HierarchyLevel level : compiled.resolvedLevels()) {
            if (level.depth() == 0) {
                // Root level is always active
                activeLevels.add(level);
            } else if (hierarchyContext != null && hierarchyContext.containsKey(level.contextKey())) {
                activeLevels.add(level);
            }
        }

        return activeLevels;
    }

    /**
     * Resolves the effective definition for the active hierarchy levels by
     * merging from root to leaf.
     *
     * @param activeLevels the active levels to merge
     * @return the effective merged definition
     */
    public Map<String, Object> resolveEffectiveDefinition(List<HierarchyLevel> activeLevels) {
        Map<String, Object> effective = Map.of();

        for (HierarchyLevel level : activeLevels) {
            if (level.definition() != null) {
                effective = MergeStrategy.merge(
                        effective instanceof java.util.LinkedHashMap ? effective : new java.util.LinkedHashMap<>(effective),
                        level.definition(),
                        level.overrideMode());
            }
        }

        return effective;
    }
}
