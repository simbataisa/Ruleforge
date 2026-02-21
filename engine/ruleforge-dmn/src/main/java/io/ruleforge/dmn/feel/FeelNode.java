package io.ruleforge.dmn.feel;

import java.util.List;
import java.util.Map;

/**
 * AST nodes produced by the S-FEEL parser.
 */
public sealed interface FeelNode {

    record NumberNode(double value) implements FeelNode {}

    record StringNode(String value) implements FeelNode {}

    record BooleanNode(boolean value) implements FeelNode {}

    record NullNode() implements FeelNode {}

    record WildcardNode() implements FeelNode {}

    record IdentifierNode(String name) implements FeelNode {}

    record BinaryNode(String operator, FeelNode left, FeelNode right) implements FeelNode {}

    record UnaryMinusNode(FeelNode operand) implements FeelNode {}

    record UnaryComparisonNode(String operator, FeelNode operand) implements FeelNode {}

    record NotNode(FeelNode operand) implements FeelNode {}

    record BetweenNode(FeelNode value, FeelNode low, FeelNode high) implements FeelNode {}

    record IfNode(FeelNode condition, FeelNode thenBranch, FeelNode elseBranch) implements FeelNode {}

    record ListNode(List<FeelNode> elements) implements FeelNode {
        public ListNode { elements = List.copyOf(elements); }
    }

    record ContextNode(Map<String, FeelNode> entries) implements FeelNode {
        public ContextNode { entries = Map.copyOf(entries); }
    }

    record RangeNode(FeelNode start, FeelNode end,
                     boolean startInclusive, boolean endInclusive) implements FeelNode {}

    record PropertyAccessNode(FeelNode target, String property) implements FeelNode {}

    record IndexAccessNode(FeelNode target, FeelNode index) implements FeelNode {}

    record FunctionCallNode(String functionName, List<FeelNode> arguments) implements FeelNode {
        public FunctionCallNode { arguments = List.copyOf(arguments); }
    }
}
