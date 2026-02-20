package io.ruleforge.tree.compiler;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the decision tree node AST as parsed from the JSON definition.
 * Each variant corresponds to a distinct node type in the tree structure.
 */
public sealed interface TreeNodeDefinition {

    /**
     * Unique identifier for this node within the tree.
     */
    String nodeId();

    /**
     * A condition node that branches based on a field value compared against operator/value pairs.
     *
     * @param nodeId   unique node identifier
     * @param field    input field path (dot-separated, resolved from working memory)
     * @param operator comparison operator (EQ, GT, LT, GTE, LTE, NEQ, BETWEEN, IN, NOT_IN, etc.)
     * @param branches ordered list of branches to evaluate
     */
    record ConditionNode(
            String nodeId,
            String field,
            String operator,
            List<Branch> branches
    ) implements TreeNodeDefinition {

        /**
         * A single branch within a condition node.
         *
         * @param value       the comparison value for the branch
         * @param secondValue secondary value for range operators (e.g. BETWEEN upper bound)
         * @param weight      branch weight for scoring mode accumulation
         * @param child       the child node to traverse if this branch matches
         */
        public record Branch(
                Object value,
                Object secondValue,
                double weight,
                TreeNodeDefinition child
        ) {}
    }

    /**
     * A terminal action node that produces output values.
     *
     * @param nodeId  unique node identifier
     * @param outputs map of output field names to their values
     */
    record ActionNode(
            String nodeId,
            Map<String, Object> outputs
    ) implements TreeNodeDefinition {}

    /**
     * A node that delegates evaluation to another rule by reference.
     *
     * @param nodeId         unique node identifier
     * @param subRuleId      the identifier of the referenced sub-rule
     * @param subRuleVersion the version of the referenced sub-rule
     */
    record SubRuleNode(
            String nodeId,
            String subRuleId,
            int subRuleVersion
    ) implements TreeNodeDefinition {}

    /**
     * A scoring node that accumulates a numeric score and optionally produces outputs.
     *
     * @param nodeId  unique node identifier
     * @param score   the score value to accumulate during traversal
     * @param outputs optional output field mappings alongside the score
     */
    record ScoreNode(
            String nodeId,
            double score,
            Map<String, Object> outputs
    ) implements TreeNodeDefinition {}

    /**
     * A gate node that evaluates an RFL expression and branches to true or false child.
     *
     * @param nodeId     unique node identifier
     * @param expression RFL expression string to evaluate as a boolean
     * @param trueChild  child node when expression evaluates to true
     * @param falseChild child node when expression evaluates to false
     */
    record GateNode(
            String nodeId,
            String expression,
            TreeNodeDefinition trueChild,
            TreeNodeDefinition falseChild
    ) implements TreeNodeDefinition {}
}
