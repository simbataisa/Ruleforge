package io.ruleforge.scorecard.compiler;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.scorecard.model.ScoreRange;
import io.ruleforge.scorecard.model.ScorecardAttribute;
import io.ruleforge.scorecard.model.ScorecardCategory;
import io.ruleforge.scorecard.model.ScorecardDefinition;
import io.ruleforge.scorecard.model.ScoringStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a raw {@link RuleDefinition} payload into a type-safe
 * {@link ScorecardDefinition} and wraps it in a {@link CompiledScorecard}.
 *
 * <p>The expected {@code definition} map structure is:
 * <pre>
 * {
 *   "baseScore":    0.0,
 *   "strategy":     "ADDITIVE",          // ADDITIVE | WEIGHTED_AVERAGE | MULTIPLICATIVE
 *   "minimumScore": 0.0,
 *   "maximumScore": 1000.0,
 *   "categories": [
 *     {
 *       "name":   "Age",
 *       "weight": 0.4,
 *       "attributes": [
 *         {
 *           "name":          "age",
 *           "factPath":      "input.age",
 *           "defaultPoints": 0.0,
 *           "ranges": [
 *             { "min": 18, "max": 25, "points": 100.0, "label": "Young adult" },
 *             { "min": 25, "max": 65, "points": 200.0, "label": "Prime"       },
 *             { "min": 65, "max": null, "points": 100.0, "label": "Senior"    }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>Range bounds default to <em>inclusive lower, exclusive upper</em> unless the
 * caller explicitly provides {@code "minInclusive"} / {@code "maxInclusive"} keys.
 */
public final class ScorecardCompiler {

    /**
     * Compiles the supplied rule definition into a {@link CompiledScorecard}.
     *
     * @param definition the raw rule definition
     * @return a ready-to-evaluate compiled scorecard
     * @throws RuleCompilationException if the payload is missing or malformed
     */
    @SuppressWarnings("unchecked")
    public CompiledScorecard compile(RuleDefinition definition) {
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            throw new RuleCompilationException(
                    definition.id(),
                    "Scorecard definition payload is null");
        }

        double baseScore    = toDouble(payload.getOrDefault("baseScore",    0.0));
        double minimumScore = toDouble(payload.getOrDefault("minimumScore", 0.0));
        double maximumScore = toDouble(payload.getOrDefault("maximumScore", 1000.0));

        String strategyStr = (String) payload.getOrDefault("strategy", "ADDITIVE");
        ScoringStrategy strategy;
        try {
            strategy = ScoringStrategy.valueOf(strategyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleCompilationException(
                    definition.id(),
                    "Unknown scoring strategy: " + strategyStr);
        }

        List<Map<String, Object>> categoryDefs =
                (List<Map<String, Object>>) payload.getOrDefault("categories", List.of());

        List<ScorecardCategory> categories = parseCategories(definition.id(), categoryDefs);

        ScorecardDefinition scorecardDef = new ScorecardDefinition(
                definition.id(),
                definition.name(),
                baseScore,
                categories,
                strategy,
                minimumScore,
                maximumScore
        );

        return new CompiledScorecard(
                definition.id(),
                definition.metadata(),
                System.currentTimeMillis(),
                scorecardDef
        );
    }

    // -------------------------------------------------------------------------
    // Private parsing helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<ScorecardCategory> parseCategories(String ruleId,
                                                     List<Map<String, Object>> categoryDefs) {
        List<ScorecardCategory> categories = new ArrayList<>(categoryDefs.size());
        for (Map<String, Object> catDef : categoryDefs) {
            String name   = (String) catDef.getOrDefault("name", "");
            double weight = toDouble(catDef.getOrDefault("weight", 1.0));

            List<Map<String, Object>> attributeDefs =
                    (List<Map<String, Object>>) catDef.getOrDefault("attributes", List.of());

            List<ScorecardAttribute> attributes = parseAttributes(ruleId, name, attributeDefs);
            categories.add(new ScorecardCategory(name, weight, attributes));
        }
        return categories;
    }

    @SuppressWarnings("unchecked")
    private List<ScorecardAttribute> parseAttributes(String ruleId,
                                                      String categoryName,
                                                      List<Map<String, Object>> attrDefs) {
        List<ScorecardAttribute> attributes = new ArrayList<>(attrDefs.size());
        for (Map<String, Object> attrDef : attrDefs) {
            String name          = (String) attrDef.getOrDefault("name", "");
            String factPath      = (String) attrDef.get("factPath");
            double defaultPoints = toDouble(attrDef.getOrDefault("defaultPoints", 0.0));

            if (factPath == null || factPath.isBlank()) {
                throw new RuleCompilationException(
                        ruleId,
                        "Attribute '" + name + "' in category '" + categoryName
                                + "' is missing required field 'factPath'");
            }

            List<Map<String, Object>> rangeDefs =
                    (List<Map<String, Object>>) attrDef.getOrDefault("ranges", List.of());

            List<ScoreRange> ranges = parseRanges(rangeDefs);
            attributes.add(new ScorecardAttribute(name, factPath, ranges, defaultPoints));
        }
        return attributes;
    }

    private List<ScoreRange> parseRanges(List<Map<String, Object>> rangeDefs) {
        List<ScoreRange> ranges = new ArrayList<>(rangeDefs.size());
        for (Map<String, Object> rangeDef : rangeDefs) {
            Object minObj = rangeDef.get("min");
            Object maxObj = rangeDef.get("max");

            Double min = (minObj != null) ? toDouble(minObj) : null;
            Double max = (maxObj != null) ? toDouble(maxObj) : null;

            // Default: inclusive lower bound, exclusive upper bound (standard numeric range convention)
            boolean minInclusive = toBooleanOrDefault(rangeDef.get("minInclusive"), true);
            boolean maxInclusive = toBooleanOrDefault(rangeDef.get("maxInclusive"), false);

            double points = toDouble(rangeDef.getOrDefault("points", 0.0));
            String label  = (String) rangeDef.getOrDefault("label", "");

            ranges.add(new ScoreRange(min, max, minInclusive, maxInclusive, points, label));
        }
        return ranges;
    }

    // -------------------------------------------------------------------------
    // Type coercion utilities
    // -------------------------------------------------------------------------

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return Double.parseDouble(s);
        }
        throw new IllegalArgumentException("Cannot convert to double: " + value);
    }

    private static boolean toBooleanOrDefault(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }
}
