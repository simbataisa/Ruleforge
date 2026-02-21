package io.ruleforge.scorecard.model;

import java.util.List;

/**
 * A logical grouping of {@link ScorecardAttribute}s within a scorecard, carrying
 * an optional weight used by the {@link ScoringStrategy#WEIGHTED_AVERAGE} strategy.
 *
 * @param name       display name for this category
 * @param weight     relative weight (meaningful for WEIGHTED_AVERAGE; ignored for
 *                   ADDITIVE and MULTIPLICATIVE strategies)
 * @param attributes the attributes that belong to this category
 */
public record ScorecardCategory(
        String name,
        double weight,
        List<ScorecardAttribute> attributes
) {}
