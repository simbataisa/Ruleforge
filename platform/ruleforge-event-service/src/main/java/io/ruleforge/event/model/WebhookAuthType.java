package io.ruleforge.event.model;

public enum WebhookAuthType {
    NONE,
    SIGNING_SECRET,
    API_KEY,
    BASIC_AUTH,
    BEARER_TOKEN,
    CUSTOM_HEADER,
    OAUTH2_CLIENT_CREDENTIALS
}
