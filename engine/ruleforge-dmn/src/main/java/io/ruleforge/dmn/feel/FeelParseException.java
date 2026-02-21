package io.ruleforge.dmn.feel;

/**
 * Thrown when an S-FEEL expression cannot be parsed or evaluated.
 */
public class FeelParseException extends RuntimeException {

    public FeelParseException(String message) {
        super(message);
    }

    public FeelParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
