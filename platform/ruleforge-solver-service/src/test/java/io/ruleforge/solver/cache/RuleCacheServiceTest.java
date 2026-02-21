package io.ruleforge.solver.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.solver.service.RuleLoaderService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleCacheServiceTest {

    @Mock private RuleLoaderService ruleLoaderService;
    private Cache<String, CompiledRule> cache;
    private RuleCacheService cacheService;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().maximumSize(100).build();
        cacheService = new RuleCacheService(cache, ruleLoaderService);
        TenantContext.set(new TenantContext.TenantInfo(orgId, spaceId, userId, "SOLVER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getCompiledRule_cacheMiss_loadsFromService() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule mockRule = mock(CompiledRule.class);
        when(mockRule.ruleId()).thenReturn(ruleId);
        when(ruleLoaderService.loadRule(ruleId, 1)).thenReturn(mockRule);

        CompiledRule result = cacheService.getCompiledRule(ruleId, 1);

        assertSame(mockRule, result);
        verify(ruleLoaderService).loadRule(ruleId, 1);
    }

    @Test
    void getCompiledRule_cacheHit_doesNotLoadFromService() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule mockRule = mock(CompiledRule.class);
        when(mockRule.ruleId()).thenReturn(ruleId);
        when(ruleLoaderService.loadRule(ruleId, 1)).thenReturn(mockRule);

        // First call: cache miss
        cacheService.getCompiledRule(ruleId, 1);

        // Second call: cache hit
        CompiledRule result = cacheService.getCompiledRule(ruleId, 1);

        assertSame(mockRule, result);
        // Should only load once
        verify(ruleLoaderService, times(1)).loadRule(ruleId, 1);
    }

    @Test
    void invalidate_removesFromCache() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule mockRule = mock(CompiledRule.class);
        when(mockRule.ruleId()).thenReturn(ruleId);
        when(ruleLoaderService.loadRule(ruleId, 1)).thenReturn(mockRule);

        // Load into cache
        cacheService.getCompiledRule(ruleId, 1);

        // Invalidate
        cacheService.invalidate(ruleId, 1);

        // Next call should reload
        cacheService.getCompiledRule(ruleId, 1);
        verify(ruleLoaderService, times(2)).loadRule(ruleId, 1);
    }

    @Test
    void invalidateAll_clearsEntireCache() {
        String ruleId1 = UUID.randomUUID().toString();
        String ruleId2 = UUID.randomUUID().toString();

        CompiledRule mockRule1 = mock(CompiledRule.class);
        when(mockRule1.ruleId()).thenReturn(ruleId1);
        when(ruleLoaderService.loadRule(ruleId1, 1)).thenReturn(mockRule1);

        CompiledRule mockRule2 = mock(CompiledRule.class);
        when(mockRule2.ruleId()).thenReturn(ruleId2);
        when(ruleLoaderService.loadRule(ruleId2, 1)).thenReturn(mockRule2);

        cacheService.getCompiledRule(ruleId1, 1);
        cacheService.getCompiledRule(ruleId2, 1);

        cacheService.invalidateAll(ruleId1);

        // Both should need reloading
        cacheService.getCompiledRule(ruleId1, 1);
        cacheService.getCompiledRule(ruleId2, 1);

        verify(ruleLoaderService, times(2)).loadRule(ruleId1, 1);
        verify(ruleLoaderService, times(2)).loadRule(ruleId2, 1);
    }

    @Test
    void getCompiledRule_nullVersion_usesLatestKey() {
        String ruleId = UUID.randomUUID().toString();
        CompiledRule mockRule = mock(CompiledRule.class);
        when(mockRule.ruleId()).thenReturn(ruleId);
        when(ruleLoaderService.loadRule(ruleId, null)).thenReturn(mockRule);

        CompiledRule result = cacheService.getCompiledRule(ruleId, null);

        assertSame(mockRule, result);
        verify(ruleLoaderService).loadRule(ruleId, null);
    }
}
