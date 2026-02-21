package io.ruleforge.ensemble.evaluator;

import io.ruleforge.ensemble.model.AggregationStrategy;

import java.util.List;
import java.util.Map;

/**
 * Facade for aggregating pre-fetched member rule results into a single output.
 *
 * <p>Since an ensemble needs to invoke other rules, the actual member execution
 * happens at the engine level (in {@link io.ruleforge.ensemble.EnsembleEngine}),
 * which has access to the sub-rule registry via the
 * {@link io.ruleforge.core.spi.EvaluationContext}. This class receives the
 * already-evaluated member outputs and delegates to the appropriate aggregator.
 *
 * <p>Voting strategies (MAJORITY_VOTE, WEIGHTED_VOTE, FIRST_MATCH) are handled
 * by {@link VotingAggregator}. Numeric strategies (AVERAGE, WEIGHTED_AVERAGE,
 * MAX, MIN) are handled by {@link AveragingAggregator}.
 */
public final class EnsembleEvaluator {

    private EnsembleEvaluator() {}

    /**
     * Aggregates a list of member rule outputs into a single result map.
     *
     * @param memberResults list of output maps produced by member rule evaluations
     * @param weights       parallel list of per-member weights (same order as memberResults)
     * @param outputField   the field name to extract and aggregate from each member output
     * @param strategy      the aggregation strategy to apply
     * @return an aggregated output map
     */
    public static Map<String, Object> aggregate(
            List<Map<String, Object>> memberResults,
            List<Double> weights,
            String outputField,
            AggregationStrategy strategy) {

        return switch (strategy) {
            case MAJORITY_VOTE, WEIGHTED_VOTE, FIRST_MATCH ->
                    VotingAggregator.aggregate(memberResults, weights, outputField, strategy);
            case AVERAGE, WEIGHTED_AVERAGE, MAX, MIN ->
                    AveragingAggregator.aggregate(memberResults, weights, outputField, strategy);
        };
    }
}
