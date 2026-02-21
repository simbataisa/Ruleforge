package io.ruleforge.dmn;

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
import io.ruleforge.dmn.compiler.CompiledDmn;
import io.ruleforge.dmn.compiler.DmnCompiler;
import io.ruleforge.dmn.evaluator.DmnEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DMN 1.4 rule engine with S-FEEL expression support.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for {@link RuleType#DMN_RULE}.
 * Uses a hand-written S-FEEL parser (no external dependencies) and supports
 * all 7 DMN hit policies.
 */
public final class DmnEngine implements RuleTypeEngine {

    private final DmnCompiler compiler = new DmnCompiler();

    @Override
    public RuleType supportedType() {
        return RuleType.DMN_RULE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        if (definition.type() != RuleType.DMN_RULE) {
            throw new RuleCompilationException(definition.id(),
                    "Expected DMN_RULE but got " + definition.type());
        }
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledDmn dmn)) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Expected CompiledDmn but got " + rule.getClass().getSimpleName());
        }
        DmnEvaluator evaluator = new DmnEvaluator();
        return evaluator.evaluate(dmn, workingMemory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (definition.type() != RuleType.DMN_RULE) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected DMN_RULE but got " + definition.type()));
            return new ValidationResult(false, errors, warnings);
        }

        Map<String, Object> def = definition.definition();
        if (def == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Definition payload is null"));
            return new ValidationResult(false, errors, warnings);
        }

        // Validate decisions exist
        Object decisions = def.get("decisions");
        if (decisions == null) {
            errors.add(new ValidationError("definition.decisions", "MISSING_DECISIONS",
                    "DMN model must have a 'decisions' list"));
        } else if (!(decisions instanceof List<?>)) {
            errors.add(new ValidationError("definition.decisions", "INVALID_DECISIONS",
                    "'decisions' must be a list"));
        } else {
            List<?> decisionList = (List<?>) decisions;
            if (decisionList.isEmpty()) {
                errors.add(new ValidationError("definition.decisions", "EMPTY_DECISIONS",
                        "DMN model must have at least one decision"));
            }
            for (int i = 0; i < decisionList.size(); i++) {
                validateDecision(i, decisionList.get(i), errors, warnings);
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @SuppressWarnings("unchecked")
    private void validateDecision(int index, Object decisionRaw,
                                   List<ValidationError> errors, List<ValidationWarning> warnings) {
        if (!(decisionRaw instanceof Map<?, ?>)) {
            errors.add(new ValidationError("definition.decisions[" + index + "]",
                    "INVALID_DECISION", "Decision must be a map"));
            return;
        }
        Map<String, Object> decision = (Map<String, Object>) decisionRaw;

        if (decision.get("id") == null && decision.get("name") == null) {
            warnings.add(new ValidationWarning("definition.decisions[" + index + "]",
                    "MISSING_ID", "Decision should have an 'id' or 'name'"));
        }

        Object table = decision.get("decisionTable");
        if (table == null) {
            errors.add(new ValidationError("definition.decisions[" + index + "].decisionTable",
                    "MISSING_TABLE", "Decision must have a 'decisionTable'"));
        } else if (table instanceof Map<?, ?> tableMap) {
            Map<String, Object> tbl = (Map<String, Object>) tableMap;
            if (tbl.get("inputClauses") == null) {
                errors.add(new ValidationError(
                        "definition.decisions[" + index + "].decisionTable.inputClauses",
                        "MISSING_INPUT_CLAUSES", "Decision table must have 'inputClauses'"));
            }
            if (tbl.get("outputClauses") == null) {
                errors.add(new ValidationError(
                        "definition.decisions[" + index + "].decisionTable.outputClauses",
                        "MISSING_OUTPUT_CLAUSES", "Decision table must have 'outputClauses'"));
            }
        }
    }
}
