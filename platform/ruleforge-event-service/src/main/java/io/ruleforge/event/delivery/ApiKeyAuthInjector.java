package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.API_KEY; }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.ApiKey ak) {
            String headerName = (ak.headerName() != null && !ak.headerName().isBlank())
                    ? ak.headerName() : "X-API-Key";
            headers.set(headerName, ak.apiKey());
        }
    }
}
