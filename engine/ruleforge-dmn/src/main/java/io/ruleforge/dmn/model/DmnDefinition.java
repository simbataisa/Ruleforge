package io.ruleforge.dmn.model;

import java.util.List;

/**
 * Top-level DMN model containing decisions and input data.
 *
 * @param id         unique identifier
 * @param name       human-readable name
 * @param namespace  DMN namespace
 * @param decisions  the decisions in this DMN model
 * @param inputData  the input data elements
 */
public record DmnDefinition(
        String id,
        String name,
        String namespace,
        List<DmnDecision> decisions,
        List<DmnInputData> inputData
) {
    public DmnDefinition {
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        inputData = inputData == null ? List.of() : List.copyOf(inputData);
    }
}
