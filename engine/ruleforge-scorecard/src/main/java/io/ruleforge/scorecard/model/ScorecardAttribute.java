package io.ruleforge.scorecard.model;

import java.util.List;

/**
 * A single scoreable attribute within a {@link ScorecardCategory}.
 *
 * <p>Each attribute reads a fact value from working memory via {@code factPath},
 * then walks its {@code ranges} list in order and returns the points for the first
 * matching {@link ScoreRange}.  If no range matches, {@code defaultPoints} is
 * returned instead.
 *
 * @param name          display name of this attribute
 * @param factPath      dot-separated path used to look up the fact in working memory
 * @param ranges        ordered list of scoring ranges (first match wins)
 * @param defaultPoints points to award when no range matches
 */
public record ScorecardAttribute(
        String name,
        String factPath,
        List<ScoreRange> ranges,
        double defaultPoints
) {

    /**
     * Evaluates this attribute for the given numeric {@code value}.
     *
     * <p>Iterates {@code ranges} in declaration order and returns the points of the
     * first range whose {@link ScoreRange#matches(double)} returns {@code true}.
     * Falls back to {@link #defaultPoints()} when no range matches.
     *
     * @param value the resolved numeric fact value
     * @return points awarded for this attribute
     */
    public double evaluate(double value) {
        for (ScoreRange range : ranges) {
            if (range.matches(value)) {
                return range.points();
            }
        }
        return defaultPoints;
    }
}
