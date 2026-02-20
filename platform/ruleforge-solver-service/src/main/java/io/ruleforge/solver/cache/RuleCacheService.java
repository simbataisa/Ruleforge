package io.ruleforge.solver.cache;

import com.github.benmanes.caffeine.cache.Cache;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.solver.service.RuleLoaderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * L1 Caffeine cache wrapper for compiled rules.
 * Cache keys are formatted as {@code "spaceId:ruleId:version"} to ensure
 * tenant isolation. On cache miss, delegates to {@link RuleLoaderService}
 * to load and compile the rule from the database.
 */
@Service
public class RuleCacheService {

    private static final Logger log = LoggerFactory.getLogger(RuleCacheService.class);

    private final Cache<String, CompiledRule> compiledRuleCache;
    private final RuleLoaderService ruleLoaderService;

    public RuleCacheService(Cache<String, CompiledRule> compiledRuleCache,
                            RuleLoaderService ruleLoaderService) {
        this.compiledRuleCache = compiledRuleCache;
        this.ruleLoaderService = ruleLoaderService;
    }

    /**
     * Retrieves a compiled rule from the cache, loading it on cache miss.
     *
     * @param ruleIdOrAlias the rule identifier (UUID or alias)
     * @param version       the specific version, or null for latest published
     * @return the compiled rule
     */
    public CompiledRule getCompiledRule(String ruleIdOrAlias, Integer version) {
        String cacheKey = buildCacheKey(ruleIdOrAlias, version);
        CompiledRule cached = compiledRuleCache.getIfPresent(cacheKey);

        if (cached != null) {
            log.debug("Cache HIT for key={}", cacheKey);
            return cached;
        }

        log.debug("Cache MISS for key={}, loading from database", cacheKey);
        CompiledRule compiled = ruleLoaderService.loadRule(ruleIdOrAlias, version);

        // Re-key using the resolved rule ID and actual version for precise cache entries
        String resolvedKey = buildCacheKey(compiled.ruleId(), version);
        compiledRuleCache.put(resolvedKey, compiled);

        // Also cache with the original key if different (alias -> ID mapping)
        if (!resolvedKey.equals(cacheKey)) {
            compiledRuleCache.put(cacheKey, compiled);
        }

        return compiled;
    }

    /**
     * Invalidates a specific version of a rule from the cache.
     *
     * @param ruleId  the rule ID
     * @param version the version to invalidate, or null to invalidate the "latest" entry
     */
    public void invalidate(String ruleId, Integer version) {
        String key = buildCacheKey(ruleId, version);
        compiledRuleCache.invalidate(key);
        log.debug("Invalidated cache key={}", key);
    }

    /**
     * Invalidates all cached versions of a rule by clearing the entire cache.
     * This is a broad invalidation used when a rule is deleted or archived.
     *
     * @param ruleId the rule ID to invalidate
     */
    public void invalidateAll(String ruleId) {
        // Since we cannot iterate Caffeine keys efficiently by prefix,
        // and rule lifecycle events are infrequent, we invalidate the entire cache.
        // The 5-minute TTL and small cache size make this acceptable.
        compiledRuleCache.invalidateAll();
        log.info("Invalidated entire compiled rule cache due to lifecycle event for rule={}", ruleId);
    }

    private String buildCacheKey(String ruleIdOrAlias, Integer version) {
        UUID spaceId = getSpaceId();
        String spacePrefix = spaceId != null ? spaceId.toString() : "global";
        String versionSuffix = version != null ? String.valueOf(version) : "latest";
        return spacePrefix + ":" + ruleIdOrAlias + ":" + versionSuffix;
    }

    private UUID getSpaceId() {
        TenantContext.TenantInfo tenant = TenantContext.get();
        return tenant != null ? tenant.spaceId() : null;
    }
}
