package io.ruleforge.dmn.model;

import java.util.List;

/**
 * A single decision element in a DMN definition.
 *
 * @param id              unique identifier
 * @param name            human-readable name
 * @param decisionTable   the decision table (if this decision uses a table)
 * @param requiredInputs  IDs of required input data elements
 * @param requiredDecisions IDs of required upstream decisions
 */
public record DmnDecision(
        String id,
        String name,
        DmnDecisionTable decisionTable,
        List<String> requiredInputs,
        List<String> requiredDecisions
) {
    public DmnDecision {
        requiredInputs = requiredInputs == null ? List.of() : List.copyOf(requiredInputs);
        requiredDecisions = requiredDecisions == null ? List.of() : List.copyOf(requiredDecisions);
    }
}
