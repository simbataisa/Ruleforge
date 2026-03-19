package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class NoneAuthInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.NONE; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        // no-op: no auth header for NONE type
    }
}
