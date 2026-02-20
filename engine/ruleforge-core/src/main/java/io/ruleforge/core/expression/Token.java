package io.ruleforge.core.expression;

/**
 * A single lexical token produced by {@link RflLexer}.
 *
 * @param type     the token classification
 * @param value    the raw text of the token
 * @param position zero-based character offset in the source input
 */
public record Token(TokenType type, String value, int position) {

    public enum TokenType {
        // Literals
        NUMBER, STRING, BOOLEAN, NULL,
        // Identifiers
        IDENTIFIER,
        // Operators
        PLUS, MINUS, STAR, SLASH, PERCENT,
        EQ, NEQ, GT, GTE, LT, LTE,
        AND, OR, NOT,
        // Keywords (comparison operators)
        IN, NOT_IN, BETWEEN, MATCHES,
        STARTS_WITH, ENDS_WITH, CONTAINS,
        IS_NULL, IS_NOT_NULL,
        // Delimiters
        LPAREN, RPAREN, LBRACKET, RBRACKET,
        DOT, QUESTION_DOT, COMMA, ARROW,
        DOUBLE_QUESTION,  // ?? (null coalescing)
        // Special
        EOF
    }
}
