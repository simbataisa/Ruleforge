package io.ruleforge.dmn.model;

/**
 * DMN 1.4 hit policies for decision tables.
 */
public enum HitPolicy {
    UNIQUE,
    FIRST,
    PRIORITY,
    ANY,
    COLLECT,
    RULE_ORDER,
    OUTPUT_ORDER
}
