package io.ruleforge.ensemble.evaluator;

import io.ruleforge.ensemble.model.AggregationStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates numeric outputs from member rules using statistical strategies.
 *
 * <p>Supported strategies:
 * <ul>
 *   <li>{@link AggregationStrategy#AVERAGE}: arithmetic mean of the output field values.</li>
 *   <li>{@link AggregationStrategy#WEIGHTED_AVERAGE}: weight-adjusted mean.</li>
 *   <li>{@link AggregationStrategy#MAX}: maximum value across all member outputs.</li>
 *   <li>{@link AggregationStrategy#MIN}: minimum value across all member outputs.</li>
 * </ul>
 *
 * <p>Values that cannot be converted to {@link Number} are silently skipped.
 */
public final class AveragingAggregator {

    private AveragingAggregator() {}

    /**
     * Aggregates the given member results using a numeric/averaging strategy.
     *
     * @param results     ordered list of member output maps
     * @param weights     parallel list of per-member weights (same order as results)
     * @param outputField the field name to extract from each output map
     * @param strategy    the numeric aggregation strategy to apply
     * @return a map containing the aggregated numeric output under {@code outputField}
     * @throws IllegalArgumentException if the strategy is not a numeric strategy
     */
    public static Map<String, Object> aggregate(
            List<Map<String, Object>> results,
            List<Double> weights,
            String outputField,
            AggregationStrategy strategy) {

        return switch (strategy) {
            case AVERAGE          -> average(results, outputField);
            case WEIGHTED_AVERAGE -> weightedAverage(results, weights, outputField);
            case MAX              -> max(results, outputField);
            case MIN              -> min(results, outputField);
            default -> throw new IllegalArgumentException(
                    "AveragingAggregator does not handle strategy: " + strategy);
        };
    }

    private static Map<String, Object> average(
            List<Map<String, Object>> results,
            String outputField) {

        List<Double> values = extractNumericValues(results, outputField);
        double aggregated = values.isEmpty()
                ? 0.0
                : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return buildOutput(outputField, aggregated);
    }

    private static Map<String, Object> weightedAverage(
            List<Map<String, Object>> results,
            List<Double> weights,
            String outputField) {

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < results.size(); i++) {
            Object raw = results.get(i).get(outputField);
            if (raw instanceof Number num) {
                double weight = (i < weights.size()) ? weights.get(i) : 1.0;
                weightedSum += num.doubleValue() * weight;
                totalWeight += weight;
            }
        }

        double aggregated = (totalWeight == 0.0) ? 0.0 : weightedSum / totalWeight;
        return buildOutput(outputField, aggregated);
    }

    private static Map<String, Object> max(
            List<Map<String, Object>> results,
            String outputField) {

        List<Double> values = extractNumericValues(results, outputField);
        double aggregated = values.isEmpty()
                ? Double.NaN
                : values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
        return buildOutput(outputField, aggregated);
    }

    private static Map<String, Object> min(
            List<Map<String, Object>> results,
            String outputField) {

        List<Double> values = extractNumericValues(results, outputField);
        double aggregated = values.isEmpty()
                ? Double.NaN
                : values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
        return buildOutput(outputField, aggregated);
    }

    private static List<Double> extractNumericValues(
            List<Map<String, Object>> results,
            String outputField) {

        List<Double> values = new ArrayList<>();
        for (Map<String, Object> result : results) {
            Object raw = result.get(outputField);
            if (raw instanceof Number num) {
                values.add(num.doubleValue());
            }
        }
        return values;
    }

    private static Map<String, Object> buildOutput(String outputField, double value) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputField, value);
        return output;
    }
}
