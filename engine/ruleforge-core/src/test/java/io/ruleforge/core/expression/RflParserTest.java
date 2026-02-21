package io.ruleforge.core.expression;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RflParserTest {

    private final RflParser parser = new RflParser();

    @Test
    void parsesIntegerLiteral() {
        Expression expr = parser.parse("42");
        assertThat(expr).isInstanceOf(Expression.Literal.class);
        assertThat(((Expression.Literal) expr).value()).isEqualTo(42);
    }

    @Test
    void parsesDoubleLiteral() {
        Expression expr = parser.parse("3.14");
        assertThat(expr).isInstanceOf(Expression.Literal.class);
        assertThat(((Expression.Literal) expr).value()).isEqualTo(3.14);
    }

    @Test
    void parsesStringLiteral() {
        Expression expr = parser.parse("\"hello\"");
        assertThat(expr).isInstanceOf(Expression.Literal.class);
        assertThat(((Expression.Literal) expr).value()).isEqualTo("hello");
    }

    @Test
    void parsesBooleanLiteral() {
        Expression trueExpr = parser.parse("true");
        assertThat(((Expression.Literal) trueExpr).value()).isEqualTo(true);

        Expression falseExpr = parser.parse("false");
        assertThat(((Expression.Literal) falseExpr).value()).isEqualTo(false);
    }

    @Test
    void parsesNullLiteral() {
        Expression expr = parser.parse("NULL");
        assertThat(((Expression.Literal) expr).value()).isNull();
    }

    @Test
    void parsesIdentifier() {
        Expression expr = parser.parse("age");
        assertThat(expr).isInstanceOf(Expression.Identifier.class);
        assertThat(((Expression.Identifier) expr).name()).isEqualTo("age");
    }

    @Test
    void parsesArithmeticExpression() {
        Expression expr = parser.parse("2 + 3");
        assertThat(expr).isInstanceOf(Expression.BinaryOp.class);
        Expression.BinaryOp binOp = (Expression.BinaryOp) expr;
        assertThat(binOp.operator()).isEqualTo("+");
    }

    @Test
    void respectsOperatorPrecedence() {
        // 2 + 3 * 4 should parse as 2 + (3 * 4)
        Expression expr = parser.parse("2 + 3 * 4");
        assertThat(expr).isInstanceOf(Expression.BinaryOp.class);
        Expression.BinaryOp add = (Expression.BinaryOp) expr;
        assertThat(add.operator()).isEqualTo("+");
        assertThat(add.right()).isInstanceOf(Expression.BinaryOp.class);
        assertThat(((Expression.BinaryOp) add.right()).operator()).isEqualTo("*");
    }

    @Test
    void parsesComparison() {
        Expression expr = parser.parse("age >= 18");
        assertThat(expr).isInstanceOf(Expression.Comparison.class);
        Expression.Comparison cmp = (Expression.Comparison) expr;
        assertThat(cmp.operator()).isEqualTo(">=");
    }

    @Test
    void parsesLogicalAnd() {
        Expression expr = parser.parse("a == 1 AND b == 2");
        assertThat(expr).isInstanceOf(Expression.LogicalOp.class);
        assertThat(((Expression.LogicalOp) expr).operator()).isEqualTo("AND");
    }

    @Test
    void parsesLogicalOr() {
        Expression expr = parser.parse("a == 1 OR b == 2");
        assertThat(expr).isInstanceOf(Expression.LogicalOp.class);
        assertThat(((Expression.LogicalOp) expr).operator()).isEqualTo("OR");
    }

    @Test
    void parsesNotUnary() {
        Expression expr = parser.parse("NOT true");
        assertThat(expr).isInstanceOf(Expression.UnaryOp.class);
        assertThat(((Expression.UnaryOp) expr).operator()).isEqualTo("NOT");
    }

    @Test
    void parsesNegation() {
        Expression expr = parser.parse("-5");
        assertThat(expr).isInstanceOf(Expression.UnaryOp.class);
        assertThat(((Expression.UnaryOp) expr).operator()).isEqualTo("-");
    }

    @Test
    void parsesPropertyAccess() {
        Expression expr = parser.parse("customer.age");
        assertThat(expr).isInstanceOf(Expression.PropertyAccess.class);
        Expression.PropertyAccess pa = (Expression.PropertyAccess) expr;
        assertThat(pa.property()).isEqualTo("age");
    }

    @Test
    void parsesNullSafeAccess() {
        Expression expr = parser.parse("customer?.address");
        assertThat(expr).isInstanceOf(Expression.NullSafeAccess.class);
        assertThat(((Expression.NullSafeAccess) expr).property()).isEqualTo("address");
    }

    @Test
    void parsesIndexAccess() {
        Expression expr = parser.parse("items[0]");
        assertThat(expr).isInstanceOf(Expression.IndexAccess.class);
    }

    @Test
    void parsesFunctionCall() {
        Expression expr = parser.parse("ABS(-5)");
        assertThat(expr).isInstanceOf(Expression.FunctionCall.class);
        Expression.FunctionCall fc = (Expression.FunctionCall) expr;
        assertThat(fc.name()).isEqualTo("ABS");
        assertThat(fc.arguments()).hasSize(1);
    }

    @Test
    void parsesBetweenExpression() {
        Expression expr = parser.parse("age BETWEEN 18 AND 65");
        assertThat(expr).isInstanceOf(Expression.BetweenExpr.class);
    }

    @Test
    void parsesInExpression() {
        Expression expr = parser.parse("status IN [\"A\", \"B\", \"C\"]");
        assertThat(expr).isInstanceOf(Expression.InExpr.class);
        assertThat(((Expression.InExpr) expr).negated()).isFalse();
    }

    @Test
    void parsesNotInExpression() {
        Expression expr = parser.parse("status NOT_IN [\"X\"]");
        assertThat(expr).isInstanceOf(Expression.InExpr.class);
        assertThat(((Expression.InExpr) expr).negated()).isTrue();
    }

    @Test
    void parsesIsNull() {
        Expression expr = parser.parse("name IS_NULL");
        assertThat(expr).isInstanceOf(Expression.Comparison.class);
        assertThat(((Expression.Comparison) expr).operator()).isEqualTo("IS_NULL");
    }

    @Test
    void parsesIsNotNull() {
        Expression expr = parser.parse("name IS_NOT_NULL");
        assertThat(expr).isInstanceOf(Expression.Comparison.class);
        assertThat(((Expression.Comparison) expr).operator()).isEqualTo("IS_NOT_NULL");
    }

    @Test
    void parsesNullCoalescing() {
        Expression expr = parser.parse("name ?? \"default\"");
        assertThat(expr).isInstanceOf(Expression.NullCoalesce.class);
    }

    @Test
    void parsesLambda() {
        Expression expr = parser.parse("x -> x + 1");
        assertThat(expr).isInstanceOf(Expression.Lambda.class);
        Expression.Lambda lam = (Expression.Lambda) expr;
        assertThat(lam.parameter()).isEqualTo("x");
    }

    @Test
    void parsesArrayLiteral() {
        Expression expr = parser.parse("[1, 2, 3]");
        assertThat(expr).isInstanceOf(Expression.ArrayLiteral.class);
        assertThat(((Expression.ArrayLiteral) expr).elements()).hasSize(3);
    }

    @Test
    void parsesGroupedExpression() {
        Expression expr = parser.parse("(2 + 3) * 4");
        assertThat(expr).isInstanceOf(Expression.BinaryOp.class);
        Expression.BinaryOp mul = (Expression.BinaryOp) expr;
        assertThat(mul.operator()).isEqualTo("*");
        assertThat(mul.left()).isInstanceOf(Expression.BinaryOp.class);
    }

    @Test
    void parsesComplexExpression() {
        Expression expr = parser.parse("customer.age >= 18 AND customer.status == \"active\"");
        assertThat(expr).isInstanceOf(Expression.LogicalOp.class);
    }

    @Test
    void throwsOnUnexpectedToken() {
        assertThatThrownBy(() -> parser.parse("1 + + 2"))
                .isInstanceOf(RflParseException.class);
    }
}
