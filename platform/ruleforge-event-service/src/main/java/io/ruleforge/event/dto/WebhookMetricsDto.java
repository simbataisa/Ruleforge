package io.ruleforge.event.dto;

import java.util.List;

public record WebhookMetricsDto(
        String webhookId,
        String period,
        double successRate,
        int totalDeliveries,
        int successDeliveries,
        int failedDeliveries,
        int dlqDepth,
        Integer avgLatencyMs,
        List<DeliveryTimelineEntry> deliveryTimeline
) {}
