package io.ruleforge.core.expression;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.FactType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionEvaluatorTest {

    private final RflParser parser = new RflParser();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private WorkingMemory wm;

    @BeforeEach
    void setUp() {
        wm = new WorkingMemory();
    }

    private Object eval(String expr) {
        return evaluator.evaluate(parser.parse(expr), wm);
    }

    private boolean evalBool(String expr) {
        return evaluator.evaluateAsBoolean(parser.parse(expr), wm);
    }

    // -- Arithmetic --

    @Test
    void evaluatesAddition() {
        assertThat(eval("2 + 3")).isEqualTo(5);
    }

    @Test
    void evaluatesSubtraction() {
        assertThat(eval("10 - 4")).isEqualTo(6);
    }

    @Test
    void evaluatesMultiplication() {
        assertThat(eval("3 * 7")).isEqualTo(21);
    }

    @Test
    void evaluatesDivision() {
        assertThat(eval("10 / 4")).isEqualTo(2.5);
    }

    @Test
    void evaluatesModulo() {
        assertThat(eval("10 % 3")).isEqualTo(1);
    }

    @Test
    void evaluatesOperatorPrecedence() {
        assertThat(eval("2 + 3 * 4")).isEqualTo(14);
    }

    @Test
    void evaluatesGroupedExpression() {
        assertThat(eval("(2 + 3) * 4")).isEqualTo(20);
    }

    @Test
    void evaluatesNegation() {
        assertThat(eval("-5")).isEqualTo(-5);
    }

    @Test
    void evaluatesStringConcatenation() {
        assertThat(eval("\"hello\" + \" \" + \"world\"")).isEqualTo("hello world");
    }

    @Test
    void divisionByZeroThrows() {
        assertThatThrownBy(() -> eval("1 / 0"))
                .isInstanceOf(RflParseException.class);
    }

    // -- Comparisons --

    @Test
    void evaluatesEquals() {
        assertThat(evalBool("5 == 5")).isTrue();
        assertThat(evalBool("5 == 6")).isFalse();
    }

    @Test
    void evaluatesNotEquals() {
        assertThat(evalBool("5 != 6")).isTrue();
        assertThat(evalBool("5 != 5")).isFalse();
    }

    @Test
    void evaluatesGreaterThan() {
        assertThat(evalBool("10 > 5")).isTrue();
        assertThat(evalBool("5 > 10")).isFalse();
    }

    @Test
    void evaluatesLessThanOrEqual() {
        assertThat(evalBool("5 <= 5")).isTrue();
        assertThat(evalBool("6 <= 5")).isFalse();
    }

    // -- Logical --

    @Test
    void evaluatesLogicalAnd() {
        assertThat(evalBool("true AND true")).isTrue();
        assertThat(evalBool("true AND false")).isFalse();
    }

    @Test
    void evaluatesLogicalOr() {
        assertThat(evalBool("false OR true")).isTrue();
        assertThat(evalBool("false OR false")).isFalse();
    }

    @Test
    void evaluatesNotOperator() {
        assertThat(evalBool("NOT false")).isTrue();
        assertThat(evalBool("NOT true")).isFalse();
    }

    // -- Working Memory Integration --

    @Test
    void evaluatesIdentifierFromWorkingMemory() {
        wm.assertFact("age", 25, FactType.INTEGER);
        assertThat(eval("age")).isEqualTo(25);
    }

    @Test
    void evaluatesComparisonWithFacts() {
        wm.assertFact("age", 25, FactType.INTEGER);
        assertThat(evalBool("age >= 18")).isTrue();
        assertThat(evalBool("age < 18")).isFalse();
    }

    @Test
    void evaluatesPropertyAccess() {
        wm.assertFact("customer", Map.of("age", 30, "name", "John"), FactType.OBJECT);
        assertThat(eval("customer.age")).isEqualTo(30);
        assertThat(eval("customer.name")).isEqualTo("John");
    }

    @Test
    void evaluatesNullSafeAccessOnNull() {
        wm.assertFact("customer", null, FactType.ANY);
        assertThat(eval("customer?.age")).isNull();
    }

    @Test
    void evaluatesNullSafeAccessOnNonNull() {
        wm.assertFact("customer", Map.of("age", 30), FactType.OBJECT);
        assertThat(eval("customer?.age")).isEqualTo(30);
    }

    @Test
    void evaluatesIndexAccess() {
        wm.assertFact("items", List.of("a", "b", "c"), FactType.ARRAY);
        assertThat(eval("items[1]")).isEqualTo("b");
    }

    // -- BETWEEN --

    @Test
    void evaluatesBetween() {
        wm.assertFact("age", 25, FactType.INTEGER);
        assertThat(evalBool("age BETWEEN 18 AND 65")).isTrue();
        assertThat(evalBool("age BETWEEN 30 AND 65")).isFalse();
    }

    // -- IN / NOT_IN --

    @Test
    void evaluatesIn() {
        wm.assertFact("status", "active", FactType.STRING);
        assertThat(evalBool("status IN [\"active\", \"pending\"]")).isTrue();
        assertThat(evalBool("status IN [\"disabled\"]")).isFalse();
    }

    @Test
    void evaluatesNotIn() {
        wm.assertFact("status", "active", FactType.STRING);
        assertThat(evalBool("status NOT_IN [\"disabled\"]")).isTrue();
    }

    // -- Null coalescing --

    @Test
    void evaluatesNullCoalescing() {
        assertThat(eval("NULL ?? \"default\"")).isEqualTo("default");
        assertThat(eval("\"value\" ?? \"default\"")).isEqualTo("value");
    }

    // -- Built-in functions --

    @Test
    void evaluatesAbsFunction() {
        assertThat(eval("ABS(-5)")).isEqualTo(5.0);
    }

    // -- Truthiness --

    @Test
    void evaluatesTruthiness() {
        assertThat(evalBool("1")).isTrue();
        assertThat(evalBool("0")).isFalse();
        assertThat(evalBool("\"nonempty\"")).isTrue();
        assertThat(evalBool("\"\"")).isFalse();
        assertThat(evalBool("NULL")).isFalse();
    }
}
