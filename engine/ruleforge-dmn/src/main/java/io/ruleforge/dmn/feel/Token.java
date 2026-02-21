package io.ruleforge.dmn.feel;

/**
 * A token produced by the S-FEEL lexer.
 *
 * @param type  the token type
 * @param value the literal text value
 */
public record Token(Type type, String value) {

    public enum Type {
        // Literals
        NUMBER,
        STRING,
        BOOLEAN,
        NULL,
        IDENTIFIER,

        // Arithmetic
        PLUS,
        MINUS,
        STAR,
        SLASH,
        POWER,

        // Comparison
        LT,
        GT,
        LTE,
        GTE,
        EQ,
        NEQ,

        // Logical
        AND,
        OR,
        NOT,

        // Keywords
        IF,
        THEN,
        ELSE,
        IN,
        BETWEEN,

        // Delimiters
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        LBRACE,
        RBRACE,
        COMMA,
        COLON,
        DOT,

        // Range
        RANGE_OP, // ..

        // End
        EOF
    }
}
