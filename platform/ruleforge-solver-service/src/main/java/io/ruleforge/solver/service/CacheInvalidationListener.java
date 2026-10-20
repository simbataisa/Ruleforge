package io.ruleforge.solver.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import io.ruleforge.solver.cache.RuleCacheService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Kafka consumer that listens for rule lifecycle events and invalidates
 * the local compiled rule cache accordingly. This ensures that when rules
 * are published, deleted, or archived in the management service, the solver
 * picks up the changes without waiting for cache TTL expiry.
 */
@Component
public class CacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private static final Set<String> INVALIDATION_EVENTS = Set.of(
            "rule.published",
            "rule.deleted",
            "rule.archived",
            "rule.version.published",
            "rule.version.deleted"
    );

    private final RuleCacheService ruleCacheService;
    private final ObjectMapper objectMapper;

    public CacheInvalidationListener(RuleCacheService ruleCacheService, ObjectMapper objectMapper) {
        this.ruleCacheService = ruleCacheService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "rule.lifecycle", groupId = "solver-cache-invalidation")
    public void onRuleLifecycleEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(
                    message, new TypeReference<>() {});

            String eventType = (String) event.get("eventType");
            String aggregateId = (String) event.get("aggregateId");

            if (eventType == null || aggregateId == null) {
                log.debug("Ignoring lifecycle event with missing eventType or aggregateId");
                return;
            }

            if (!INVALIDATION_EVENTS.contains(eventType)) {
                log.debug("Ignoring non-invalidation event type: {}", eventType);
                return;
            }

            log.info("Received cache invalidation event: type={}, ruleId={}", eventType, aggregateId);

            // Extract version from payload if available for targeted invalidation
            Map<String, Object> payload = getPayload(event);
            Integer version = extractVersion(payload);

            if (version != null) {
                ruleCacheService.invalidate(aggregateId, version);
            } else {
                ruleCacheService.invalidateAll(aggregateId);
            }

        } catch (Exception e) {
            log.error("Failed to process rule lifecycle event: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPayload(Map<String, Object> event) {
        Object payload = event.get("payload");
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        return Map.of();
    }

    private Integer extractVersion(Map<String, Object> payload) {
        Object versionObj = payload.get("version");
        if (versionObj instanceof Number number) {
            return number.intValue();
        }
        if (versionObj instanceof String versionStr) {
            try {
                return Integer.parseInt(versionStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
