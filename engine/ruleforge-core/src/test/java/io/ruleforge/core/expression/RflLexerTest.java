package io.ruleforge.core.expression;

import io.ruleforge.core.expression.Token.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RflLexerTest {

    @Test
    void tokenizesSimpleIdentifier() {
        List<Token> tokens = new RflLexer("age").tokenize();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(0).value()).isEqualTo("age");
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void tokenizesIntegerLiteral() {
        List<Token> tokens = new RflLexer("42").tokenize();
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(0).value()).isEqualTo("42");
    }

    @Test
    void tokenizesDecimalLiteral() {
        List<Token> tokens = new RflLexer("3.14").tokenize();
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(0).value()).isEqualTo("3.14");
    }

    @Test
    void tokenizesStringLiteral() {
        List<Token> tokens = new RflLexer("\"hello world\"").tokenize();
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.STRING);
        assertThat(tokens.get(0).value()).isEqualTo("hello world");
    }

    @Test
    void tokenizesBooleanLiterals() {
        List<Token> trueTokens = new RflLexer("true").tokenize();
        assertThat(trueTokens.get(0).type()).isEqualTo(TokenType.BOOLEAN);
        assertThat(trueTokens.get(0).value()).isEqualTo("true");

        List<Token> falseTokens = new RflLexer("false").tokenize();
        assertThat(falseTokens.get(0).type()).isEqualTo(TokenType.BOOLEAN);
        assertThat(falseTokens.get(0).value()).isEqualTo("false");
    }

    @Test
    void tokenizesNullLiteral() {
        List<Token> tokens = new RflLexer("NULL").tokenize();
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.NULL);
    }

    @Test
    void tokenizesComparisonOperators() {
        assertTokenType("==", TokenType.EQ);
        assertTokenType("!=", TokenType.NEQ);
        assertTokenType(">", TokenType.GT);
        assertTokenType(">=", TokenType.GTE);
        assertTokenType("<", TokenType.LT);
        assertTokenType("<=", TokenType.LTE);
    }

    @Test
    void tokenizesArithmeticOperators() {
        assertTokenType("+", TokenType.PLUS);
        assertTokenType("-", TokenType.MINUS);
        assertTokenType("*", TokenType.STAR);
        assertTokenType("/", TokenType.SLASH);
        assertTokenType("%", TokenType.PERCENT);
    }

    @Test
    void tokenizesLogicalKeywords() {
        assertTokenType("AND", TokenType.AND);
        assertTokenType("OR", TokenType.OR);
        assertTokenType("NOT", TokenType.NOT);
    }

    @Test
    void tokenizesComparisonKeywords() {
        assertTokenType("IN", TokenType.IN);
        assertTokenType("NOT_IN", TokenType.NOT_IN);
        assertTokenType("BETWEEN", TokenType.BETWEEN);
        assertTokenType("MATCHES", TokenType.MATCHES);
        assertTokenType("STARTS_WITH", TokenType.STARTS_WITH);
        assertTokenType("ENDS_WITH", TokenType.ENDS_WITH);
        assertTokenType("CONTAINS", TokenType.CONTAINS);
        assertTokenType("IS_NULL", TokenType.IS_NULL);
        assertTokenType("IS_NOT_NULL", TokenType.IS_NOT_NULL);
    }

    @Test
    void tokenizesComplexExpression() {
        List<Token> tokens = new RflLexer("age >= 18 AND status == \"active\"").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.GTE, TokenType.NUMBER,
                TokenType.AND,
                TokenType.IDENTIFIER, TokenType.EQ, TokenType.STRING,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesDotAccess() {
        List<Token> tokens = new RflLexer("customer.age").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.DOT, TokenType.IDENTIFIER, TokenType.EOF
        );
    }

    @Test
    void tokenizesNullSafeAccess() {
        List<Token> tokens = new RflLexer("customer?.age").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.QUESTION_DOT, TokenType.IDENTIFIER, TokenType.EOF
        );
    }

    @Test
    void tokenizesNullCoalescing() {
        List<Token> tokens = new RflLexer("a ?? b").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.DOUBLE_QUESTION, TokenType.IDENTIFIER, TokenType.EOF
        );
    }

    @Test
    void tokenizesBracketsAndParens() {
        List<Token> tokens = new RflLexer("items[0]").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.LBRACKET, TokenType.NUMBER, TokenType.RBRACKET, TokenType.EOF
        );
    }

    @Test
    void tokenizesArrow() {
        List<Token> tokens = new RflLexer("x -> x + 1").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.IDENTIFIER, TokenType.ARROW,
                TokenType.IDENTIFIER, TokenType.PLUS, TokenType.NUMBER,
                TokenType.EOF
        );
    }

    @Test
    void skipsWhitespace() {
        List<Token> tokens = new RflLexer("  a   +   b  ").tokenize();
        assertThat(tokens).hasSize(4); // a, +, b, EOF
    }

    @Test
    void recordsPositions() {
        List<Token> tokens = new RflLexer("a + b").tokenize();
        assertThat(tokens.get(0).position()).isEqualTo(0);
        assertThat(tokens.get(1).position()).isEqualTo(2);
        assertThat(tokens.get(2).position()).isEqualTo(4);
    }

    private void assertTokenType(String input, TokenType expected) {
        List<Token> tokens = new RflLexer(input).tokenize();
        assertThat(tokens.get(0).type()).isEqualTo(expected);
    }
}
