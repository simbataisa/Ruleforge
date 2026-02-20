package io.ruleforge.table.evaluator;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.table.compiler.CompiledDecisionTable;

/**
 * Strategy interface for evaluating a compiled decision table under a specific hit policy.
 */
public interface TableEvaluator {

    /**
     * Evaluates the compiled decision table against the given working memory.
     *
     * @param table the compiled decision table
     * @param wm    the working memory containing input facts
     * @param ctx   the evaluation context
     * @return the rule evaluation result
     */
    RuleResult evaluate(CompiledDecisionTable table, WorkingMemory wm, EvaluationContext ctx);
}
