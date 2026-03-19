package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Injects a single custom HTTP header for webhooks configured with
 * {@link WebhookAuthType#CUSTOM_HEADER}. Throws {@link WebhookAuthException}
 * if the header value is missing or blank.
 */
@Component
public class CustomHeaderInjector implements WebhookAuthInjector {

    @Override
    public WebhookAuthType supportedType() {
        return WebhookAuthType.CUSTOM_HEADER;
    }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.CustomHeader ch) {
            if (ch.headerValue() == null || ch.headerValue().isBlank()) {
                throw new WebhookAuthException("CUSTOM_HEADER headerValue is blank or missing");
            }
            String name = (ch.headerName() != null && !ch.headerName().isBlank())
                    ? ch.headerName()
                    : "X-Custom-Header";
            headers.set(name, ch.headerValue());
        }
    }
}
