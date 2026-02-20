package io.ruleforge.tree.compiler;

import io.ruleforge.core.expression.Expression;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface for compiled (optimized) tree nodes. Mirrors {@link TreeNodeDefinition}
 * but with pre-parsed expressions and pre-compiled conditions ready for efficient evaluation.
 */
public sealed interface CompiledTreeNode {

    /**
     * Unique identifier for this compiled node.
     */
    String nodeId();

    /**
     * Compiled condition node with pre-resolved branches and an optional default branch.
     *
     * @param nodeId        unique node identifier
     * @param field         the working memory field path to evaluate
     * @param operator      the comparison operator string
     * @param branches      compiled branches in evaluation order
     * @param defaultBranch fallback node when no branch matches (may be null)
     */
    record CompiledConditionNode(
            String nodeId,
            String field,
            String operator,
            List<CompiledBranch> branches,
            CompiledTreeNode defaultBranch
    ) implements CompiledTreeNode {

        /**
         * A compiled branch with its comparison value(s) and child node.
         *
         * @param value       the primary comparison value
         * @param secondValue the secondary value (for BETWEEN operator)
         * @param weight      branch weight for scoring mode
         * @param child       the compiled child node
         */
        public record CompiledBranch(
                Object value,
                Object secondValue,
                double weight,
                CompiledTreeNode child
        ) {}
    }

    /**
     * Compiled action node that produces output values. Terminal node in the tree.
     *
     * @param nodeId  unique node identifier
     * @param outputs immutable map of output field names to values
     */
    record CompiledActionNode(
            String nodeId,
            Map<String, Object> outputs
    ) implements CompiledTreeNode {}

    /**
     * Compiled sub-rule reference node for deferred resolution.
     *
     * @param nodeId         unique node identifier
     * @param subRuleId      the referenced rule identifier
     * @param subRuleVersion the referenced rule version
     */
    record CompiledSubRuleNode(
            String nodeId,
            String subRuleId,
            int subRuleVersion
    ) implements CompiledTreeNode {}

    /**
     * Compiled scoring node with pre-resolved score and optional outputs.
     *
     * @param nodeId  unique node identifier
     * @param score   the numeric score to accumulate
     * @param outputs optional output field mappings
     */
    record CompiledScoreNode(
            String nodeId,
            double score,
            Map<String, Object> outputs
    ) implements CompiledTreeNode {}

    /**
     * Compiled gate node with a pre-parsed RFL expression.
     *
     * @param nodeId             unique node identifier
     * @param compiledExpression the pre-parsed RFL expression AST
     * @param trueChild          child node for true evaluation
     * @param falseChild         child node for false evaluation
     */
    record CompiledGateNode(
            String nodeId,
            Expression compiledExpression,
            CompiledTreeNode trueChild,
            CompiledTreeNode falseChild
    ) implements CompiledTreeNode {}
}
