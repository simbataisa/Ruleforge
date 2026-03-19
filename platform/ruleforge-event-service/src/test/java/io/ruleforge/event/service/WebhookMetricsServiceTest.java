package io.ruleforge.event.service;

import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.event.dto.WebhookMetricsDto;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.repository.DailyDeliveryStats;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookMetricsServiceTest {

    @Mock private WebhookDeliveryRepository deliveryRepository;
    @Mock private WebhookRepository webhookRepository;

    @InjectMocks
    private WebhookMetricsService metricsService;

    // ------------------------------------------------------------------
    // Helper: build a DailyDeliveryStats mock with specified values
    // ------------------------------------------------------------------

    private DailyDeliveryStats dailyStats(int success, int failed, LocalDate date) {
        DailyDeliveryStats stats = mock(DailyDeliveryStats.class);
        when(stats.getSuccess()).thenReturn(success);
        when(stats.getFailed()).thenReturn(failed);
        when(stats.getDate()).thenReturn(date);
        return stats;
    }

    // ------------------------------------------------------------------
    // Test 1: normal case with 2 days of data
    // ------------------------------------------------------------------

    @Test
    void getMetrics_normalCase() {
        UUID webhookId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        // Ownership check — webhook exists
        WebhookEntity webhook = mock(WebhookEntity.class);
        when(webhookRepository.findByIdAndOrgId(webhookId, orgId))
                .thenReturn(Optional.of(webhook));

        // Two days: day1 success=10 failed=2 (total 12), day2 success=8 failed=0 (total 8)
        // Grand total: success=18, failed=2, total=20  →  successRate = round(18/20*1000)/10 = round(900)/10 = 90.0
        DailyDeliveryStats day1 = dailyStats(10, 2, LocalDate.of(2026, 3, 11));
        DailyDeliveryStats day2 = dailyStats(8, 0, LocalDate.of(2026, 3, 12));

        when(deliveryRepository.findDailyStats(eq(webhookId), eq(orgId), any(Instant.class)))
                .thenReturn(List.of(day1, day2));

        when(deliveryRepository.countByWebhookIdAndOrgIdAndStatus(webhookId, orgId, "IN_DLQ"))
                .thenReturn(1);

        when(deliveryRepository.avgLatencyMs(eq(webhookId), eq(orgId), any(Instant.class)))
                .thenReturn(250);

        WebhookMetricsDto dto = metricsService.getMetrics(webhookId, orgId);

        assertEquals(webhookId.toString(), dto.webhookId());
        assertEquals("7d", dto.period());
        assertEquals(90.0, dto.successRate(), 0.01);
        assertEquals(20, dto.totalDeliveries());
        assertEquals(18, dto.successDeliveries());
        assertEquals(2, dto.failedDeliveries());
        assertEquals(1, dto.dlqDepth());
        assertEquals(250, dto.avgLatencyMs());
        assertEquals(2, dto.deliveryTimeline().size());
    }

    // ------------------------------------------------------------------
    // Test 2: empty timeline (no deliveries yet)
    // ------------------------------------------------------------------

    @Test
    void getMetrics_emptyTimeline() {
        UUID webhookId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        WebhookEntity webhook = mock(WebhookEntity.class);
        when(webhookRepository.findByIdAndOrgId(webhookId, orgId))
                .thenReturn(Optional.of(webhook));

        when(deliveryRepository.findDailyStats(eq(webhookId), eq(orgId), any(Instant.class)))
                .thenReturn(List.of());

        when(deliveryRepository.countByWebhookIdAndOrgIdAndStatus(webhookId, orgId, "IN_DLQ"))
                .thenReturn(0);

        when(deliveryRepository.avgLatencyMs(eq(webhookId), eq(orgId), any(Instant.class)))
                .thenReturn(null);

        WebhookMetricsDto dto = metricsService.getMetrics(webhookId, orgId);

        assertEquals(0.0, dto.successRate(), 0.001,
                "successRate must be 0.0 when there are no deliveries");
        assertEquals(0, dto.totalDeliveries());
        assertEquals(0, dto.successDeliveries());
        assertEquals(0, dto.failedDeliveries());
        assertEquals(0, dto.dlqDepth());
        assertNull(dto.avgLatencyMs());
        assertTrue(dto.deliveryTimeline().isEmpty());
    }

    // ------------------------------------------------------------------
    // Test 3: webhook not found — must throw EntityNotFoundException
    // ------------------------------------------------------------------

    @Test
    void getMetrics_webhookNotFound_throwsEntityNotFoundException() {
        UUID webhookId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        when(webhookRepository.findByIdAndOrgId(webhookId, orgId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> metricsService.getMetrics(webhookId, orgId),
                "Must throw EntityNotFoundException when webhook does not belong to orgId");

        // No delivery repository calls should be made when ownership check fails
        verifyNoInteractions(deliveryRepository);
    }
}
