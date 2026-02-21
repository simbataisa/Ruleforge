package io.ruleforge.core.expression;

import io.ruleforge.core.expression.Token.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written recursive-descent parser for the RFL expression language.
 *
 * <p><strong>Precedence (lowest to highest):</strong></p>
 * <ol>
 *   <li>Null coalescing ({@code ??})</li>
 *   <li>Logical OR</li>
 *   <li>Logical AND</li>
 *   <li>Comparison / relational</li>
 *   <li>Addition / subtraction</li>
 *   <li>Multiplication / division / modulo</li>
 *   <li>Unary (NOT, negation)</li>
 *   <li>Postfix (dot access, index, call)</li>
 *   <li>Primary (literals, identifiers, grouping)</li>
 * </ol>
 */
public final class RflParser {

    private List<Token> tokens;
    private int current;

    // ------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------

    /**
     * Parses a raw RFL expression string into an AST.
     *
     * @param input the expression source text
     * @return the root {@link Expression} node
     * @throws RflParseException on lexer or parser errors
     */
    public Expression parse(String input) {
        return parse(new RflLexer(input).tokenize());
    }

    /**
     * Parses a pre-tokenized list of tokens into an AST.
     *
     * @param tokens the token list (must end with {@link TokenType#EOF})
     * @return the root {@link Expression} node
     * @throws RflParseException on parser errors
     */
    public Expression parse(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
        Expression expr = parseNullCoalesce();
        if (!isAtEnd()) {
            throw error("Unexpected token '" + peek().value() + "' after expression");
        }
        return expr;
    }

    // ------------------------------------------------------------------
    // Grammar rules (ordered by increasing precedence)
    // ------------------------------------------------------------------

    /** nullCoalesce = logicalOr ( "??" logicalOr )* ; */
    private Expression parseNullCoalesce() {
        Expression expr = parseLogicalOr();
        while (match(TokenType.DOUBLE_QUESTION)) {
            Expression right = parseLogicalOr();
            expr = new Expression.NullCoalesce(expr, right);
        }
        return expr;
    }

    /** logicalOr = logicalAnd ( "OR" logicalAnd )* ; */
    private Expression parseLogicalOr() {
        Expression expr = parseLogicalAnd();
        while (match(TokenType.OR)) {
            Expression right = parseLogicalAnd();
            expr = new Expression.LogicalOp("OR", expr, right);
        }
        return expr;
    }

    /** logicalAnd = comparison ( "AND" comparison )* ; */
    private Expression parseLogicalAnd() {
        Expression expr = parseComparison();
        while (match(TokenType.AND)) {
            Expression right = parseComparison();
            expr = new Expression.LogicalOp("AND", expr, right);
        }
        return expr;
    }

    /**
     * comparison = addition ( compOp addition )? ;
     * Special: BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL
     */
    private Expression parseComparison() {
        Expression expr = parseAddition();

        // IS_NULL / IS_NOT_NULL are postfix-unary
        if (match(TokenType.IS_NULL)) {
            return new Expression.Comparison("IS_NULL", expr, new Expression.Literal(null));
        }
        if (match(TokenType.IS_NOT_NULL)) {
            return new Expression.Comparison("IS_NOT_NULL", expr, new Expression.Literal(null));
        }

        // BETWEEN expr AND expr
        if (match(TokenType.BETWEEN)) {
            Expression low = parseAddition();
            consume(TokenType.AND, "Expected 'AND' in BETWEEN expression");
            Expression high = parseAddition();
            return new Expression.BetweenExpr(expr, low, high);
        }

        // IN [values]
        if (match(TokenType.IN)) {
            List<Expression> elements = parseArrayElements();
            return new Expression.InExpr(expr, elements, false);
        }

        // NOT_IN [values]
        if (match(TokenType.NOT_IN)) {
            List<Expression> elements = parseArrayElements();
            return new Expression.InExpr(expr, elements, true);
        }

        // Standard binary comparisons
        if (checkAny(TokenType.EQ, TokenType.NEQ, TokenType.GT, TokenType.GTE,
                TokenType.LT, TokenType.LTE, TokenType.MATCHES,
                TokenType.STARTS_WITH, TokenType.ENDS_WITH, TokenType.CONTAINS)) {
            Token op = advance();
            Expression right = parseAddition();
            return new Expression.Comparison(op.value(), expr, right);
        }

        return expr;
    }

    /** addition = multiplication ( ("+" | "-") multiplication )* ; */
    private Expression parseAddition() {
        Expression expr = parseMultiplication();
        while (checkAny(TokenType.PLUS, TokenType.MINUS)) {
            Token op = advance();
            Expression right = parseMultiplication();
            expr = new Expression.BinaryOp(op.value(), expr, right);
        }
        return expr;
    }

    /** multiplication = unary ( ("*" | "/" | "%") unary )* ; */
    private Expression parseMultiplication() {
        Expression expr = parseUnary();
        while (checkAny(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token op = advance();
            Expression right = parseUnary();
            expr = new Expression.BinaryOp(op.value(), expr, right);
        }
        return expr;
    }

    /** unary = ("NOT" | "-") unary | postfix ; */
    private Expression parseUnary() {
        if (match(TokenType.NOT)) {
            Expression operand = parseUnary();
            return new Expression.UnaryOp("NOT", operand);
        }
        if (match(TokenType.MINUS)) {
            Expression operand = parseUnary();
            return new Expression.UnaryOp("-", operand);
        }
        return parsePostfix();
    }

    /**
     * postfix = primary ( "." identifier | "?." identifier
     *                    | "[" expression "]"
     *                    | "(" arguments ")" )* ;
     */
    private Expression parsePostfix() {
        Expression expr = parsePrimary();

        while (true) {
            if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expected property name after '.'");
                expr = new Expression.PropertyAccess(expr, name.value());
            } else if (match(TokenType.QUESTION_DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expected property name after '?.'");
                expr = new Expression.NullSafeAccess(expr, name.value());
            } else if (match(TokenType.LBRACKET)) {
                Expression index = parseNullCoalesce();
                consume(TokenType.RBRACKET, "Expected ']' after index expression");
                expr = new Expression.IndexAccess(expr, index);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * primary = number | string | boolean | "NULL" | identifier
     *         | "(" expression ")" | array_literal
     *         | function_call | lambda ;
     */
    private Expression parsePrimary() {
        // Number literal
        if (check(TokenType.NUMBER)) {
            Token t = advance();
            String v = t.value();
            if (v.contains(".")) {
                return new Expression.Literal(Double.parseDouble(v));
            }
            long longVal = Long.parseLong(v);
            if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                return new Expression.Literal((int) longVal);
            }
            return new Expression.Literal(longVal);
        }

        // String literal
        if (check(TokenType.STRING)) {
            Token t = advance();
            return new Expression.Literal(t.value());
        }

        // Boolean literal
        if (check(TokenType.BOOLEAN)) {
            Token t = advance();
            return new Expression.Literal(Boolean.parseBoolean(t.value()));
        }

        // NULL literal
        if (match(TokenType.NULL)) {
            return new Expression.Literal(null);
        }

        // Identifier, function call, or lambda
        if (check(TokenType.IDENTIFIER)) {
            Token ident = advance();

            // Lambda: identifier "->" expression
            if (check(TokenType.ARROW)) {
                advance(); // consume ->
                Expression body = parseNullCoalesce();
                return new Expression.Lambda(ident.value(), body);
            }

            // Function call: identifier "(" arguments ")"
            if (check(TokenType.LPAREN)) {
                advance(); // consume (
                List<Expression> args = parseArguments();
                consume(TokenType.RPAREN, "Expected ')' after function arguments");
                return new Expression.FunctionCall(ident.value(), args);
            }

            // Plain identifier
            return new Expression.Identifier(ident.value());
        }

        // Parenthesized expression
        if (match(TokenType.LPAREN)) {
            Expression expr = parseNullCoalesce();
            consume(TokenType.RPAREN, "Expected ')' after grouped expression");
            return expr;
        }

        // Array literal: "[" (expression ("," expression)*)? "]"
        if (check(TokenType.LBRACKET)) {
            return parseArrayLiteral();
        }

        throw error("Expected expression but found '" + peek().value() + "'");
    }

    // ------------------------------------------------------------------
    // Helper productions
    // ------------------------------------------------------------------

    /** arguments = ( expression ( "," expression )* )? ; */
    private List<Expression> parseArguments() {
        List<Expression> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            args.add(parseNullCoalesce());
            while (match(TokenType.COMMA)) {
                args.add(parseNullCoalesce());
            }
        }
        return args;
    }

    /** Parses "[" expression ("," expression)* "]" returning elements list. */
    private List<Expression> parseArrayElements() {
        consume(TokenType.LBRACKET, "Expected '[' for value list");
        List<Expression> elements = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            elements.add(parseNullCoalesce());
            while (match(TokenType.COMMA)) {
                elements.add(parseNullCoalesce());
            }
        }
        consume(TokenType.RBRACKET, "Expected ']' after value list");
        return elements;
    }

    /** array_literal = "[" (expression ("," expression)*)? "]" ; */
    private Expression parseArrayLiteral() {
        consume(TokenType.LBRACKET, "Expected '['");
        List<Expression> elements = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            elements.add(parseNullCoalesce());
            while (match(TokenType.COMMA)) {
                elements.add(parseNullCoalesce());
            }
        }
        consume(TokenType.RBRACKET, "Expected ']' after array elements");
        return new Expression.ArrayLiteral(elements);
    }

    // ------------------------------------------------------------------
    // Token navigation
    // ------------------------------------------------------------------

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        Token t = tokens.get(current);
        if (t.type() != TokenType.EOF) {
            current++;
        }
        return t;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean checkAny(TokenType... types) {
        TokenType current = peek().type();
        for (TokenType t : types) {
            if (current == t) return true;
        }
        return false;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(message);
    }

    private RflParseException error(String message) {
        return new RflParseException(message, peek().position());
    }
}
