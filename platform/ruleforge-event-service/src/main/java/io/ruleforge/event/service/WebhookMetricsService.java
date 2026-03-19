package io.ruleforge.event.service;

import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.event.dto.DeliveryTimelineEntry;
import io.ruleforge.event.dto.WebhookMetricsDto;
import io.ruleforge.event.repository.DailyDeliveryStats;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookMetricsService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookRepository webhookRepository;

    public WebhookMetricsService(WebhookDeliveryRepository deliveryRepository,
                                  WebhookRepository webhookRepository) {
        this.deliveryRepository = deliveryRepository;
        this.webhookRepository = webhookRepository;
    }

    @Transactional(readOnly = true)
    public WebhookMetricsDto getMetrics(UUID webhookId, UUID orgId) {
        // Verify ownership (throws 404 if not found or cross-tenant)
        webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);

        List<DailyDeliveryStats> timeline = deliveryRepository.findDailyStats(webhookId, orgId, since);

        int totalSuccess = timeline.stream().mapToInt(DailyDeliveryStats::getSuccess).sum();
        int totalFailed = timeline.stream().mapToInt(DailyDeliveryStats::getFailed).sum();
        int total = totalSuccess + totalFailed;

        int dlqDepth = deliveryRepository.countByWebhookIdAndOrgIdAndStatus(webhookId, orgId, "IN_DLQ");
        Integer avgLatency = deliveryRepository.avgLatencyMs(webhookId, orgId, since);

        double successRate = total == 0 ? 0.0
                : Math.round(totalSuccess * 1000.0 / total) / 10.0;

        List<DeliveryTimelineEntry> entries = timeline.stream()
                .map(d -> new DeliveryTimelineEntry(d.getDate().toString(), d.getSuccess(), d.getFailed()))
                .toList();

        return new WebhookMetricsDto(
                webhookId.toString(), "7d",
                successRate, total, totalSuccess, totalFailed, dlqDepth,
                avgLatency,
                entries
        );
    }
}
