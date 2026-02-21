package io.ruleforge.scorecard;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.scorecard.compiler.CompiledScorecard;
import io.ruleforge.scorecard.compiler.ScorecardCompiler;
import io.ruleforge.scorecard.model.ScorecardAttribute;
import io.ruleforge.scorecard.model.ScorecardCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Engine for evaluating scorecard rules with weighted scoring categories.
 *
 * <p>Implements the {@link RuleTypeEngine} SPI for {@link RuleType#SCORECARD}.
 * Compilation is delegated to {@link ScorecardCompiler}; evaluation delegates to
 * {@link io.ruleforge.scorecard.evaluator.ScorecardEvaluator} via the compiled rule.
 */
public final class ScorecardEngine implements RuleTypeEngine {

    private final ScorecardCompiler compiler = new ScorecardCompiler();

    // -------------------------------------------------------------------------
    // RuleTypeEngine SPI
    // -------------------------------------------------------------------------

    @Override
    public RuleType supportedType() {
        return RuleType.SCORECARD;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledScorecard scorecardRule)) {
            throw new RuleEvaluationException(
                    rule.ruleId(),
                    "Expected CompiledScorecard but got " + rule.getClass().getSimpleName());
        }
        return scorecardRule.evaluate(workingMemory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();

        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            errors.add(new ValidationError(
                    "definition",
                    "MISSING_DEFINITION",
                    "Scorecard definition payload must not be null"));
            return ValidationResult.failure(errors);
        }

        List<Map<String, Object>> categoryDefs =
                (List<Map<String, Object>>) payload.get("categories");

        if (categoryDefs == null || categoryDefs.isEmpty()) {
            errors.add(new ValidationError(
                    "definition.categories",
                    "MISSING_CATEGORIES",
                    "Scorecard must define at least one category"));
        } else {
            for (int ci = 0; ci < categoryDefs.size(); ci++) {
                Map<String, Object> catDef = categoryDefs.get(ci);
                String catPath = "definition.categories[" + ci + "]";

                List<Map<String, Object>> attrDefs =
                        (List<Map<String, Object>>) catDef.get("attributes");

                if (attrDefs == null || attrDefs.isEmpty()) {
                    errors.add(new ValidationError(
                            catPath + ".attributes",
                            "MISSING_ATTRIBUTES",
                            "Category '" + catDef.getOrDefault("name", ci) + "' must have at least one attribute"));
                    continue;
                }

                for (int ai = 0; ai < attrDefs.size(); ai++) {
                    Map<String, Object> attrDef = attrDefs.get(ai);
                    String attrPath = catPath + ".attributes[" + ai + "]";
                    Object factPath = attrDef.get("factPath");

                    if (factPath == null || factPath.toString().isBlank()) {
                        errors.add(new ValidationError(
                                attrPath + ".factPath",
                                "MISSING_FACT_PATH",
                                "Attribute '" + attrDef.getOrDefault("name", ai) + "' is missing required field 'factPath'"));
                    }
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
