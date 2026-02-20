package io.ruleforge.solver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.ruleforge.core.model.CompiledRule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine cache configuration for the solver service.
 * Provides an L1 in-process cache for compiled rule artifacts to avoid
 * repeated database lookups and recompilation.
 */
@Configuration
public class CacheConfig {

    /**
     * L1 compiled rule cache. Keys are formatted as {@code "spaceId:ruleId:version"}.
     * Entries expire 5 minutes after write and the cache holds at most 1000 entries.
     * Stats recording is enabled for monitoring via actuator.
     */
    @Bean
    public Cache<String, CompiledRule> compiledRuleCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
}
