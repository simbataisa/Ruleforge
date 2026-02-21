package io.ruleforge.analytics.controller;

import io.ruleforge.analytics.dto.DashboardMetricsDto;
import io.ruleforge.analytics.dto.DecisionLogDto;
import io.ruleforge.analytics.dto.DecisionQueryRequest;
import io.ruleforge.analytics.dto.RuleStatsDto;
import io.ruleforge.analytics.service.AnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller exposing analytics query endpoints.
 * All routes are prefixed with /v1/analytics and routed through Kong.
 */
@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Query decision logs with optional filtering by orgId, ruleId, and time range.
     *
     * GET /v1/analytics/decisions?orgId=&ruleId=&from=&to=&page=&size=
     */
    @GetMapping("/decisions")
    public ResponseEntity<Page<DecisionLogDto>> queryDecisions(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DecisionQueryRequest request = new DecisionQueryRequest(orgId, ruleId, from, to, page, size);
        Page<DecisionLogDto> result = analyticsService.queryDecisions(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Get aggregated dashboard metrics for an organization over a rolling window.
     *
     * GET /v1/analytics/decisions/aggregate?orgId=&hours=
     */
    @GetMapping("/decisions/aggregate")
    public ResponseEntity<DashboardMetricsDto> aggregateDecisions(
            @RequestParam String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        DashboardMetricsDto metrics = analyticsService.getDashboardMetrics(orgId, hours);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get aggregated statistics for a specific rule over a rolling window.
     *
     * GET /v1/analytics/rules/{id}/stats?days=
     */
    @GetMapping("/rules/{id}/stats")
    public ResponseEntity<RuleStatsDto> ruleStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "30") int days) {

        RuleStatsDto stats = analyticsService.getRuleStats(id, days);
        return ResponseEntity.ok(stats);
    }

    /**
     * Alias for /decisions/aggregate — returns dashboard metrics for an organization.
     *
     * GET /v1/analytics/dashboard?orgId=&hours=
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDto> dashboard(
            @RequestParam String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        DashboardMetricsDto metrics = analyticsService.getDashboardMetrics(orgId, hours);
        return ResponseEntity.ok(metrics);
    }
}
