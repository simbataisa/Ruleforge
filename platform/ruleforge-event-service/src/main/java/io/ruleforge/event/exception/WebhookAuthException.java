package io.ruleforge.event.exception;

public class WebhookAuthException extends RuntimeException {
    public WebhookAuthException(String message) { super(message); }
    public WebhookAuthException(String message, Throwable cause) { super(message, cause); }
}
