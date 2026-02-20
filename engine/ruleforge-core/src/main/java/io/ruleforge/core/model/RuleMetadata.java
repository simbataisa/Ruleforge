package io.ruleforge.core.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Metadata attached to every rule unit. Controls scheduling, grouping, and lifecycle.
 *
 * @param salience        Conflict-resolution priority (-10000 to 10000).
 * @param priority        Secondary ordering hint.
 * @param weight          Relative weight (0.0 to 1.0).
 * @param activationGroup Only one rule in the group may fire per evaluation.
 * @param agendaGroup     Logical grouping on the agenda.
 * @param ruleflowGroup   Links the rule to a ruleflow node.
 * @param ttl             Time-to-live for cached evaluation results.
 * @param noLoop          If true, prevents the rule from re-activating itself.
 * @param lockOnActive    If true, prevents rules in the same agenda group from re-firing.
 * @param tags            Arbitrary key-value tags for filtering and reporting.
 * @param effectiveDate   Rule is eligible only after this instant.
 * @param expirationDate  Rule expires after this instant.
 */
public record RuleMetadata(
        int salience,
        int priority,
        double weight,
        String activationGroup,
        String agendaGroup,
        String ruleflowGroup,
        Duration ttl,
        boolean noLoop,
        boolean lockOnActive,
        Map<String, String> tags,
        Instant effectiveDate,
        Instant expirationDate
) {

    /**
     * Returns a {@link RuleMetadata} instance with sensible defaults.
     */
    public static RuleMetadata defaults() {
        return new RuleMetadata(
                0,          // salience
                0,          // priority
                1.0,        // weight
                null,       // activationGroup
                null,       // agendaGroup
                null,       // ruleflowGroup
                null,       // ttl
                false,      // noLoop
                false,      // lockOnActive
                Map.of(),   // tags
                null,       // effectiveDate
                null        // expirationDate
        );
    }
}
