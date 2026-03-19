package io.ruleforge.event.exception;

/**
 * Thrown by WebhookAuthInjector implementations when authentication cannot be applied.
 * Signals AUTH_FAILURE — permanent, no retry, does not trigger circuit breaker.
 *
 * Examples: blank credential, SSRF-blocked tokenUrl, OAuth2 token endpoint non-2xx.
 */
public class WebhookAuthException extends RuntimeException {
    public WebhookAuthException(String message) { super(message); }
    public WebhookAuthException(String message, Throwable cause) { super(message, cause); }
}
