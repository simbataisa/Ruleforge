package io.ruleforge.hierarchy;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import io.ruleforge.core.model.ValidationResult.ValidationWarning;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine for evaluating hierarchical rules.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for {@link RuleType#HIERARCHY}.
 * Resolves effective rule definitions by walking the
 * hierarchy from root to leaf, merging definitions according to each level's
 * {@link OverrideMode}.
 */
public final class HierarchyEngine implements RuleTypeEngine {

    private final HierarchyCompiler compiler = new HierarchyCompiler();
    private final HierarchyResolver resolver = new HierarchyResolver();

    @Override
    public RuleType supportedType() {
        return RuleType.HIERARCHY;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        if (definition.type() != RuleType.HIERARCHY) {
            throw new RuleCompilationException(definition.id(),
                    "Expected HIERARCHY but got " + definition.type());
        }
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledHierarchy hierarchy)) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Expected CompiledHierarchy but got " + rule.getClass().getSimpleName());
        }

        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(rule.ruleId());

        // Resolve active levels based on evaluation context
        List<HierarchyLevel> activeLevels = resolver.resolveActiveLevels(hierarchy, ctx);

        // Merge effective definition from active levels
        Map<String, Object> effectiveDefinition = resolver.resolveEffectiveDefinition(activeLevels);

        // The effective definition is the output
        Map<String, Object> output = new LinkedHashMap<>(effectiveDefinition);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();

        RuleResultMetadata resultMetadata = new RuleResultMetadata(
                elapsed, activeLevels.size(), 1, 0, "HIERARCHY"
        );

        return new RuleResult(output, resultMetadata, List.of(), trace);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (definition.type() != RuleType.HIERARCHY) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected HIERARCHY but got " + definition.type()));
            return new ValidationResult(false, errors, warnings);
        }

        Map<String, Object> def = definition.definition();
        if (def == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Definition payload is null"));
            return new ValidationResult(false, errors, warnings);
        }

        // Validate levels
        Object levelsRaw = def.get("levels");
        if (levelsRaw == null) {
            errors.add(new ValidationError("definition.levels", "MISSING_LEVELS",
                    "Hierarchy definition must have a 'levels' list"));
        } else if (!(levelsRaw instanceof List<?> levelList)) {
            errors.add(new ValidationError("definition.levels", "INVALID_LEVELS",
                    "'levels' must be a list"));
        } else {
            if (levelList.isEmpty()) {
                errors.add(new ValidationError("definition.levels", "EMPTY_LEVELS",
                        "Hierarchy must have at least one level"));
            }
            for (int i = 0; i < levelList.size(); i++) {
                Object item = levelList.get(i);
                if (!(item instanceof Map<?, ?>)) {
                    errors.add(new ValidationError("definition.levels[" + i + "]",
                            "INVALID_LEVEL", "Level must be a map"));
                    continue;
                }
                Map<String, Object> level = (Map<String, Object>) item;
                if (level.get("name") == null || level.get("name").toString().isBlank()) {
                    errors.add(new ValidationError("definition.levels[" + i + "].name",
                            "MISSING_NAME", "Level name is required"));
                }
            }
        }

        // Validate defaultMode if present
        Object defaultModeRaw = def.get("defaultMode");
        if (defaultModeRaw != null) {
            try {
                OverrideMode.valueOf(defaultModeRaw.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                warnings.add(new ValidationWarning("definition.defaultMode", "INVALID_MODE",
                        "Unknown override mode: " + defaultModeRaw +
                                ". Valid values: REPLACE, MERGE, PREPEND, APPEND, OVERRIDE_ROWS"));
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
}
