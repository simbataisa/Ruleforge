package io.ruleforge.analytics.controller;

import io.ruleforge.analytics.dto.AbTestResultsDto;
import io.ruleforge.analytics.dto.DashboardMetricsDto;
import io.ruleforge.analytics.dto.DecisionLogDto;
import io.ruleforge.analytics.dto.DecisionQueryRequest;
import io.ruleforge.analytics.dto.LatencyPercentilesDto;
import io.ruleforge.analytics.dto.PassFailPointDto;
import io.ruleforge.analytics.dto.RuleStatsDto;
import io.ruleforge.analytics.dto.SignificanceDto;
import io.ruleforge.analytics.dto.TimelinePointDto;
import io.ruleforge.analytics.dto.TypeDistributionDto;
import io.ruleforge.analytics.service.AnalyticsService;
import io.ruleforge.common.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/decisions")
    public ResponseEntity<Page<DecisionLogDto>> queryDecisions(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long minLatency,
            @RequestParam(required = false) Long maxLatency) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        DecisionQueryRequest request = new DecisionQueryRequest(resolvedOrgId, ruleId, from, to, page, size, status, search, minLatency, maxLatency);
        Page<DecisionLogDto> result = analyticsService.queryDecisions(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/decisions/aggregate")
    public ResponseEntity<DashboardMetricsDto> aggregateDecisions(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        DashboardMetricsDto metrics = analyticsService.getDashboardMetrics(resolvedOrgId, hours);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/rules/{id}/stats")
    public ResponseEntity<RuleStatsDto> ruleStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "30") int days) {

        RuleStatsDto stats = analyticsService.getRuleStats(id, days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDto> dashboard(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        DashboardMetricsDto metrics = analyticsService.getDashboardMetrics(resolvedOrgId, hours);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<TimelinePointDto>> timeline(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "1h") String interval) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        List<TimelinePointDto> result = analyticsService.getTimeline(resolvedOrgId, hours, interval);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/type-distribution")
    public ResponseEntity<List<TypeDistributionDto>> typeDistribution(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        List<TypeDistributionDto> result = analyticsService.getTypeDistribution(resolvedOrgId, hours);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/latency-percentiles")
    public ResponseEntity<LatencyPercentilesDto> latencyPercentiles(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        LatencyPercentilesDto result = analyticsService.getLatencyPercentiles(resolvedOrgId, hours);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pass-fail")
    public ResponseEntity<List<PassFailPointDto>> passFail(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "1h") String interval) {

        String resolvedOrgId = (orgId != null) ? orgId : TenantContext.get().orgId().toString();
        List<PassFailPointDto> result = analyticsService.getPassFailTimeline(resolvedOrgId, hours, interval);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ab-tests/{testId}/results")
    public ResponseEntity<AbTestResultsDto> abTestResults(@PathVariable String testId) {
        AbTestResultsDto result = analyticsService.getAbTestResults(testId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ab-tests/{testId}/significance")
    public ResponseEntity<SignificanceDto> abTestSignificance(@PathVariable String testId) {
        SignificanceDto result = analyticsService.getAbTestSignificance(testId);
        return ResponseEntity.ok(result);
    }
}
