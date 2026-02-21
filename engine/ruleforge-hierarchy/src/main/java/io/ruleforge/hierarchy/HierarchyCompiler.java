package io.ruleforge.hierarchy;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.model.RuleDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link RuleDefinition} containing hierarchy information into
 * a {@link CompiledHierarchy} by resolving hierarchy levels and merging definitions.
 */
public final class HierarchyCompiler {

    /**
     * Compiles a hierarchy rule definition.
     *
     * <p>Expected keys in {@link RuleDefinition#definition()}:
     * <ul>
     *   <li>{@code "hierarchyId"} - String identifier for the hierarchy</li>
     *   <li>{@code "hierarchyName"} - Human-readable name</li>
     *   <li>{@code "defaultMode"} - Default override mode (optional, defaults to REPLACE)</li>
     *   <li>{@code "levels"} - List of level maps with keys: name, depth, overrideMode, definition, contextKey</li>
     * </ul>
     *
     * @param definition the rule definition
     * @return the compiled hierarchy
     */
    @SuppressWarnings("unchecked")
    public CompiledHierarchy compile(RuleDefinition definition) {
        Map<String, Object> def = definition.definition();
        if (def == null) {
            throw new RuleCompilationException(definition.id(), "Definition payload is null");
        }

        String hierarchyId = getStringOrDefault(def, "hierarchyId", definition.id());
        String hierarchyName = getStringOrDefault(def, "hierarchyName", definition.name());
        OverrideMode defaultMode = parseOverrideMode(def.get("defaultMode"), OverrideMode.REPLACE);

        // Parse levels
        Object levelsRaw = def.get("levels");
        if (levelsRaw == null) {
            throw new RuleCompilationException(definition.id(), "Missing 'levels' in hierarchy definition");
        }
        if (!(levelsRaw instanceof List<?> levelList)) {
            throw new RuleCompilationException(definition.id(), "'levels' must be a list");
        }

        List<HierarchyLevel> levels = new ArrayList<>();
        for (int i = 0; i < levelList.size(); i++) {
            Object item = levelList.get(i);
            if (!(item instanceof Map<?, ?> levelMap)) {
                throw new RuleCompilationException(definition.id(),
                        "Level at index " + i + " must be a map");
            }
            Map<String, Object> lm = (Map<String, Object>) levelMap;

            String name = requireString(lm, "name", definition.id(), "levels[" + i + "]");
            int depth = lm.containsKey("depth")
                    ? ((Number) lm.get("depth")).intValue()
                    : i;
            OverrideMode mode = parseOverrideMode(lm.get("overrideMode"), defaultMode);
            Map<String, Object> levelDef = lm.containsKey("definition") && lm.get("definition") instanceof Map
                    ? (Map<String, Object>) lm.get("definition")
                    : null;
            String contextKey = lm.containsKey("contextKey")
                    ? lm.get("contextKey").toString()
                    : name;

            levels.add(new HierarchyLevel(name, depth, mode, levelDef, contextKey));
        }

        HierarchyDefinition hierarchyDef = new HierarchyDefinition(
                hierarchyId, hierarchyName, levels, defaultMode);

        // Resolve effective definition by walking from root to leaf
        Map<String, Object> effectiveDefinition = resolveEffectiveDefinition(levels);

        return new CompiledHierarchy(
                definition.id(),
                definition.name(),
                definition.version(),
                definition.metadata(),
                System.currentTimeMillis(),
                hierarchyDef,
                levels,
                effectiveDefinition
        );
    }

    /**
     * Walks the hierarchy from root to leaf, merging each level's definition
     * into the accumulated effective definition.
     */
    private Map<String, Object> resolveEffectiveDefinition(List<HierarchyLevel> levels) {
        Map<String, Object> effective = new LinkedHashMap<>();

        for (HierarchyLevel level : levels) {
            if (level.definition() != null) {
                effective = MergeStrategy.merge(effective, level.definition(), level.overrideMode());
            }
        }

        return effective;
    }

    private OverrideMode parseOverrideMode(Object raw, OverrideMode defaultMode) {
        if (raw == null) {
            return defaultMode;
        }
        try {
            return OverrideMode.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultMode;
        }
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private String requireString(Map<String, Object> map, String key, String ruleId, String path) {
        Object val = map.get(key);
        if (val == null) {
            throw new RuleCompilationException(ruleId,
                    "Missing required field '" + key + "' at " + path);
        }
        return val.toString();
    }
}
