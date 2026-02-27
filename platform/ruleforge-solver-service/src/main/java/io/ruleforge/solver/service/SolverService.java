package io.ruleforge.solver.service;

import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.solver.cache.RuleCacheService;
import io.ruleforge.solver.dto.BulkSolveRequest;
import io.ruleforge.solver.dto.BulkSolveResponse;
import io.ruleforge.solver.dto.BulkSolveResponse.BulkMetadata;
import io.ruleforge.solver.dto.DecisionTraceDto;
import io.ruleforge.solver.dto.MatchedRuleDto;
import io.ruleforge.solver.dto.SolveMetadata;
import io.ruleforge.solver.dto.SolveRequest;
import io.ruleforge.solver.dto.SolveResponse;
import io.ruleforge.solver.service.AbTestRouter.AbTestAssignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core service for rule evaluation. Orchestrates loading compiled rules from cache,
 * evaluating them against input facts, publishing execution events, and returning
 * structured results.
 */
@Service
public class SolverService {

    private static final Logger log = LoggerFactory.getLogger(SolverService.class);
    private static final String EXECUTION_TOPIC = "rule.execution";

    private final RuleCacheService cacheService;
    private final RuleForgeEngine engine;
    private final EventPublisher eventPublisher;
    private final AbTestRouter abTestRouter;

    public SolverService(RuleCacheService cacheService,
                         RuleForgeEngine engine,
                         EventPublisher eventPublisher,
                         AbTestRouter abTestRouter) {
        this.cacheService = cacheService;
        this.engine = engine;
        this.eventPublisher = eventPublisher;
        this.abTestRouter = abTestRouter;
    }

    /**
     * Evaluates a single rule against the provided input facts.
     *
     * @param ruleIdOrAlias the rule identifier (UUID or alias)
     * @param version       specific version, or null for latest published
     * @param request       the solve request containing input facts
     * @return the solve response with output, metadata, and matched rules
     */
    public SolveResponse solve(String ruleIdOrAlias, Integer version, SolveRequest request) {
        long startNanos = System.nanoTime();

        // 1. Check for active A/B test and route if applicable
        AbTestAssignment abAssignment = null;
        Integer effectiveVersion = version;
        if (version == null) {
            // Only route A/B when no explicit version is requested
            UUID ruleUuid = tryParseUuid(ruleIdOrAlias);
            if (ruleUuid != null) {
                Optional<AbTestAssignment> assignment = abTestRouter.route(ruleUuid);
                if (assignment.isPresent()) {
                    abAssignment = assignment.get();
                    effectiveVersion = abAssignment.versionNumber();
                    log.debug("A/B test routing: testId={}, version={}, variant={}",
                            abAssignment.testId(), abAssignment.versionNumber(), abAssignment.abTestVersion());
                }
            }
        }

        // 2. Load compiled rule (from cache or database)
        CompiledRule compiled = cacheService.getCompiledRule(ruleIdOrAlias, effectiveVersion);

        // 3. Create WorkingMemory from input
        WorkingMemory workingMemory = new WorkingMemory();
        workingMemory.loadFromMap(request.input());

        // 4. Evaluate via engine (dispatches through RuleTypeEngine for proper lifecycle)
        RuleResult result = engine.evaluate(compiled, workingMemory);

        // 5. Calculate elapsed time
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        // 6. Publish execution event asynchronously (with A/B test metadata)
        publishExecutionEvent(compiled, result, elapsedMs, abAssignment);

        // 7. Map to response
        return mapToResponse(compiled, result, elapsedMs);
    }

    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Evaluates a rule against multiple input sets in bulk.
     * The rule is loaded once and evaluated for each input. Evaluation uses
     * virtual threads for parallel processing.
     *
     * @param ruleIdOrAlias the rule identifier (UUID or alias)
     * @param version       specific version, or null for latest published
     * @param request       the bulk solve request containing multiple input sets
     * @return the bulk solve response with individual results and aggregate metadata
     */
    public BulkSolveResponse solveBulk(String ruleIdOrAlias, Integer version, BulkSolveRequest request) {
        long bulkStartNanos = System.nanoTime();

        // Load compiled rule once
        CompiledRule compiled = cacheService.getCompiledRule(ruleIdOrAlias, version);

        // Capture tenant context for virtual threads
        TenantContext.TenantInfo tenantInfo = TenantContext.get();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Evaluate each input using virtual threads for parallelism
        List<CompletableFuture<SolveResponse>> futures = request.inputs().stream()
                .map(input -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Propagate tenant context to virtual thread
                        if (tenantInfo != null) {
                            TenantContext.set(tenantInfo);
                        }

                        long startNanos = System.nanoTime();
                        WorkingMemory workingMemory = new WorkingMemory();
                        workingMemory.loadFromMap(input);
                        RuleResult result = engine.evaluate(compiled, workingMemory);
                        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

                        successCount.incrementAndGet();
                        return mapToResponse(compiled, result, elapsedMs);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.warn("Bulk evaluation failed for one input: {}", e.getMessage());
                        return createErrorResponse(compiled, e);
                    } finally {
                        TenantContext.clear();
                    }
                }))
                .toList();

        // Collect results preserving order
        List<SolveResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long bulkElapsedMs = (System.nanoTime() - bulkStartNanos) / 1_000_000;

        BulkMetadata bulkMetadata = new BulkMetadata(
                request.inputs().size(),
                successCount.get(),
                errorCount.get(),
                bulkElapsedMs
        );

        return new BulkSolveResponse(results, bulkMetadata);
    }

    private SolveResponse mapToResponse(CompiledRule compiled, RuleResult result, long elapsedMs) {
        SolveMetadata metadata = new SolveMetadata(
                compiled.ruleId(),
                compiled.metadata() != null ? compiled.metadata().tags().getOrDefault("name", compiled.ruleId()) : compiled.ruleId(),
                compiled.type().name(),
                0, // version is not directly available on CompiledRule
                elapsedMs,
                result.metadata() != null ? result.metadata().rulesEvaluated() : 0,
                result.matchedRules() != null ? result.matchedRules().size() : 0,
                result.metadata() != null ? result.metadata().strategy() : "NONE"
        );

        List<MatchedRuleDto> matchedRuleDtos = result.matchedRules() != null
                ? result.matchedRules().stream()
                .map(this::mapMatchedRule)
                .toList()
                : List.of();

        DecisionTraceDto traceDto = result.trace() != null
                ? mapTrace(result.trace())
                : null;

        return new SolveResponse(result.output(), metadata, matchedRuleDtos, traceDto);
    }

    private MatchedRuleDto mapMatchedRule(MatchedRule matched) {
        int priority = matched.priority() != null ? matched.priority().salience() : 0;
        return new MatchedRuleDto(
                matched.ruleId(),
                priority,
                matched.producedOutput()
        );
    }

    private DecisionTraceDto mapTrace(DecisionTrace trace) {
        return new DecisionTraceDto(
                trace.ruleId(),
                trace.totalTime() != null ? trace.totalTime().toMillis() : 0,
                List.of() // Trace steps are complex nested structures; simplified for API output
        );
    }

    private SolveResponse createErrorResponse(CompiledRule compiled, Exception e) {
        SolveMetadata metadata = new SolveMetadata(
                compiled.ruleId(),
                compiled.ruleId(),
                compiled.type().name(),
                0,
                0,
                0,
                0,
                "ERROR"
        );
        return new SolveResponse(
                Map.of("error", e.getMessage()),
                metadata,
                List.of(),
                null
        );
    }

    private void publishExecutionEvent(CompiledRule compiled, RuleResult result, long elapsedMs,
                                       AbTestAssignment abAssignment) {
        try {
            TenantContext.TenantInfo tenant = TenantContext.get();
            UUID orgId = tenant != null ? tenant.orgId() : null;
            UUID actorId = tenant != null ? tenant.userId() : null;

            Map<String, Object> payload = new HashMap<>();
            payload.put("ruleType", compiled.type().name());
            payload.put("executionTimeMs", elapsedMs);
            payload.put("rulesEvaluated", result.metadata() != null ? result.metadata().rulesEvaluated() : 0);
            payload.put("rulesMatched", result.matchedRules() != null ? result.matchedRules().size() : 0);
            payload.put("hasOutput", result.hasOutput());

            if (abAssignment != null) {
                payload.put("abTestId", abAssignment.testId().toString());
                payload.put("abTestVersion", abAssignment.abTestVersion());
            }

            DomainEvent event = DomainEvent.create(
                    "rule.executed",
                    "Rule",
                    compiled.ruleId(),
                    orgId,
                    actorId,
                    payload
            );

            eventPublisher.publish(EXECUTION_TOPIC, event);
        } catch (Exception e) {
            log.warn("Failed to publish execution event for rule {}: {}", compiled.ruleId(), e.getMessage());
        }
    }
}
