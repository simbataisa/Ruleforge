package io.ruleforge.hierarchy;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A compiled hierarchy rule. Holds the hierarchy definition and the merged
 * (effective) definition for a specific position in the hierarchy.
 */
public final class CompiledHierarchy extends CompiledRule {

    private final HierarchyDefinition hierarchyDefinition;
    private final List<HierarchyLevel> resolvedLevels;
    private final Map<String, Object> effectiveDefinition;
    private final String ruleName;
    private final int ruleVersion;

    public CompiledHierarchy(String ruleId,
                              String ruleName,
                              int ruleVersion,
                              RuleMetadata metadata,
                              long compiledAt,
                              HierarchyDefinition hierarchyDefinition,
                              List<HierarchyLevel> resolvedLevels,
                              Map<String, Object> effectiveDefinition) {
        super(ruleId, RuleType.HIERARCHY, metadata, compiledAt);
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
        this.hierarchyDefinition = hierarchyDefinition;
        this.resolvedLevels = List.copyOf(resolvedLevels);
        this.effectiveDefinition = effectiveDefinition != null
                ? new LinkedHashMap<>(effectiveDefinition)
                : new LinkedHashMap<>();
    }

    public HierarchyDefinition hierarchyDefinition() {
        return hierarchyDefinition;
    }

    public List<HierarchyLevel> resolvedLevels() {
        return resolvedLevels;
    }

    public Map<String, Object> effectiveDefinition() {
        return effectiveDefinition;
    }

    public String ruleName() {
        return ruleName;
    }

    public int ruleVersion() {
        return ruleVersion;
    }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(ruleId());

        // The effective definition is the output
        Map<String, Object> output = new LinkedHashMap<>(effectiveDefinition);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();

        RuleResultMetadata resultMetadata = new RuleResultMetadata(
                elapsed, resolvedLevels.size(), 1, 0, "HIERARCHY"
        );

        return new RuleResult(output, resultMetadata, List.of(), trace);
    }
}
