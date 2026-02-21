package io.ruleforge.dmn.feel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-written recursive descent parser for S-FEEL (Simple FEEL) expressions.
 * <p>
 * Supports: arithmetic (+, -, *, /, **), comparisons (&lt;, &gt;, &lt;=, &gt;=, =, !=),
 * boolean logic (and, or, not), string literals, numbers, booleans, null,
 * if-then-else, ranges, lists, contexts, function calls, and property access.
 * <p>
 * Grammar (simplified):
 * <pre>
 * expression     = or_expr
 * or_expr        = and_expr ( "or" and_expr )*
 * and_expr       = not_expr ( "and" not_expr )*
 * not_expr       = "not" comparison | comparison
 * comparison     = addition ( ( "=" | "!=" | "&lt;" | "&gt;" | "&lt;=" | "&gt;=" | "in" | "between" ) addition )*
 * addition       = multiplication ( ( "+" | "-" ) multiplication )*
 * multiplication = power ( ( "*" | "/" ) power )*
 * power          = unary ( "**" unary )*
 * unary          = ( "-" ) unary | postfix
 * postfix        = primary ( "." IDENTIFIER | "[" expression "]" | "(" arguments ")" )*
 * primary        = NUMBER | STRING | BOOLEAN | NULL
 *                | if_expr | list | context | range | "(" expression ")" | IDENTIFIER
 * </pre>
 */
public final class FeelParser {

    private final List<Token> tokens;
    private int pos;

    public FeelParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public FeelNode parse() {
        FeelNode node = parseExpression();
        if (current().type() != Token.Type.EOF) {
            throw new FeelParseException("Unexpected token after expression: " + current());
        }
        return node;
    }

    /**
     * Parse an S-FEEL unary test expression (used in decision table input entries).
     * Unary tests: -, value, comparison, range, list of values, not(tests).
     */
    public FeelNode parseUnaryTest() {
        Token t = current();

        // Wildcard "-"
        if (t.type() == Token.Type.MINUS) {
            advance();
            if (current().type() == Token.Type.EOF || current().type() == Token.Type.COMMA) {
                return new FeelNode.WildcardNode();
            }
            // It's a negative number — back up and parse as expression
            pos--;
        }

        // Empty string means wildcard
        if (t.type() == Token.Type.EOF) {
            return new FeelNode.WildcardNode();
        }

        // "not" function for negated tests
        if (t.type() == Token.Type.NOT && peek().type() == Token.Type.LPAREN) {
            advance(); // skip 'not'
            advance(); // skip '('
            FeelNode inner = parseExpression();
            expect(Token.Type.RPAREN, "Expected ')' after not(...)");
            return new FeelNode.NotNode(inner);
        }

        // Range: [a..b], (a..b], etc.
        if (t.type() == Token.Type.LBRACKET || t.type() == Token.Type.LPAREN) {
            if (looksLikeRange()) {
                return parseRange();
            }
        }

        // Comparison operators as unary tests: < 10, >= 5, etc.
        if (isComparisonOp(t.type())) {
            advance();
            FeelNode operand = parseAddition();
            return new FeelNode.UnaryComparisonNode(t.value(), operand);
        }

        return parseExpression();
    }

    // ---------------------------------------------------------------
    // Expression parsing
    // ---------------------------------------------------------------

    private FeelNode parseExpression() {
        return parseOrExpr();
    }

    private FeelNode parseOrExpr() {
        FeelNode left = parseAndExpr();
        while (match(Token.Type.OR)) {
            FeelNode right = parseAndExpr();
            left = new FeelNode.BinaryNode("or", left, right);
        }
        return left;
    }

    private FeelNode parseAndExpr() {
        FeelNode left = parseNotExpr();
        while (match(Token.Type.AND)) {
            FeelNode right = parseNotExpr();
            left = new FeelNode.BinaryNode("and", left, right);
        }
        return left;
    }

    private FeelNode parseNotExpr() {
        if (match(Token.Type.NOT)) {
            FeelNode operand = parseComparison();
            return new FeelNode.NotNode(operand);
        }
        return parseComparison();
    }

    private FeelNode parseComparison() {
        FeelNode left = parseAddition();

        Token t = current();
        if (isComparisonOp(t.type())) {
            advance();
            FeelNode right = parseAddition();
            left = new FeelNode.BinaryNode(t.value(), left, right);
        } else if (t.type() == Token.Type.IN) {
            advance();
            FeelNode right = parseAddition();
            left = new FeelNode.BinaryNode("in", left, right);
        } else if (t.type() == Token.Type.BETWEEN) {
            advance();
            FeelNode low = parseAddition();
            expect(Token.Type.AND, "Expected 'and' in between expression");
            FeelNode high = parseAddition();
            left = new FeelNode.BetweenNode(left, low, high);
        }

        return left;
    }

    private FeelNode parseAddition() {
        FeelNode left = parseMultiplication();
        while (current().type() == Token.Type.PLUS || current().type() == Token.Type.MINUS) {
            String op = current().value();
            advance();
            FeelNode right = parseMultiplication();
            left = new FeelNode.BinaryNode(op, left, right);
        }
        return left;
    }

    private FeelNode parseMultiplication() {
        FeelNode left = parsePower();
        while (current().type() == Token.Type.STAR || current().type() == Token.Type.SLASH) {
            String op = current().value();
            advance();
            FeelNode right = parsePower();
            left = new FeelNode.BinaryNode(op, left, right);
        }
        return left;
    }

    private FeelNode parsePower() {
        FeelNode left = parseUnary();
        while (current().type() == Token.Type.POWER) {
            advance();
            FeelNode right = parseUnary();
            left = new FeelNode.BinaryNode("**", left, right);
        }
        return left;
    }

    private FeelNode parseUnary() {
        if (current().type() == Token.Type.MINUS) {
            advance();
            FeelNode operand = parseUnary();
            return new FeelNode.UnaryMinusNode(operand);
        }
        return parsePostfix();
    }

    private FeelNode parsePostfix() {
        FeelNode node = parsePrimary();
        while (true) {
            if (current().type() == Token.Type.DOT) {
                advance();
                String prop = expect(Token.Type.IDENTIFIER, "Expected property name after '.'").value();
                node = new FeelNode.PropertyAccessNode(node, prop);
            } else if (current().type() == Token.Type.LBRACKET) {
                advance();
                FeelNode index = parseExpression();
                expect(Token.Type.RBRACKET, "Expected ']'");
                node = new FeelNode.IndexAccessNode(node, index);
            } else if (current().type() == Token.Type.LPAREN && node instanceof FeelNode.IdentifierNode id) {
                advance();
                List<FeelNode> args = parseArguments();
                expect(Token.Type.RPAREN, "Expected ')'");
                node = new FeelNode.FunctionCallNode(id.name(), args);
            } else {
                break;
            }
        }
        return node;
    }

    private FeelNode parsePrimary() {
        Token t = current();

        switch (t.type()) {
            case NUMBER -> {
                advance();
                return new FeelNode.NumberNode(Double.parseDouble(t.value()));
            }
            case STRING -> {
                advance();
                return new FeelNode.StringNode(t.value());
            }
            case BOOLEAN -> {
                advance();
                return new FeelNode.BooleanNode("true".equals(t.value()));
            }
            case NULL -> {
                advance();
                return new FeelNode.NullNode();
            }
            case IF -> {
                return parseIfExpr();
            }
            case LBRACKET, LPAREN -> {
                if (looksLikeRange()) {
                    return parseRange();
                }
                if (t.type() == Token.Type.LBRACKET) {
                    return parseList();
                }
                // Parenthesized expression
                advance();
                FeelNode expr = parseExpression();
                expect(Token.Type.RPAREN, "Expected ')'");
                return expr;
            }
            case LBRACE -> {
                return parseContext();
            }
            case IDENTIFIER -> {
                advance();
                return new FeelNode.IdentifierNode(t.value());
            }
            default -> throw new FeelParseException("Unexpected token: " + t.type() + " '" + t.value() + "'");
        }
    }

    private FeelNode parseIfExpr() {
        expect(Token.Type.IF, "Expected 'if'");
        FeelNode condition = parseExpression();
        expect(Token.Type.THEN, "Expected 'then'");
        FeelNode thenExpr = parseExpression();
        expect(Token.Type.ELSE, "Expected 'else'");
        FeelNode elseExpr = parseExpression();
        return new FeelNode.IfNode(condition, thenExpr, elseExpr);
    }

    private FeelNode parseList() {
        expect(Token.Type.LBRACKET, "Expected '['");
        List<FeelNode> elements = new ArrayList<>();
        if (current().type() != Token.Type.RBRACKET) {
            elements.add(parseExpression());
            while (match(Token.Type.COMMA)) {
                elements.add(parseExpression());
            }
        }
        expect(Token.Type.RBRACKET, "Expected ']'");
        return new FeelNode.ListNode(elements);
    }

    private FeelNode parseContext() {
        expect(Token.Type.LBRACE, "Expected '{'");
        Map<String, FeelNode> entries = new LinkedHashMap<>();
        if (current().type() != Token.Type.RBRACE) {
            parseContextEntry(entries);
            while (match(Token.Type.COMMA)) {
                parseContextEntry(entries);
            }
        }
        expect(Token.Type.RBRACE, "Expected '}'");
        return new FeelNode.ContextNode(entries);
    }

    private void parseContextEntry(Map<String, FeelNode> entries) {
        String key;
        if (current().type() == Token.Type.STRING) {
            key = current().value();
            advance();
        } else {
            key = expect(Token.Type.IDENTIFIER, "Expected context key").value();
        }
        expect(Token.Type.COLON, "Expected ':' after context key");
        FeelNode value = parseExpression();
        entries.put(key, value);
    }

    private FeelNode parseRange() {
        boolean startInclusive = current().type() == Token.Type.LBRACKET;
        advance(); // skip [ or (
        FeelNode start = parseAddition();
        expect(Token.Type.RANGE_OP, "Expected '..' in range");
        FeelNode end = parseAddition();
        boolean endInclusive = current().type() == Token.Type.RBRACKET;
        if (current().type() == Token.Type.RBRACKET || current().type() == Token.Type.RPAREN) {
            advance();
        } else {
            throw new FeelParseException("Expected ']' or ')' to close range");
        }
        return new FeelNode.RangeNode(start, end, startInclusive, endInclusive);
    }

    private List<FeelNode> parseArguments() {
        List<FeelNode> args = new ArrayList<>();
        if (current().type() != Token.Type.RPAREN) {
            args.add(parseExpression());
            while (match(Token.Type.COMMA)) {
                args.add(parseExpression());
            }
        }
        return args;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private boolean looksLikeRange() {
        // Scan ahead to find ".." before the matching bracket
        int depth = 0;
        for (int i = pos; i < tokens.size(); i++) {
            Token.Type tt = tokens.get(i).type();
            if (tt == Token.Type.LPAREN || tt == Token.Type.LBRACKET) depth++;
            else if (tt == Token.Type.RPAREN || tt == Token.Type.RBRACKET) {
                depth--;
                if (depth <= 0) return false;
            }
            else if (tt == Token.Type.RANGE_OP && depth == 1) return true;
            else if (tt == Token.Type.EOF) return false;
        }
        return false;
    }

    private boolean isComparisonOp(Token.Type type) {
        return type == Token.Type.LT || type == Token.Type.GT
                || type == Token.Type.LTE || type == Token.Type.GTE
                || type == Token.Type.EQ || type == Token.Type.NEQ;
    }

    private Token current() {
        return pos < tokens.size() ? tokens.get(pos) : new Token(Token.Type.EOF, "");
    }

    private Token peek() {
        int next = pos + 1;
        return next < tokens.size() ? tokens.get(next) : new Token(Token.Type.EOF, "");
    }

    private void advance() {
        if (pos < tokens.size()) pos++;
    }

    private boolean match(Token.Type type) {
        if (current().type() == type) {
            advance();
            return true;
        }
        return false;
    }

    private Token expect(Token.Type type, String message) {
        Token t = current();
        if (t.type() != type) {
            throw new FeelParseException(message + ", got " + t.type() + " '" + t.value() + "'");
        }
        advance();
        return t;
    }
}
