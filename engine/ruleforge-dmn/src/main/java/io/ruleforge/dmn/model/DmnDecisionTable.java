package io.ruleforge.dmn.model;

import java.util.List;

/**
 * A DMN decision table with input/output columns and rules.
 *
 * @param hitPolicy     the hit policy for this decision table
 * @param inputClauses  input column definitions
 * @param outputClauses output column definitions
 * @param rules         the decision table rules (rows)
 */
public record DmnDecisionTable(
        HitPolicy hitPolicy,
        List<InputClause> inputClauses,
        List<OutputClause> outputClauses,
        List<DmnRule> rules
) {
    public DmnDecisionTable {
        inputClauses = List.copyOf(inputClauses);
        outputClauses = List.copyOf(outputClauses);
        rules = List.copyOf(rules);
    }

    /**
     * An input column in a DMN decision table.
     *
     * @param name       column name (maps to input data)
     * @param expression input expression (S-FEEL)
     */
    public record InputClause(String name, String expression) {}

    /**
     * An output column in a DMN decision table.
     *
     * @param name         column name
     * @param defaultValue default output value (S-FEEL expression), nullable
     */
    public record OutputClause(String name, String defaultValue) {}

    /**
     * A single rule (row) in the decision table.
     *
     * @param ruleNumber     row number (1-based)
     * @param inputEntries   S-FEEL expressions for each input column (empty string = wildcard)
     * @param outputEntries  S-FEEL expressions for each output column
     * @param annotation     optional annotation
     */
    public record DmnRule(
            int ruleNumber,
            List<String> inputEntries,
            List<String> outputEntries,
            String annotation
    ) {
        public DmnRule {
            inputEntries = List.copyOf(inputEntries);
            outputEntries = List.copyOf(outputEntries);
        }
    }
}
