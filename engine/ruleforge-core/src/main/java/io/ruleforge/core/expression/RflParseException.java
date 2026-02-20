package io.ruleforge.core.expression;

/**
 * Thrown when the RFL lexer or parser encounters invalid input.
 * Carries the character position where the error was detected so
 * that callers can produce meaningful diagnostics.
 */
public class RflParseException extends RuntimeException {

    private final int position;

    public RflParseException(String message, int position) {
        super(message + " (at position " + position + ")");
        this.position = position;
    }

    public RflParseException(String message, int position, Throwable cause) {
        super(message + " (at position " + position + ")", cause);
        this.position = position;
    }

    /**
     * Returns the zero-based character offset in the source input
     * where the error was detected.
     */
    public int getPosition() {
        return position;
    }
}
