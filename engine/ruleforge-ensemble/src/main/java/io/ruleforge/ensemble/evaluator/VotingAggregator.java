package io.ruleforge.ensemble.evaluator;

import io.ruleforge.ensemble.model.AggregationStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates a list of member rule output maps using voting-based strategies.
 *
 * <p>Supported strategies:
 * <ul>
 *   <li>{@link AggregationStrategy#MAJORITY_VOTE}: each member casts one equal vote;
 *       the value with the highest vote count wins.</li>
 *   <li>{@link AggregationStrategy#WEIGHTED_VOTE}: each member's vote is multiplied by
 *       its weight; the value with the highest weighted score wins.</li>
 *   <li>{@link AggregationStrategy#FIRST_MATCH}: returns the output of the first member
 *       that produced a non-null value for the output field.</li>
 * </ul>
 */
public final class VotingAggregator {

    private VotingAggregator() {}

    /**
     * Aggregates the given member results using a voting strategy.
     *
     * @param results     ordered list of member output maps
     * @param weights     parallel list of per-member weights (same order as results)
     * @param outputField the field name to extract and aggregate from each output map
     * @param strategy    the voting strategy to apply
     * @return a map containing the aggregated output under the given {@code outputField}
     * @throws IllegalArgumentException if the strategy is not a voting strategy
     */
    public static Map<String, Object> aggregate(
            List<Map<String, Object>> results,
            List<Double> weights,
            String outputField,
            AggregationStrategy strategy) {

        return switch (strategy) {
            case MAJORITY_VOTE -> majorityVote(results, outputField);
            case WEIGHTED_VOTE -> weightedVote(results, weights, outputField);
            case FIRST_MATCH   -> firstMatch(results, outputField);
            default -> throw new IllegalArgumentException(
                    "VotingAggregator does not handle strategy: " + strategy);
        };
    }

    private static Map<String, Object> majorityVote(
            List<Map<String, Object>> results,
            String outputField) {

        Map<Object, Integer> voteCounts = new LinkedHashMap<>();
        for (Map<String, Object> result : results) {
            Object value = result.get(outputField);
            if (value != null) {
                voteCounts.merge(value, 1, Integer::sum);
            }
        }

        Object winner = pickHighest(voteCounts);
        return buildOutput(outputField, winner, voteCounts);
    }

    private static Map<String, Object> weightedVote(
            List<Map<String, Object>> results,
            List<Double> weights,
            String outputField) {

        Map<Object, Double> weightedScores = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            Object value = results.get(i).get(outputField);
            if (value != null) {
                double weight = (i < weights.size()) ? weights.get(i) : 1.0;
                weightedScores.merge(value, weight, Double::sum);
            }
        }

        Object winner = pickHighestDouble(weightedScores);
        Map<Object, Integer> voteCounts = new LinkedHashMap<>();
        weightedScores.forEach((k, v) -> voteCounts.put(k, v.intValue()));
        return buildOutput(outputField, winner, voteCounts);
    }

    private static Map<String, Object> firstMatch(
            List<Map<String, Object>> results,
            String outputField) {

        for (Map<String, Object> result : results) {
            Object value = result.get(outputField);
            if (value != null) {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put(outputField, value);
                return output;
            }
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputField, null);
        return output;
    }

    private static <T extends Comparable<T>> Object pickHighest(Map<Object, Integer> scores) {
        Object best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Map.Entry<Object, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static Object pickHighestDouble(Map<Object, Double> scores) {
        Object best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Object, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static Map<String, Object> buildOutput(
            String outputField,
            Object winner,
            Map<Object, Integer> voteCounts) {

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputField, winner);
        output.put(outputField + "_voteCounts", voteCounts.toString());
        return output;
    }
}
