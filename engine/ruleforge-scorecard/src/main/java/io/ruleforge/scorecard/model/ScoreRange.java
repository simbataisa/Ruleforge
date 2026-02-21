package io.ruleforge.scorecard.model;

/**
 * Represents a scoring range that maps a numeric value to a point award.
 *
 * <p>A range is defined by optional {@code min} and {@code max} bounds, together
 * with inclusivity flags. When {@code min == max} (and both are non-null) the
 * range represents an exact-value match.  A {@code null} bound means "unbounded"
 * in that direction (e.g. {@code max == null} means "greater than or equal to min").
 *
 * @param min          lower bound of the range, or {@code null} if unbounded below
 * @param max          upper bound of the range, or {@code null} if unbounded above
 * @param minInclusive whether the lower bound is inclusive ({@code >=} vs {@code >})
 * @param maxInclusive whether the upper bound is inclusive ({@code <=} vs {@code <})
 * @param points       points awarded when the tested value falls inside this range
 * @param label        human-readable label for this range (used in trace output)
 */
public record ScoreRange(
        Double min,
        Double max,
        boolean minInclusive,
        boolean maxInclusive,
        double points,
        String label
) {

    /**
     * Tests whether the given numeric {@code value} falls within this range.
     *
     * <p>Exact-value ranges ({@code min == max}) are always treated as inclusive on
     * both sides; the individual {@code minInclusive}/{@code maxInclusive} flags are
     * ignored for this case to avoid accidental mismatches.
     *
     * @param value the value to test
     * @return {@code true} if the value is covered by this range
     */
    public boolean matches(double value) {
        // Exact-value range
        if (min != null && max != null && Double.compare(min, max) == 0) {
            return Double.compare(value, min) == 0;
        }

        // Check lower bound
        if (min != null) {
            int cmp = Double.compare(value, min);
            if (minInclusive ? cmp < 0 : cmp <= 0) {
                return false;
            }
        }

        // Check upper bound
        if (max != null) {
            int cmp = Double.compare(value, max);
            if (maxInclusive ? cmp > 0 : cmp >= 0) {
                return false;
            }
        }

        return true;
    }
}
