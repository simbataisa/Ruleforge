package io.ruleforge.core.expression;

import java.util.List;

/**
 * Sealed AST hierarchy for all RFL (RuleForge Language) expression nodes.
 *
 * <p>Every concrete node is an immutable {@code record}, making the tree
 * safe to share across threads and trivially serializable. The sealed
 * hierarchy enables exhaustive pattern matching in evaluators and visitors.
 */
public sealed interface Expression permits
        Expression.Literal, Expression.Identifier, Expression.UnaryOp,
        Expression.BinaryOp, Expression.Comparison, Expression.LogicalOp,
        Expression.PropertyAccess, Expression.NullSafeAccess,
        Expression.IndexAccess, Expression.FunctionCall,
        Expression.BetweenExpr, Expression.InExpr,
        Expression.NullCoalesce, Expression.Lambda,
        Expression.ArrayLiteral {

    /** Literal values: numbers, strings, booleans, null. */
    record Literal(Object value) implements Expression {}

    /** Variable / fact path reference. */
    record Identifier(String name) implements Expression {}

    /** Unary operators: NOT, negation (-). */
    record UnaryOp(String operator, Expression operand) implements Expression {}

    /** Binary arithmetic: +, -, *, /, %. */
    record BinaryOp(String operator, Expression left, Expression right) implements Expression {}

    /** Comparison: ==, !=, >, >=, <, <=, MATCHES, STARTS_WITH, ENDS_WITH, CONTAINS. */
    record Comparison(String operator, Expression left, Expression right) implements Expression {}

    /** Logical connectives: AND, OR. */
    record LogicalOp(String operator, Expression left, Expression right) implements Expression {}

    /** Dot property access: expr.field. */
    record PropertyAccess(Expression object, String property) implements Expression {}

    /** Null-safe property access: expr?.field. */
    record NullSafeAccess(Expression object, String property) implements Expression {}

    /** Index/bracket access: expr[index]. */
    record IndexAccess(Expression object, Expression index) implements Expression {}

    /** Function call: NAME(args...). */
    record FunctionCall(String name, List<Expression> arguments) implements Expression {}

    /** BETWEEN expression: value BETWEEN low AND high. */
    record BetweenExpr(Expression value, Expression low, Expression high) implements Expression {}

    /** IN / NOT_IN expression: value IN [a, b, c]. */
    record InExpr(Expression value, List<Expression> elements, boolean negated) implements Expression {}

    /** Null coalescing: value ?? default. */
    record NullCoalesce(Expression value, Expression defaultValue) implements Expression {}

    /** Lambda: param -> body. */
    record Lambda(String parameter, Expression body) implements Expression {}

    /** Array literal: [expr, expr, ...]. */
    record ArrayLiteral(List<Expression> elements) implements Expression {}
}
