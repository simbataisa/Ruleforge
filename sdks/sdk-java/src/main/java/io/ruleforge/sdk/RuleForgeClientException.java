package io.ruleforge.sdk;

/**
 * Unchecked exception thrown when an SDK API call fails.
 */
public class RuleForgeClientException extends RuntimeException {

    public RuleForgeClientException(String message) {
        super(message);
    }

    public RuleForgeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
