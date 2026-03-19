package io.ruleforge.event.exception;

public class WebhookCredentialDecryptionException extends RuntimeException {
    public WebhookCredentialDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
    public WebhookCredentialDecryptionException(String message) {
        super(message);
    }
}
