package io.ruleforge.ensemble.model;

/**
 * Describes a single member rule participating in an ensemble.
 *
 * @param ruleId  The unique identifier of the member rule.
 * @param alias   A short human-readable alias used for logging and tracing.
 * @param weight  Relative weight applied in weighted voting/averaging strategies (0.0–1.0).
 * @param version The specific rule version to invoke; {@code 0} means "latest".
 */
public record MemberRule(
        String ruleId,
        String alias,
        double weight,
        int version
) {}
