package io.ruleforge.analytics.service;

import io.ruleforge.analytics.dto.DashboardMetricsDto;
import io.ruleforge.analytics.dto.DecisionLogDto;
import io.ruleforge.analytics.dto.DecisionQueryRequest;
import io.ruleforge.analytics.dto.RuleStatsDto;
import io.ruleforge.analytics.model.DecisionLogDocument;
import io.ruleforge.analytics.repository.DecisionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for querying decision logs from Elasticsearch and computing
 * aggregated analytics metrics.
 */
@Service
public class AnalyticsService {

    private final DecisionLogRepository decisionLogRepository;

    public AnalyticsService(DecisionLogRepository decisionLogRepository) {
        this.decisionLogRepository = decisionLogRepository;
    }

    /**
     * Query decision logs with optional orgId/ruleId and time-range filters.
     * Returns a paginated result of lightweight {@link DecisionLogDto} records.
     */
    public Page<DecisionLogDto> queryDecisions(DecisionQueryRequest request) {
        Instant from = request.from() != null ? request.from() : Instant.EPOCH;
        Instant to = request.to() != null ? request.to() : Instant.now();
        int page = Math.max(0, request.page());
        int size = request.size() > 0 ? request.size() : 20;
        Pageable pageable = PageRequest.of(page, size);

        List<DecisionLogDocument> docs;

        if (request.ruleId() != null && !request.ruleId().isBlank()) {
            docs = decisionLogRepository.findByRuleIdAndTimestampBetween(
                    request.ruleId(), from, to, pageable);
        } else if (request.orgId() != null && !request.orgId().isBlank()) {
            docs = decisionLogRepository.findByOrgIdAndTimestampBetween(
                    request.orgId(), from, to, pageable);
        } else {
            docs = List.of();
        }

        List<DecisionLogDto> dtos = docs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    /**
     * Compute aggregated statistics for a specific rule over the past {@code days} days.
     */
    public RuleStatsDto getRuleStats(String ruleId, int days) {
        int effectiveDays = days > 0 ? days : 30;
        Instant from = Instant.now().minus(effectiveDays, ChronoUnit.DAYS);
        Instant to = Instant.now();

        List<DecisionLogDocument> docs = decisionLogRepository.findByRuleIdAndTimestampBetween(
                ruleId, from, to, PageRequest.of(0, 10000));

        long total = docs.size();
        double avgExecTime = docs.stream()
                .mapToLong(DecisionLogDocument::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        long errorCount = docs.stream()
                .filter(d -> "ERROR".equalsIgnoreCase(d.getStatus()))
                .count();
        double errorRate = total > 0 ? (double) errorCount / total : 0.0;

        Instant lastExecuted = docs.stream()
                .map(DecisionLogDocument::getTimestamp)
                .filter(t -> t != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        String ruleName = docs.stream()
                .map(DecisionLogDocument::getRuleName)
                .filter(n -> n != null)
                .findFirst()
                .orElse(ruleId);

        return new RuleStatsDto(ruleId, ruleName, total, avgExecTime, errorRate, lastExecuted);
    }

    /**
     * Compute dashboard-level metrics for an organization over the past {@code hours} hours.
     * Includes total decisions, throughput, average execution time, error rate, and top rules.
     */
    public DashboardMetricsDto getDashboardMetrics(String orgId, int hours) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);
        Instant to = Instant.now();

        List<DecisionLogDocument> docs = decisionLogRepository.findByOrgIdAndTimestampBetween(
                orgId, from, to, PageRequest.of(0, 10000));

        long total = docs.size();

        double avgExecTime = docs.stream()
                .mapToLong(DecisionLogDocument::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        long errorCount = docs.stream()
                .filter(d -> "ERROR".equalsIgnoreCase(d.getStatus()))
                .count();
        double errorRate = total > 0 ? (double) errorCount / total : 0.0;

        double windowMinutes = effectiveHours * 60.0;
        double decisionsPerMinute = windowMinutes > 0 ? total / windowMinutes : 0.0;

        // Group by ruleId and compute per-rule stats for the top 10 most-executed rules
        Map<String, List<DecisionLogDocument>> byRule = docs.stream()
                .collect(Collectors.groupingBy(d ->
                        d.getRuleId() != null ? d.getRuleId() : "unknown"));

        List<RuleStatsDto> topRules = byRule.entrySet().stream()
                .map(entry -> {
                    String ruleId = entry.getKey();
                    List<DecisionLogDocument> ruleDocs = entry.getValue();
                    long ruleTotal = ruleDocs.size();
                    double ruleAvg = ruleDocs.stream()
                            .mapToLong(DecisionLogDocument::getExecutionTimeMs)
                            .average().orElse(0.0);
                    long ruleErrors = ruleDocs.stream()
                            .filter(d -> "ERROR".equalsIgnoreCase(d.getStatus()))
                            .count();
                    double ruleErrorRate = ruleTotal > 0 ? (double) ruleErrors / ruleTotal : 0.0;
                    Instant lastExec = ruleDocs.stream()
                            .map(DecisionLogDocument::getTimestamp)
                            .filter(t -> t != null)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    String ruleName = ruleDocs.stream()
                            .map(DecisionLogDocument::getRuleName)
                            .filter(n -> n != null)
                            .findFirst().orElse(ruleId);
                    return new RuleStatsDto(ruleId, ruleName, ruleTotal, ruleAvg,
                            ruleErrorRate, lastExec);
                })
                .sorted(Comparator.comparingLong(RuleStatsDto::totalExecutions).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new DashboardMetricsDto(total, decisionsPerMinute, avgExecTime, errorRate, topRules);
    }

    private DecisionLogDto toDto(DecisionLogDocument doc) {
        return new DecisionLogDto(
                doc.getId(),
                doc.getRuleId(),
                doc.getRuleName(),
                doc.getRuleType(),
                doc.getOrgId(),
                doc.getExecutionTimeMs(),
                doc.getStatus(),
                doc.getTimestamp(),
                doc.getCorrelationId()
        );
    }
}
