package io.ruleforge.event.dto;

import java.util.List;

public record UpdateWebhookRequest(
        String url,
        List<String> events,
        Boolean active,
        String name
) {}
