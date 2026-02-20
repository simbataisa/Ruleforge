package io.ruleforge.core.resolution;

import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.model.RuleMetadata;

import java.util.Map;

/**
 * Represents a rule that has been activated (matched) by the current working memory state.
 * Contains the compiled rule plus activation-specific context.
 */
public record ActivatedRule(
    String id,
    String name,
    CompiledRule compiledRule,
    RuleMetadata metadata,
    PriorityVector priority,
    int conditionCount,
    Map<String, Object> matchedConditions
) implements Comparable<ActivatedRule> {

    @Override
    public int compareTo(ActivatedRule other) {
        return this.priority.compareTo(other.priority);
    }
}
