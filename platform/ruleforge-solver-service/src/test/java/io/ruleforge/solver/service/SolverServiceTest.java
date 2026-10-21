package io.ruleforge.solver.service;

import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.common.tenant.TenantScopeExtension;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.solver.cache.RuleCacheService;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.solver.dto.BulkSolveRequest;
import io.ruleforge.solver.dto.SolveRequest;
import io.ruleforge.solver.entity.SolverRuleEntity;
import io.ruleforge.solver.dto.SolveResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(TenantScopeExtension.class)
class SolverServiceTest implements TenantScopeExtension.TenantProvider {

    private TenantContext.TenantInfo tenant;

    @Override
    public TenantContext.TenantInfo tenant() {
        return tenant;
    }

    @Mock private RuleCacheService cacheService;
    @Mock private RuleForgeEngine engine;
    @Mock private EventPublisher eventPublisher;
    @Mock private AbTestRouter abTestRouter;
    @Mock private RuleLoaderService ruleLoaderService;

    private SolverService solverService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        solverService = new SolverService(cacheService, engine, eventPublisher, abTestRouter, ruleLoaderService);
        lenient().when(abTestRouter.route(any())).thenReturn(Optional.empty());
        SolverRuleEntity activeRule = ruleWithStatus("PUBLISHED");
        lenient().when(ruleLoaderService.resolveRule(any())).thenReturn(activeRule);
        tenant = new TenantContext.TenantInfo(orgId, spaceId, userId, "SOLVER");
    }


    @Test
    void solve_returnsEngineOutput() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule compiledRule = mock(CompiledRule.class);
        when(compiledRule.ruleId()).thenReturn(ruleId);
        when(compiledRule.type()).thenReturn(RuleType.DECISION_TABLE);
        when(compiledRule.metadata()).thenReturn(RuleMetadata.defaults());
        when(cacheService.getCompiledRule(ruleId, null)).thenReturn(compiledRule);

        RuleResult engineResult = new RuleResult(
                Map.of("risk", "LOW"),
                new RuleResultMetadata(Duration.ofMillis(3), 5, 1, 0, "FIRST_MATCH"),
                List.of(),
                null
        );
        when(engine.evaluate(any(CompiledRule.class), any(WorkingMemory.class))).thenReturn(engineResult);

        SolveRequest request = new SolveRequest(Map.of("age", 30), null, null);
        SolveResponse response = solverService.solve(ruleId, null, request);

        assertNotNull(response);
        assertEquals("LOW", response.output().get("risk"));
        assertEquals(ruleId, response.metadata().ruleId());
        assertEquals("DECISION_TABLE", response.metadata().ruleType());
    }

    @Test
    void solve_publishesExecutionEvent() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule compiledRule = mock(CompiledRule.class);
        when(compiledRule.ruleId()).thenReturn(ruleId);
        when(compiledRule.type()).thenReturn(RuleType.DECISION_TABLE);
        when(compiledRule.metadata()).thenReturn(RuleMetadata.defaults());
        when(cacheService.getCompiledRule(ruleId, null)).thenReturn(compiledRule);

        RuleResult engineResult = RuleResult.empty();
        when(engine.evaluate(any(CompiledRule.class), any(WorkingMemory.class))).thenReturn(engineResult);

        SolveRequest request = new SolveRequest(Map.of("key", "value"), null, null);
        solverService.solve(ruleId, null, request);

        verify(eventPublisher).publish(eq("rule.execution"), any());
    }

    @Test
    void solve_withSpecificVersion_passesVersionToCache() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule compiledRule = mock(CompiledRule.class);
        when(compiledRule.ruleId()).thenReturn(ruleId);
        when(compiledRule.type()).thenReturn(RuleType.DECISION_TREE);
        when(compiledRule.metadata()).thenReturn(RuleMetadata.defaults());
        when(cacheService.getCompiledRule(ruleId, 3)).thenReturn(compiledRule);

        RuleResult engineResult = RuleResult.empty();
        when(engine.evaluate(any(CompiledRule.class), any(WorkingMemory.class))).thenReturn(engineResult);

        SolveRequest request = new SolveRequest(Map.of("x", 1), null, null);
        solverService.solve(ruleId, 3, request);

        verify(cacheService).getCompiledRule(ruleId, 3);
    }

    @Test
    void solve_includesMatchedRulesInResponse() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule compiledRule = mock(CompiledRule.class);
        when(compiledRule.ruleId()).thenReturn(ruleId);
        when(compiledRule.type()).thenReturn(RuleType.DECISION_TABLE);
        when(compiledRule.metadata()).thenReturn(RuleMetadata.defaults());
        when(cacheService.getCompiledRule(ruleId, null)).thenReturn(compiledRule);

        MatchedRule matchedRule = new MatchedRule(
                "rule-1", "Rule One", 1, null,
                Map.of("age", 30), Map.of("tier", "GOLD")
        );
        RuleResult engineResult = new RuleResult(
                Map.of("tier", "GOLD"),
                new RuleResultMetadata(Duration.ofMillis(2), 3, 1, 0, "FIRST_MATCH"),
                List.of(matchedRule),
                null
        );
        when(engine.evaluate(any(CompiledRule.class), any(WorkingMemory.class))).thenReturn(engineResult);

        SolveRequest request = new SolveRequest(Map.of("age", 30), null, null);
        SolveResponse response = solverService.solve(ruleId, null, request);

        assertEquals(1, response.matchedRules().size());
        assertEquals("rule-1", response.matchedRules().get(0).ruleId());
    }

    @Test
    void solve_refusesArchivedRuleEvenWhenCompiledRuleIsCached() {
        String ruleId = UUID.randomUUID().toString();
        SolverRuleEntity archived = ruleWithStatus("ARCHIVED");
        when(ruleLoaderService.resolveRule(ruleId)).thenReturn(archived);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> solverService.solve(ruleId, null, new SolveRequest(Map.of("age", 30), null, null)));

        assertTrue(ex.getMessage().contains("archived"));
        verifyNoInteractions(cacheService, engine);
    }

    @Test
    void solveBulk_refusesArchivedRule() {
        String ruleId = UUID.randomUUID().toString();
        SolverRuleEntity archived = ruleWithStatus("ARCHIVED");
        when(ruleLoaderService.resolveRule(ruleId)).thenReturn(archived);

        assertThrows(ConflictException.class, () -> solverService.solveBulk(ruleId, null,
                new BulkSolveRequest(List.of(Map.of("age", 30)), null, null)));
        verifyNoInteractions(cacheService, engine);
    }

    @Test
    void solve_refusesDeletedRule() {
        String ruleId = UUID.randomUUID().toString();
        SolverRuleEntity deleted = ruleWithStatus("DELETED");
        when(ruleLoaderService.resolveRule(ruleId)).thenReturn(deleted);

        assertThrows(ConflictException.class,
                () -> solverService.solve(ruleId, null, new SolveRequest(Map.of(), null, null)));
        verifyNoInteractions(cacheService);
    }

    @Test
    void solveBulk_evaluatesEachInputWithTheCallersTenantBound() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule compiledRule = mock(CompiledRule.class);
        lenient().when(compiledRule.ruleId()).thenReturn(ruleId);
        lenient().when(compiledRule.type()).thenReturn(RuleType.DECISION_TABLE);
        lenient().when(compiledRule.metadata()).thenReturn(RuleMetadata.defaults());
        when(cacheService.getCompiledRule(ruleId, null)).thenReturn(compiledRule);

        java.util.Set<TenantContext.TenantInfo> tenantsSeen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Set<Boolean> virtualThreads = java.util.concurrent.ConcurrentHashMap.newKeySet();
        when(engine.evaluate(any(CompiledRule.class), any(WorkingMemory.class))).thenAnswer(inv -> {
            tenantsSeen.add(TenantContext.get());
            virtualThreads.add(Thread.currentThread().isVirtual());
            return new RuleResult(Map.of(), new RuleResultMetadata(Duration.ofMillis(1), 1, 1, 0, "FIRST_MATCH"),
                    List.of(), null);
        });

        solverService.solveBulk(ruleId, null,
                new BulkSolveRequest(List.of(Map.of("a", 1), Map.of("a", 2), Map.of("a", 3)), null, null));

        assertEquals(java.util.Set.of(tenant), tenantsSeen);
        assertEquals(java.util.Set.of(true), virtualThreads);
    }

    private SolverRuleEntity ruleWithStatus(String status) {
        SolverRuleEntity rule = mock(SolverRuleEntity.class);
        lenient().when(rule.getStatus()).thenReturn(status);
        return rule;
    }
}
