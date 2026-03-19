package io.ruleforge.event.dto;

import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.model.WebhookScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateWebhookRequest(
        @NotBlank String url,
        @NotEmpty List<String> events,
        @NotNull WebhookScopeType scopeType,
        @NotNull UUID scopeId,
        String name,
        Boolean active,
        WebhookAuthType authType,
        WebhookAuthConfigRequest authConfig,
        WebhookRetryConfigRequest retryConfig
) {}
