package io.ruleforge.solver.service;

import io.ruleforge.solver.entity.SolverAbTestEntity;
import io.ruleforge.solver.repository.SolverAbTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AbTestRouter {

    private static final Logger log = LoggerFactory.getLogger(AbTestRouter.class);
    private static final String CACHE_PREFIX = "ab_test:";
    private static final String CACHE_SUFFIX = ":active";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String NULL_SENTINEL = "__NONE__";

    private final SolverAbTestRepository abTestRepository;
    private final StringRedisTemplate redisTemplate;

    public AbTestRouter(SolverAbTestRepository abTestRepository,
                        StringRedisTemplate redisTemplate) {
        this.abTestRepository = abTestRepository;
        this.redisTemplate = redisTemplate;
    }

    public Optional<AbTestAssignment> route(UUID ruleId) {
        try {
            return doRoute(ruleId);
        } catch (Exception e) {
            log.warn("A/B test routing failed for ruleId={}, falling back to no assignment: {}",
                    ruleId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AbTestAssignment> doRoute(UUID ruleId) {
        String cacheKey = CACHE_PREFIX + ruleId + CACHE_SUFFIX;

        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (NULL_SENTINEL.equals(cached)) {
                return Optional.empty();
            }
            return parseAndAssign(cached);
        }

        // Cache miss — query DB
        Optional<SolverAbTestEntity> active = abTestRepository.findByRuleIdAndStatus(ruleId, "ACTIVE");
        if (active.isEmpty()) {
            // Cache null result to avoid DB hammering
            redisTemplate.opsForValue().set(cacheKey, NULL_SENTINEL, CACHE_TTL);
            return Optional.empty();
        }

        SolverAbTestEntity test = active.get();
        String cacheValue = "%s|%d|%d|%d".formatted(
                test.getId(), test.getVersionA(), test.getVersionB(), test.getTrafficSplit());
        redisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL);

        return assign(test.getId(), test.getVersionA(), test.getVersionB(), test.getTrafficSplit());
    }

    private Optional<AbTestAssignment> parseAndAssign(String cached) {
        String[] parts = cached.split("\\|");
        if (parts.length != 4) return Optional.empty();
        UUID testId = UUID.fromString(parts[0]);
        int versionA = Integer.parseInt(parts[1]);
        int versionB = Integer.parseInt(parts[2]);
        int trafficSplit = Integer.parseInt(parts[3]);
        return assign(testId, versionA, versionB, trafficSplit);
    }

    private Optional<AbTestAssignment> assign(UUID testId, int versionA, int versionB, int trafficSplit) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < trafficSplit) {
            return Optional.of(new AbTestAssignment(testId, "A", versionA));
        } else {
            return Optional.of(new AbTestAssignment(testId, "B", versionB));
        }
    }

    public record AbTestAssignment(UUID testId, String abTestVersion, int versionNumber) {}
}
