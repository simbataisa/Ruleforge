package io.ruleforge.scorecard.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.scorecard.evaluator.ScorecardEvaluator;
import io.ruleforge.scorecard.model.ScorecardDefinition;

/**
 * Compiled representation of a scorecard rule.
 *
 * <p>Stores the pre-parsed {@link ScorecardDefinition} produced by
 * {@link ScorecardCompiler} and delegates evaluation to
 * {@link ScorecardEvaluator} on every call to {@link #evaluate(WorkingMemory)}.
 */
public final class CompiledScorecard extends CompiledRule {

    private final ScorecardDefinition scorecardDefinition;

    public CompiledScorecard(String ruleId,
                              RuleMetadata metadata,
                              long compiledAt,
                              ScorecardDefinition scorecardDefinition) {
        super(ruleId, RuleType.SCORECARD, metadata, compiledAt);
        this.scorecardDefinition = scorecardDefinition;
    }

    /**
     * Returns the parsed scorecard definition.
     */
    public ScorecardDefinition scorecardDefinition() {
        return scorecardDefinition;
    }

    /**
     * Evaluates this scorecard against the supplied working memory.
     * Delegates all logic to {@link ScorecardEvaluator}.
     *
     * @param workingMemory the current fact store for this execution
     * @return the evaluation result containing the computed score and per-category breakdown
     */
    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        return ScorecardEvaluator.evaluate(scorecardDefinition, workingMemory);
    }
}
