package io.ruleforge.analytics.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
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
import io.ruleforge.analytics.dto.VariantStatsDto;
import io.ruleforge.analytics.model.DecisionLogDocument;
import io.ruleforge.analytics.repository.DecisionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final DecisionLogRepository decisionLogRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public AnalyticsService(DecisionLogRepository decisionLogRepository,
                            ElasticsearchOperations elasticsearchOperations) {
        this.decisionLogRepository = decisionLogRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public Page<DecisionLogDto> queryDecisions(DecisionQueryRequest request) {
        Instant from = request.from() != null ? request.from() : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant to = request.to() != null ? request.to() : Instant.now();
        int page = Math.max(0, request.page());
        int size = request.size() > 0 ? request.size() : 20;
        Pageable pageable = PageRequest.of(page, size);

        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.must(m -> m.term(t -> t.field("orgId").value(request.orgId())));
            boolBuilder.must(m -> m.range(r -> r.untyped(u -> u.field("timestamp")
                    .gte(JsonData.of(from.toString()))
                    .lte(JsonData.of(to.toString())))));

            if (request.ruleId() != null && !request.ruleId().isBlank()) {
                boolBuilder.must(m -> m.term(t -> t.field("ruleId").value(request.ruleId())));
            }
            if (request.status() != null && !request.status().isBlank()) {
                boolBuilder.must(m -> m.term(t -> t.field("status").value(request.status())));
            }
            if (request.search() != null && !request.search().isBlank()) {
                boolBuilder.must(m -> m.match(mt -> mt.field("ruleName").query(request.search())));
            }
            if (request.minLatency() != null) {
                boolBuilder.must(m -> m.range(r -> r.untyped(u -> u.field("executionTimeMs").gte(JsonData.of(request.minLatency())))));
            }
            if (request.maxLatency() != null) {
                boolBuilder.must(m -> m.range(r -> r.untyped(u -> u.field("executionTimeMs").lte(JsonData.of(request.maxLatency())))));
            }

            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                    .withPageable(pageable)
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            List<DecisionLogDto> dtos = hits.getSearchHits().stream()
                    .map(h -> toDto(h.getContent()))
                    .collect(Collectors.toList());
            long total = hits.getTotalHits();
            return new PageImpl<>(dtos, pageable, total);
        } catch (Exception e) {
            log.warn("Analytics queryDecisions failed (Elasticsearch unavailable?): {}", e.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    public RuleStatsDto getRuleStats(String ruleId, int days) {
        int effectiveDays = days > 0 ? days : 30;
        Instant from = Instant.now().minus(effectiveDays, ChronoUnit.DAYS);
        Instant to = Instant.now();

        try {
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
        } catch (Exception e) {
            log.warn("Analytics getRuleStats failed for ruleId={}: {}", ruleId, e.getMessage());
            return new RuleStatsDto(ruleId, ruleId, 0, 0.0, 0.0, null);
        }
    }

    /**
     * Dashboard metrics using ES aggregations instead of loading all documents into memory.
     */
    public DashboardMetricsDto getDashboardMetrics(String orgId, int hours) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);

        try {
            Query baseQuery = buildOrgTimeRangeQuery(orgId, from);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("avg_exec_time", Aggregation.of(a -> a.avg(avg -> avg.field("executionTimeMs"))))
                    .withAggregation("error_count", Aggregation.of(a -> a.filter(f ->
                            f.term(t -> t.field("status").value("ERROR")))))
                    .withAggregation("top_rules", Aggregation.of(a -> a.terms(t -> t.field("ruleId").size(10))
                            .aggregations("avg_latency", Aggregation.of(sa -> sa.avg(avg -> avg.field("executionTimeMs"))))
                            .aggregations("errors", Aggregation.of(sa -> sa.filter(f ->
                                    f.term(t2 -> t2.field("status").value("ERROR")))))
                            .aggregations("last_exec", Aggregation.of(sa -> sa.max(m -> m.field("timestamp"))))
                            .aggregations("rule_name", Aggregation.of(sa -> sa.terms(t -> t.field("ruleName").size(1))))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) {
                return new DashboardMetricsDto(0, 0.0, 0.0, 0.0, List.of());
            }

            long total = hits.getTotalHits();
            double avgExecTime = getAvgValue(aggs, "avg_exec_time");
            long errorCount = getFilterDocCount(aggs, "error_count");
            double errorRate = total > 0 ? (double) errorCount / total : 0.0;
            double windowMinutes = effectiveHours * 60.0;
            double decisionsPerMinute = windowMinutes > 0 ? total / windowMinutes : 0.0;

            List<RuleStatsDto> topRules = new ArrayList<>();
            var topRulesAgg = aggs.get("top_rules");
            if (topRulesAgg != null) {
                var buckets = topRulesAgg.aggregation().getAggregate().sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    String ruleId = bucket.key().stringValue();
                    long ruleTotal = bucket.docCount();
                    double ruleAvg = bucket.aggregations().get("avg_latency").avg().value();
                    long ruleErrors = bucket.aggregations().get("errors").filter().docCount();
                    double ruleErrorRate = ruleTotal > 0 ? (double) ruleErrors / ruleTotal : 0.0;
                    String maxTs = bucket.aggregations().get("last_exec").max().valueAsString();
                    Instant lastExec = maxTs != null ? Instant.parse(maxTs) : null;
                    // Extract ruleName from the nested terms aggregation on the keyword field
                    String ruleName = ruleId;
                    var ruleNameAgg = bucket.aggregations().get("rule_name");
                    if (ruleNameAgg != null) {
                        var nameBuckets = ruleNameAgg.sterms().buckets().array();
                        if (!nameBuckets.isEmpty()) {
                            ruleName = nameBuckets.get(0).key().stringValue();
                        }
                    }
                    topRules.add(new RuleStatsDto(ruleId, ruleName, ruleTotal, ruleAvg, ruleErrorRate, lastExec));
                }
            }

            return new DashboardMetricsDto(total, decisionsPerMinute, avgExecTime, errorRate, topRules);
        } catch (Exception e) {
            log.warn("Analytics getDashboardMetrics failed (Elasticsearch unavailable?): {}", e.getMessage());
            return new DashboardMetricsDto(0, 0.0, 0.0, 0.0, List.of());
        }
    }

    /**
     * Timeline: date_histogram aggregation on timestamp with error sub-count.
     */
    public List<TimelinePointDto> getTimeline(String orgId, int hours, String interval) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);
        CalendarInterval calendarInterval = parseCalendarInterval(interval);

        try {
            Query baseQuery = buildOrgTimeRangeQuery(orgId, from);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("timeline", Aggregation.of(a -> a
                            .dateHistogram(dh -> dh.field("timestamp").calendarInterval(calendarInterval))
                            .aggregations("errors", Aggregation.of(sa -> sa
                                    .filter(f -> f.term(t -> t.field("status").value("ERROR")))))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return List.of();

            List<TimelinePointDto> result = new ArrayList<>();
            var buckets = aggs.get("timeline").aggregation().getAggregate().dateHistogram().buckets().array();
            for (DateHistogramBucket bucket : buckets) {
                Instant timestamp = Instant.ofEpochMilli(bucket.key());
                long count = bucket.docCount();
                long errorCount = bucket.aggregations().get("errors").filter().docCount();
                result.add(new TimelinePointDto(timestamp, count, errorCount));
            }
            return result;
        } catch (Exception e) {
            log.warn("Analytics getTimeline failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Type distribution: terms aggregation on ruleType keyword field.
     */
    public List<TypeDistributionDto> getTypeDistribution(String orgId, int hours) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);

        try {
            Query baseQuery = buildOrgTimeRangeQuery(orgId, from);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("type_dist", Aggregation.of(a -> a
                            .terms(t -> t.field("ruleType").size(50))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return List.of();

            long totalHits = hits.getTotalHits();
            List<TypeDistributionDto> result = new ArrayList<>();
            var buckets = aggs.get("type_dist").aggregation().getAggregate().sterms().buckets().array();
            for (StringTermsBucket bucket : buckets) {
                String ruleType = bucket.key().stringValue();
                long count = bucket.docCount();
                double percentage = totalHits > 0 ? (double) count / totalHits * 100.0 : 0.0;
                result.add(new TypeDistributionDto(ruleType, count, percentage));
            }
            return result;
        } catch (Exception e) {
            log.warn("Analytics getTypeDistribution failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Latency percentiles: percentiles aggregation on executionTimeMs.
     */
    public LatencyPercentilesDto getLatencyPercentiles(String orgId, int hours) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);

        try {
            Query baseQuery = buildOrgTimeRangeQuery(orgId, from);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("latency_pct", Aggregation.of(a -> a
                            .percentiles(p -> p.field("executionTimeMs")
                                    .percents(50.0, 75.0, 90.0, 95.0, 99.0))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return new LatencyPercentilesDto(0, 0, 0, 0, 0);

            var percentiles = aggs.get("latency_pct").aggregation().getAggregate()
                    .tdigestPercentiles().values().keyed();
            return new LatencyPercentilesDto(
                    safePercentile(percentiles, "50.0"),
                    safePercentile(percentiles, "75.0"),
                    safePercentile(percentiles, "90.0"),
                    safePercentile(percentiles, "95.0"),
                    safePercentile(percentiles, "99.0")
            );
        } catch (Exception e) {
            log.warn("Analytics getLatencyPercentiles failed: {}", e.getMessage());
            return new LatencyPercentilesDto(0, 0, 0, 0, 0);
        }
    }

    /**
     * Pass/fail timeline: date_histogram with filters sub-aggregation for PASS vs FAIL/ERROR.
     */
    public List<PassFailPointDto> getPassFailTimeline(String orgId, int hours, String interval) {
        int effectiveHours = hours > 0 ? hours : 24;
        Instant from = Instant.now().minus(effectiveHours, ChronoUnit.HOURS);
        CalendarInterval calendarInterval = parseCalendarInterval(interval);

        try {
            Query baseQuery = buildOrgTimeRangeQuery(orgId, from);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("passfail", Aggregation.of(a -> a
                            .dateHistogram(dh -> dh.field("timestamp").calendarInterval(calendarInterval))
                            .aggregations("outcomes", Aggregation.of(sa -> sa
                                    .filters(f -> f.filters(fb -> fb.keyed(Map.of(
                                            "pass", Query.of(q -> q.bool(b -> b.mustNot(
                                                    mn -> mn.term(t -> t.field("status").value("ERROR"))))),
                                            "fail", Query.of(q -> q.term(t -> t.field("status").value("ERROR")))
                                    ))))))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return List.of();

            List<PassFailPointDto> result = new ArrayList<>();
            var buckets = aggs.get("passfail").aggregation().getAggregate().dateHistogram().buckets().array();
            for (DateHistogramBucket bucket : buckets) {
                Instant timestamp = Instant.ofEpochMilli(bucket.key());
                Map<String, FiltersBucket> outcomes = bucket.aggregations().get("outcomes")
                        .filters().buckets().keyed();
                long passCount = outcomes.containsKey("pass") ? outcomes.get("pass").docCount() : 0;
                long failCount = outcomes.containsKey("fail") ? outcomes.get("fail").docCount() : 0;
                result.add(new PassFailPointDto(timestamp, passCount, failCount));
            }
            return result;
        } catch (Exception e) {
            log.warn("Analytics getPassFailTimeline failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * A/B test results: aggregate by abTestVersion for a given testId.
     */
    public AbTestResultsDto getAbTestResults(String testId) {
        try {
            Query baseQuery = Query.of(q -> q.term(t -> t.field("abTestId").value(testId)));

            NativeQuery query = NativeQuery.builder()
                    .withQuery(baseQuery)
                    .withMaxResults(0)
                    .withAggregation("variants", Aggregation.of(a -> a
                            .terms(t -> t.field("abTestVersion").size(2))
                            .aggregations("errors", Aggregation.of(sa -> sa
                                    .filter(f -> f.term(t -> t.field("status").value("ERROR")))))
                            .aggregations("avg_latency", Aggregation.of(sa -> sa
                                    .avg(avg -> avg.field("executionTimeMs"))))
                            .aggregations("p95_latency", Aggregation.of(sa -> sa
                                    .percentiles(p -> p.field("executionTimeMs").percents(95.0))))))
                    .build();

            SearchHits<DecisionLogDocument> hits = elasticsearchOperations.search(query, DecisionLogDocument.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

            VariantStatsDto statsA = new VariantStatsDto("A", 0, 0, 0, 0, 0);
            VariantStatsDto statsB = new VariantStatsDto("B", 0, 0, 0, 0, 0);

            if (aggs != null) {
                var buckets = aggs.get("variants").aggregation().getAggregate().sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    String variant = bucket.key().stringValue();
                    long executions = bucket.docCount();
                    long errors = bucket.aggregations().get("errors").filter().docCount();
                    double errorRate = executions > 0 ? (double) errors / executions : 0;
                    double avgLatency = bucket.aggregations().get("avg_latency").avg().value();
                    var p95map = bucket.aggregations().get("p95_latency").tdigestPercentiles().values().keyed();
                    double p95 = safePercentile(p95map, "95.0");

                    VariantStatsDto stats = new VariantStatsDto(variant, executions, errors, errorRate, avgLatency, p95);
                    if ("A".equals(variant)) statsA = stats;
                    else if ("B".equals(variant)) statsB = stats;
                }
            }

            return new AbTestResultsDto(testId, statsA, statsB, statsA.executions() + statsB.executions());
        } catch (Exception e) {
            log.warn("Analytics getAbTestResults failed for testId={}: {}", testId, e.getMessage());
            VariantStatsDto empty = new VariantStatsDto("A", 0, 0, 0, 0, 0);
            return new AbTestResultsDto(testId, empty, new VariantStatsDto("B", 0, 0, 0, 0, 0), 0);
        }
    }

    /**
     * A/B test significance: chi-square for error rates, z-test for latency.
     */
    public SignificanceDto getAbTestSignificance(String testId) {
        AbTestResultsDto results = getAbTestResults(testId);
        long nA = results.versionA().executions();
        long nB = results.versionB().executions();
        int minSamples = 1000;

        if (nA < minSamples || nB < minSamples) {
            int needed = (int) Math.max(minSamples - nA, minSamples - nB);
            return new SignificanceDto("INCONCLUSIVE", 0, 1.0, false, needed);
        }

        // Chi-square test for error rates
        double pA = results.versionA().errorRate();
        double pB = results.versionB().errorRate();
        double pPooled = (double) (results.versionA().errors() + results.versionB().errors()) / (nA + nB);

        double chiSquare = 0;
        if (pPooled > 0 && pPooled < 1) {
            double se = Math.sqrt(pPooled * (1 - pPooled) * (1.0 / nA + 1.0 / nB));
            double z = (pA - pB) / se;
            chiSquare = z * z;
        }

        // Z-test for latency means (simplified, using avg only)
        double avgA = results.versionA().avgLatencyMs();
        double avgB = results.versionB().avgLatencyMs();

        // Determine winner based on lower error rate and lower latency
        double pValue = chiSquarePValue(chiSquare, 1);
        double confidence = 1.0 - pValue;

        String winner;
        if (pValue > 0.05) {
            winner = "INCONCLUSIVE";
        } else if (pA < pB || (pA == pB && avgA < avgB)) {
            winner = "A";
        } else {
            winner = "B";
        }

        return new SignificanceDto(winner, confidence, pValue, true, 0);
    }

    /**
     * Approximate chi-square p-value for 1 degree of freedom using the normal approximation.
     */
    private double chiSquarePValue(double chiSquare, int df) {
        if (chiSquare <= 0) return 1.0;
        // Approximation: for df=1, chi-square ~ N(0,1)^2, so p = 2*(1-Phi(sqrt(chi2)))
        double z = Math.sqrt(chiSquare);
        return 2.0 * (1.0 - normalCdf(z));
    }

    private double normalCdf(double z) {
        // Abramowitz and Stegun approximation
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double d = 0.3989422804014327; // 1/sqrt(2*pi)
        double p = d * Math.exp(-z * z / 2.0) *
                (t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429)))));
        return z > 0 ? 1.0 - p : p;
    }

    // --- Helpers ---

    private Query buildOrgTimeRangeQuery(String orgId, Instant from) {
        return Query.of(q -> q.bool(BoolQuery.of(b -> b
                .must(m -> m.term(t -> t.field("orgId").value(orgId)))
                .must(m -> m.range(r -> r.untyped(u -> u.field("timestamp")
                        .gte(JsonData.of(from.toString()))
                        .lte(JsonData.of("now"))))))));
    }

    private CalendarInterval parseCalendarInterval(String interval) {
        if (interval == null || interval.isBlank()) return CalendarInterval.Hour;
        return switch (interval) {
            case "1m", "minute" -> CalendarInterval.Minute;
            case "1h", "hour" -> CalendarInterval.Hour;
            case "1d", "day" -> CalendarInterval.Day;
            case "1w", "week" -> CalendarInterval.Week;
            case "1M", "month" -> CalendarInterval.Month;
            default -> CalendarInterval.Hour;
        };
    }

    private double getAvgValue(ElasticsearchAggregations aggs, String name) {
        var agg = aggs.get(name);
        if (agg == null) return 0.0;
        return agg.aggregation().getAggregate().avg().value();
    }

    private long getFilterDocCount(ElasticsearchAggregations aggs, String name) {
        var agg = aggs.get(name);
        if (agg == null) return 0;
        return agg.aggregation().getAggregate().filter().docCount();
    }

    private double safePercentile(Map<String, String> keyed, String key) {
        String val = keyed.get(key);
        if (val == null || "NaN".equals(val)) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
