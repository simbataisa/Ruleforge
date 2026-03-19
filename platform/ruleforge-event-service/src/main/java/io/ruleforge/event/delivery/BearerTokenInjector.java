package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.BEARER_TOKEN; }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.BearerToken bt) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bt.token());
        }
    }
}
