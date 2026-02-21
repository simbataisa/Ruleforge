package io.ruleforge.dmn.feel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hand-written tokenizer for S-FEEL (Simple Friendly Enough Expression Language).
 * Supports: numbers, strings, booleans, identifiers, operators, comparisons,
 * ranges, lists, contexts, if-then-else, and built-in function names.
 */
public final class FeelLexer {

    private static final Set<String> KEYWORDS = Set.of(
            "true", "false", "null", "if", "then", "else",
            "not", "and", "or", "in", "between",
            "date", "time", "duration",
            "function", "for", "return", "some", "every", "satisfies"
    );

    private final String input;
    private int pos;
    private final int length;

    public FeelLexer(String input) {
        this.input = input;
        this.length = input.length();
        this.pos = 0;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < length) {
            skipWhitespace();
            if (pos >= length) break;

            char c = input.charAt(pos);

            if (c == '"') {
                tokens.add(readString());
            } else if (c == '-' && pos + 1 < length && Character.isDigit(input.charAt(pos + 1))
                    && (tokens.isEmpty() || isOperatorToken(tokens.get(tokens.size() - 1)))) {
                tokens.add(readNumber());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (c == '.' && pos + 1 < length && input.charAt(pos + 1) == '.') {
                pos += 2;
                tokens.add(new Token(Token.Type.RANGE_OP, ".."));
            } else {
                tokens.add(readSymbol());
            }
        }
        tokens.add(new Token(Token.Type.EOF, ""));
        return tokens;
    }

    private boolean isOperatorToken(Token t) {
        return t.type() == Token.Type.LPAREN || t.type() == Token.Type.LBRACKET
                || t.type() == Token.Type.COMMA || t.type() == Token.Type.COLON
                || t.type() == Token.Type.PLUS || t.type() == Token.Type.MINUS
                || t.type() == Token.Type.STAR || t.type() == Token.Type.SLASH
                || t.type() == Token.Type.LT || t.type() == Token.Type.GT
                || t.type() == Token.Type.LTE || t.type() == Token.Type.GTE
                || t.type() == Token.Type.EQ || t.type() == Token.Type.NEQ;
    }

    private void skipWhitespace() {
        while (pos < length && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private Token readString() {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < length && input.charAt(pos) != '"') {
            if (input.charAt(pos) == '\\' && pos + 1 < length) {
                pos++;
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> { sb.append('\\'); sb.append(escaped); }
                }
            } else {
                sb.append(input.charAt(pos));
            }
            pos++;
        }
        if (pos < length) pos++; // skip closing quote
        return new Token(Token.Type.STRING, sb.toString());
    }

    private Token readNumber() {
        int start = pos;
        if (input.charAt(pos) == '-') pos++;
        while (pos < length && Character.isDigit(input.charAt(pos))) pos++;
        if (pos < length && input.charAt(pos) == '.' && pos + 1 < length
                && Character.isDigit(input.charAt(pos + 1))) {
            pos++; // skip dot
            while (pos < length && Character.isDigit(input.charAt(pos))) pos++;
        }
        return new Token(Token.Type.NUMBER, input.substring(start, pos));
    }

    private Token readIdentifierOrKeyword() {
        int start = pos;
        while (pos < length && (Character.isLetterOrDigit(input.charAt(pos))
                || input.charAt(pos) == '_' || input.charAt(pos) == ' ')) {
            // FEEL allows spaces in names, but we stop at keywords
            if (input.charAt(pos) == ' ') {
                // Look ahead to see if this is a multi-word identifier or a keyword
                String ahead = peekWord(pos + 1);
                if (KEYWORDS.contains(ahead) || ahead.isEmpty()) {
                    break;
                }
                // Check if it's part of a known function like "string length"
                pos++;
            } else {
                pos++;
            }
        }
        String text = input.substring(start, pos).trim();

        return switch (text) {
            case "true" -> new Token(Token.Type.BOOLEAN, "true");
            case "false" -> new Token(Token.Type.BOOLEAN, "false");
            case "null" -> new Token(Token.Type.NULL, "null");
            case "if" -> new Token(Token.Type.IF, "if");
            case "then" -> new Token(Token.Type.THEN, "then");
            case "else" -> new Token(Token.Type.ELSE, "else");
            case "not" -> new Token(Token.Type.NOT, "not");
            case "and" -> new Token(Token.Type.AND, "and");
            case "or" -> new Token(Token.Type.OR, "or");
            case "in" -> new Token(Token.Type.IN, "in");
            case "between" -> new Token(Token.Type.BETWEEN, "between");
            default -> new Token(Token.Type.IDENTIFIER, text);
        };
    }

    private String peekWord(int from) {
        int i = from;
        while (i < length && Character.isWhitespace(input.charAt(i))) i++;
        int start = i;
        while (i < length && Character.isLetterOrDigit(input.charAt(i))) i++;
        return i > start ? input.substring(start, i) : "";
    }

    private Token readSymbol() {
        char c = input.charAt(pos);
        pos++;

        return switch (c) {
            case '(' -> new Token(Token.Type.LPAREN, "(");
            case ')' -> new Token(Token.Type.RPAREN, ")");
            case '[' -> new Token(Token.Type.LBRACKET, "[");
            case ']' -> new Token(Token.Type.RBRACKET, "]");
            case '{' -> new Token(Token.Type.LBRACE, "{");
            case '}' -> new Token(Token.Type.RBRACE, "}");
            case ',' -> new Token(Token.Type.COMMA, ",");
            case ':' -> new Token(Token.Type.COLON, ":");
            case '.' -> new Token(Token.Type.DOT, ".");
            case '+' -> new Token(Token.Type.PLUS, "+");
            case '-' -> new Token(Token.Type.MINUS, "-");
            case '*' -> {
                if (pos < length && input.charAt(pos) == '*') {
                    pos++;
                    yield new Token(Token.Type.POWER, "**");
                }
                yield new Token(Token.Type.STAR, "*");
            }
            case '/' -> new Token(Token.Type.SLASH, "/");
            case '=' -> new Token(Token.Type.EQ, "=");
            case '!' -> {
                if (pos < length && input.charAt(pos) == '=') {
                    pos++;
                    yield new Token(Token.Type.NEQ, "!=");
                }
                yield new Token(Token.Type.NOT, "!");
            }
            case '<' -> {
                if (pos < length && input.charAt(pos) == '=') {
                    pos++;
                    yield new Token(Token.Type.LTE, "<=");
                }
                yield new Token(Token.Type.LT, "<");
            }
            case '>' -> {
                if (pos < length && input.charAt(pos) == '=') {
                    pos++;
                    yield new Token(Token.Type.GTE, ">=");
                }
                yield new Token(Token.Type.GT, ">");
            }
            default -> throw new FeelParseException("Unexpected character: '" + c + "' at position " + (pos - 1));
        };
    }
}
