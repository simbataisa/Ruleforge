package io.ruleforge.core.expression;

import io.ruleforge.core.expression.Token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand-written lexer (tokenizer) for the RFL expression language.
 *
 * <p>Converts a raw input string into a list of {@link Token}s, skipping
 * whitespace and producing a trailing {@link TokenType#EOF} sentinel.
 * All position values are zero-based character offsets into the source string.
 */
public final class RflLexer {

    /**
     * Keyword/operator map. Multi-word keywords use underscores in the source
     * language (e.g. {@code NOT_IN}, {@code IS_NULL}).
     */
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("AND", TokenType.AND),
            Map.entry("OR", TokenType.OR),
            Map.entry("NOT", TokenType.NOT),
            Map.entry("IN", TokenType.IN),
            Map.entry("NOT_IN", TokenType.NOT_IN),
            Map.entry("BETWEEN", TokenType.BETWEEN),
            Map.entry("MATCHES", TokenType.MATCHES),
            Map.entry("STARTS_WITH", TokenType.STARTS_WITH),
            Map.entry("ENDS_WITH", TokenType.ENDS_WITH),
            Map.entry("CONTAINS", TokenType.CONTAINS),
            Map.entry("IS_NULL", TokenType.IS_NULL),
            Map.entry("IS_NOT_NULL", TokenType.IS_NOT_NULL),
            Map.entry("true", TokenType.BOOLEAN),
            Map.entry("false", TokenType.BOOLEAN),
            Map.entry("NULL", TokenType.NULL)
    );

    private final String input;
    private int pos;

    public RflLexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    /**
     * Tokenizes the entire input and returns an immutable token list
     * ending with {@link TokenType#EOF}.
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token token = nextToken();
            tokens.add(token);
            if (token.type() == TokenType.EOF) {
                break;
            }
        }
        return List.copyOf(tokens);
    }

    // ------------------------------------------------------------------
    // Core scanning
    // ------------------------------------------------------------------

    private Token nextToken() {
        skipWhitespace();

        if (pos >= input.length()) {
            return new Token(TokenType.EOF, "", pos);
        }

        char c = input.charAt(pos);

        // ----- String literal -----
        if (c == '"') {
            return readString();
        }

        // ----- Number literal -----
        if (Character.isDigit(c)) {
            return readNumber();
        }

        // ----- Identifier / keyword -----
        if (isIdentifierStart(c)) {
            return readIdentifierOrKeyword();
        }

        // ----- Two-character operators / delimiters -----
        if (pos + 1 < input.length()) {
            char next = input.charAt(pos + 1);

            switch (c) {
                case '=' -> {
                    if (next == '=') { pos += 2; return new Token(TokenType.EQ, "==", pos - 2); }
                }
                case '!' -> {
                    if (next == '=') { pos += 2; return new Token(TokenType.NEQ, "!=", pos - 2); }
                }
                case '>' -> {
                    if (next == '=') { pos += 2; return new Token(TokenType.GTE, ">=", pos - 2); }
                }
                case '<' -> {
                    if (next == '=') { pos += 2; return new Token(TokenType.LTE, "<=", pos - 2); }
                }
                case '-' -> {
                    if (next == '>') { pos += 2; return new Token(TokenType.ARROW, "->", pos - 2); }
                }
                case '?' -> {
                    if (next == '.') { pos += 2; return new Token(TokenType.QUESTION_DOT, "?.", pos - 2); }
                    if (next == '?') { pos += 2; return new Token(TokenType.DOUBLE_QUESTION, "??", pos - 2); }
                }
                default -> { /* fall through to single-char */ }
            }
        }

        // ----- Single-character operators / delimiters -----
        int startPos = pos;
        pos++;
        return switch (c) {
            case '+' -> new Token(TokenType.PLUS, "+", startPos);
            case '-' -> new Token(TokenType.MINUS, "-", startPos);
            case '*' -> new Token(TokenType.STAR, "*", startPos);
            case '/' -> new Token(TokenType.SLASH, "/", startPos);
            case '%' -> new Token(TokenType.PERCENT, "%", startPos);
            case '>' -> new Token(TokenType.GT, ">", startPos);
            case '<' -> new Token(TokenType.LT, "<", startPos);
            case '(' -> new Token(TokenType.LPAREN, "(", startPos);
            case ')' -> new Token(TokenType.RPAREN, ")", startPos);
            case '[' -> new Token(TokenType.LBRACKET, "[", startPos);
            case ']' -> new Token(TokenType.RBRACKET, "]", startPos);
            case '.' -> new Token(TokenType.DOT, ".", startPos);
            case ',' -> new Token(TokenType.COMMA, ",", startPos);
            default -> throw new RflParseException("Unexpected character '" + c + "'", startPos);
        };
    }

    // ------------------------------------------------------------------
    // Specialized readers
    // ------------------------------------------------------------------

    private Token readString() {
        int start = pos;
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\') {
                if (pos + 1 >= input.length()) {
                    throw new RflParseException("Unterminated escape sequence in string", pos);
                }
                char escaped = input.charAt(pos + 1);
                switch (escaped) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '"'  -> sb.append('"');
                    default   -> throw new RflParseException(
                            "Unknown escape sequence '\\" + escaped + "'", pos);
                }
                pos += 2;
            } else if (c == '"') {
                pos++; // skip closing quote
                return new Token(TokenType.STRING, sb.toString(), start);
            } else {
                sb.append(c);
                pos++;
            }
        }
        throw new RflParseException("Unterminated string literal", start);
    }

    private Token readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        // Decimal part
        if (pos < input.length() && input.charAt(pos) == '.'
                && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1))) {
            pos++; // skip dot
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        return new Token(TokenType.NUMBER, input.substring(start, pos), start);
    }

    private Token readIdentifierOrKeyword() {
        int start = pos;
        while (pos < input.length() && isIdentifierPart(input.charAt(pos))) {
            pos++;
        }
        String text = input.substring(start, pos);

        TokenType keywordType = KEYWORDS.get(text);
        if (keywordType != null) {
            return new Token(keywordType, text, start);
        }
        return new Token(TokenType.IDENTIFIER, text, start);
    }

    // ------------------------------------------------------------------
    // Character classification helpers
    // ------------------------------------------------------------------

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
