package io.ruleforge.event.dto;

import io.ruleforge.event.model.WebhookAuthType;

import java.util.List;

public record UpdateWebhookRequest(
        String url,
        List<String> events,
        Boolean active,
        String name,
        WebhookAuthType authType,
        WebhookAuthConfigRequest authConfig,
        WebhookRetryConfigRequest retryConfig
) {}
