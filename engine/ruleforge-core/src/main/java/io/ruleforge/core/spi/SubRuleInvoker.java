package io.ruleforge.core.spi;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.RuleResult;

/**
 * SPI for invoking sub-rules by ID during evaluation.
 *
 * <p>This allows ensemble engines, flow executors, and other composite rule
 * types to delegate evaluation to member/child rules without depending on
 * a concrete rule store or engine implementation.
 */
@FunctionalInterface
public interface SubRuleInvoker {

    /**
     * Invokes a rule by its identifier against the given working memory.
     *
     * @param ruleId        the unique identifier of the rule to invoke
     * @param workingMemory the working memory context for evaluation
     * @return the evaluation result
     */
    RuleResult invoke(String ruleId, WorkingMemory workingMemory);
}
