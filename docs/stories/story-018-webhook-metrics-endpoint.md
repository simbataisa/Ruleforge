# Story 018 — Webhook Delivery Metrics Endpoint

**Story ID:** story-018
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Backend Engineer
**Priority:** P1 — Sprint 5.3
**Estimate:** 2 story points
**Depends on:** Story 014 (compound index on `webhook_deliveries`)

---

## User Story

**As a** DevOps engineer or org owner,
**I want** `GET /v1/webhooks/{id}/metrics` to return 7-day delivery statistics,
**So that** I can see at a glance whether my webhook is healthy without tailing logs.

---

## Background

ADR-009 decided: on-demand SQL aggregate from `webhook_deliveries` with the compound index added in Story 014. No pre-aggregation table or background job needed.

The endpoint is tenant-scoped (RLS enforced via `TenantContext`) and requires OWNER or EDITOR role.

---

## Acceptance Criteria

1. `GET /v1/webhooks/{id}/metrics` returns HTTP 200 with `WebhookMetricsDto`.
2. Response fields:
   - `webhookId` (UUID string)
   - `period` (`"7d"`)
   - `successRate` (double, 0.0–100.0, rounded to 1 decimal)
   - `totalDeliveries` (int)
   - `successDeliveries` (int)
   - `failedDeliveries` (int)
   - `dlqDepth` (int — count of `IN_DLQ` deliveries currently)
   - `avgLatencyMs` (int — average of `delivered_at - attempted_at` in ms, null if no successful deliveries)
   - `deliveryTimeline` (list of `{ date: "YYYY-MM-DD", success: N, failed: N }` for last 7 days, ordered ascending)
3. Empty state (no deliveries): all numeric fields = 0, `successRate` = 0.0, `deliveryTimeline` = [] (not null).
4. `GET /v1/webhooks/{id}/metrics` returns 404 if webhook not found or belongs to different org.
5. VIEWER role: returns 403.
6. Response is correctly scoped to the authenticated org (no cross-tenant data).
7. `avgLatencyMs` is derived from `delivered_at - attempted_at` for `SUCCESS` status rows only.
8. `dlqDepth` counts deliveries in `IN_DLQ` status regardless of age (not limited to 7 days).
9. Unit test mocks repository and verifies DTO computation.
10. `./gradlew :ruleforge-event-service:test` passes.

---

## Files to Create

### `io.ruleforge.event.service.WebhookMetricsService`

```java
@Service
@RequiredArgsConstructor
public class WebhookMetricsService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookRepository webhookRepository;

    public WebhookMetricsDto getMetrics(UUID webhookId, UUID orgId) {
        // Verify ownership
        webhookRepository.findByIdAndOrgId(webhookId, orgId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);

        // Query aggregate from webhook_deliveries
        List<DailyDeliveryStats> timeline = deliveryRepository
            .findDailyStats(webhookId, orgId, since);

        int total = timeline.stream().mapToInt(d -> d.success() + d.failed()).sum();
        int success = timeline.stream().mapToInt(DailyDeliveryStats::success).sum();
        int failed = timeline.stream().mapToInt(DailyDeliveryStats::failed).sum();
        int dlqDepth = deliveryRepository.countByWebhookIdAndOrgIdAndStatus(
            webhookId, orgId, WebhookDeliveryStatus.IN_DLQ);
        Integer avgLatency = deliveryRepository.avgLatencyMs(webhookId, orgId, since);

        double successRate = total == 0 ? 0.0
            : Math.round((success * 1000.0 / total)) / 10.0;

        return new WebhookMetricsDto(
            webhookId.toString(), "7d",
            successRate, total, success, failed, dlqDepth,
            avgLatency,
            timeline.stream().map(d -> new DeliveryTimelineEntry(
                d.date().toString(), d.success(), d.failed()
            )).toList()
        );
    }
}
```

### `WebhookMetricsDto.java` (new response DTO)

```java
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

public record DeliveryTimelineEntry(String date, int success, int failed) {}
```

---

## Files to Modify

### `WebhookController.java` — add metrics endpoint

```java
@GetMapping("/{id}/metrics")
@PreAuthorize("hasAnyRole('OWNER', 'EDITOR')")
public ResponseEntity<WebhookMetricsDto> getMetrics(
        @PathVariable UUID id,
        @AuthenticationPrincipal JwtUserDetails user) {
    UUID orgId = TenantContext.getCurrentOrgId();
    return ResponseEntity.ok(metricsService.getMetrics(id, orgId));
}
```

### `WebhookDeliveryRepository.java` — add query methods

```java
// Native query for daily timeline
@Query(nativeQuery = true, value = """
    SELECT
        date_trunc('day', attempted_at)::DATE AS date,
        COUNT(*) FILTER (WHERE status = 'SUCCESS') AS success,
        COUNT(*) FILTER (WHERE status != 'SUCCESS') AS failed
    FROM webhook_deliveries
    WHERE webhook_id = :webhookId
      AND org_id = :orgId
      AND attempted_at >= :since
    GROUP BY date_trunc('day', attempted_at)
    ORDER BY date
    """)
List<DailyDeliveryStats> findDailyStats(
    @Param("webhookId") UUID webhookId,
    @Param("orgId") UUID orgId,
    @Param("since") Instant since);

// DLQ depth
int countByWebhookIdAndOrgIdAndStatus(UUID webhookId, UUID orgId, WebhookDeliveryStatus status);

// Average latency (ms)
@Query(nativeQuery = true, value = """
    SELECT AVG(EXTRACT(EPOCH FROM (delivered_at - attempted_at)) * 1000)::INT
    FROM webhook_deliveries
    WHERE webhook_id = :webhookId AND org_id = :orgId
      AND status = 'SUCCESS' AND attempted_at >= :since
    """)
Integer avgLatencyMs(
    @Param("webhookId") UUID webhookId,
    @Param("orgId") UUID orgId,
    @Param("since") Instant since);
```

---

## Definition of Done

- [ ] `WebhookMetricsService` with correct aggregation logic
- [ ] `WebhookMetricsDto` + `DeliveryTimelineEntry` records
- [ ] `GET /v1/webhooks/{id}/metrics` endpoint in `WebhookController`
- [ ] VIEWER role returns 403
- [ ] 404 on unknown / cross-tenant webhook
- [ ] Empty state returns all zeros + empty list (not null)
- [ ] Native queries use compound index (query plan verified in test)
- [ ] Unit tests: normal metrics, empty state, DLQ depth counting
- [ ] `./gradlew :ruleforge-event-service:test` passes
