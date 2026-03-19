package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;

public interface WebhookAuthInjector {
    WebhookAuthType supportedType();
    void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload);
}
