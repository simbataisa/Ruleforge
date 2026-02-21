package io.ruleforge.cep;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import io.ruleforge.core.model.ValidationResult.ValidationWarning;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Engine for evaluating Complex Event Processing rules.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for {@link RuleType#CEP_RULE}.
 * Uses pattern matchers (sequence, aggregation, frequency) to detect patterns
 * in event streams and trigger actions.
 */
public final class CepEngine implements RuleTypeEngine {

    private final CepCompiler compiler = new CepCompiler();
    private final CepEvaluator evaluator = new CepEvaluator();

    @Override
    public RuleType supportedType() {
        return RuleType.CEP_RULE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        if (definition.type() != RuleType.CEP_RULE) {
            throw new RuleCompilationException(definition.id(),
                    "Expected CEP_RULE but got " + definition.type());
        }
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledCep compiled)) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Expected CompiledCep but got " + rule.getClass().getSimpleName());
        }
        return evaluator.evaluate(compiled, workingMemory, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (definition.type() != RuleType.CEP_RULE) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected CEP_RULE but got " + definition.type()));
            return new ValidationResult(false, errors, warnings);
        }

        Map<String, Object> def = definition.definition();
        if (def == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Definition payload is null"));
            return new ValidationResult(false, errors, warnings);
        }

        // Validate patternType
        Object patternTypeRaw = def.get("patternType");
        if (patternTypeRaw == null) {
            errors.add(new ValidationError("definition.patternType", "MISSING_PATTERN_TYPE",
                    "CEP definition must have a 'patternType'"));
        } else {
            try {
                PatternType.valueOf(patternTypeRaw.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationError("definition.patternType", "INVALID_PATTERN_TYPE",
                        "Unknown pattern type: " + patternTypeRaw +
                                ". Valid: SEQUENCE, AGGREGATION, FREQUENCY, ABSENCE, CONJUNCTION"));
            }
        }

        // Validate eventSchemas
        Object schemasRaw = def.get("eventSchemas");
        if (schemasRaw != null && !(schemasRaw instanceof List<?>)) {
            errors.add(new ValidationError("definition.eventSchemas", "INVALID_SCHEMAS",
                    "'eventSchemas' must be a list"));
        } else if (schemasRaw instanceof List<?> schemaList) {
            for (int i = 0; i < schemaList.size(); i++) {
                Object item = schemaList.get(i);
                if (!(item instanceof Map<?, ?>)) {
                    errors.add(new ValidationError("definition.eventSchemas[" + i + "]",
                            "INVALID_SCHEMA", "Event schema must be a map"));
                    continue;
                }
                Map<String, Object> schema = (Map<String, Object>) item;
                if (schema.get("eventType") == null || schema.get("eventType").toString().isBlank()) {
                    errors.add(new ValidationError("definition.eventSchemas[" + i + "].eventType",
                            "MISSING_EVENT_TYPE", "Event type is required"));
                }
            }
        }

        // Validate timeWindow
        Object windowRaw = def.get("timeWindow");
        if (windowRaw != null && !(windowRaw instanceof Map<?, ?>)) {
            errors.add(new ValidationError("definition.timeWindow", "INVALID_TIME_WINDOW",
                    "'timeWindow' must be a map"));
        }

        // Validate actions
        Object actionsRaw = def.get("actions");
        if (actionsRaw != null && !(actionsRaw instanceof List<?>)) {
            errors.add(new ValidationError("definition.actions", "INVALID_ACTIONS",
                    "'actions' must be a list"));
        } else if (actionsRaw == null || ((List<?>) actionsRaw).isEmpty()) {
            warnings.add(new ValidationWarning("definition.actions", "NO_ACTIONS",
                    "CEP rule has no actions defined"));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
}
