package io.ruleforge.ensemble.model;

import java.util.List;

/**
 * Compiled, strongly-typed representation of an ensemble rule definition.
 *
 * @param ruleId      Unique identifier of the ensemble rule itself.
 * @param ruleName    Human-readable name of the ensemble rule.
 * @param members     Ordered list of member rules participating in the ensemble.
 * @param strategy    The aggregation strategy used to combine member outputs.
 * @param outputField The field name in each member's output map to aggregate
 *                    (defaults to {@code "result"}).
 */
public record EnsembleDefinition(
        String ruleId,
        String ruleName,
        List<MemberRule> members,
        AggregationStrategy strategy,
        String outputField
) {
    public EnsembleDefinition {
        members = members == null ? List.of() : List.copyOf(members);
        if (outputField == null || outputField.isBlank()) {
            outputField = "result";
        }
    }
}
