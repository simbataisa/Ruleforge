package io.ruleforge.common.exception;

/**
 * Thrown when an operation would result in a conflict (e.g., duplicate name, version clash).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
