package io.ruleforge.event.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebhookAuthConfig.None.class, name = "NONE"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.SigningSecret.class, name = "SIGNING_SECRET"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.ApiKey.class, name = "API_KEY"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BasicAuth.class, name = "BASIC_AUTH"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BearerToken.class, name = "BEARER_TOKEN"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.CustomHeader.class, name = "CUSTOM_HEADER")
})
public sealed interface WebhookAuthConfig permits
        WebhookAuthConfig.None,
        WebhookAuthConfig.SigningSecret,
        WebhookAuthConfig.ApiKey,
        WebhookAuthConfig.BasicAuth,
        WebhookAuthConfig.BearerToken,
        WebhookAuthConfig.CustomHeader {

    record None() implements WebhookAuthConfig {}
    record SigningSecret(String secret) implements WebhookAuthConfig {}
    record ApiKey(String headerName, String apiKey) implements WebhookAuthConfig {}
    record BasicAuth(String username, String password) implements WebhookAuthConfig {}
    record BearerToken(String token) implements WebhookAuthConfig {}
    record CustomHeader(String headerName, String headerValue) implements WebhookAuthConfig {}
}
