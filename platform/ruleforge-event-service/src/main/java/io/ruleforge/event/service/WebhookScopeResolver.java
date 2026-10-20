package io.ruleforge.event.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookScopeType;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves all matching active webhooks for a given domain event across
 * all scope levels (ORG, SPACE, FOLDER, RULE).
 */
@Service
public class WebhookScopeResolver {

    private static final Logger log = LoggerFactory.getLogger(WebhookScopeResolver.class);

    private final WebhookRepository webhookRepository;
    private final ObjectMapper objectMapper;

    public WebhookScopeResolver(WebhookRepository webhookRepository, ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolve all active webhooks that match the given event across all scope levels.
     * Returns a deduplicated list filtered by event subscription patterns.
     */
    public List<WebhookEntity> resolve(DomainEvent event) {
        if (event.orgId() == null) {
            return Collections.emptyList();
        }

        // Collect candidate webhooks from all matching scopes
        Map<UUID, WebhookEntity> candidates = new LinkedHashMap<>();

        // ORG scope: orgId = event.orgId
        addCandidates(candidates, WebhookScopeType.ORG, event.orgId());

        // SPACE scope: from payload
        UUID spaceId = extractUuidFromPayload(event, "spaceId");
        if (spaceId != null) {
            addCandidates(candidates, WebhookScopeType.SPACE, spaceId);
        }

        // FOLDER scope: from payload
        UUID folderId = extractUuidFromPayload(event, "folderId");
        if (folderId != null) {
            addCandidates(candidates, WebhookScopeType.FOLDER, folderId);
        }

        // RULE scope: from aggregateId (if aggregateType is Rule) or from payload
        UUID ruleId = extractRuleId(event);
        if (ruleId != null) {
            addCandidates(candidates, WebhookScopeType.RULE, ruleId);
        }

        // Filter by active status, circuit not open, and event subscription match
        List<WebhookEntity> matched = new ArrayList<>();
        for (WebhookEntity webhook : candidates.values()) {
            if (!webhook.isActive()) {
                continue;
            }
            if (webhook.getCircuitOpenedAt() != null) {
                continue;
            }
            if (isSubscribedToEvent(webhook, event.eventType())) {
                matched.add(webhook);
            }
        }

        log.debug("Resolved {} webhooks for event type={} orgId={}",
                matched.size(), event.eventType(), event.orgId());
        return matched;
    }

    private void addCandidates(Map<UUID, WebhookEntity> candidates,
                                WebhookScopeType scopeType, UUID scopeId) {
        List<WebhookEntity> webhooks = webhookRepository.findByScopeTypeAndScopeId(scopeType, scopeId);
        for (WebhookEntity webhook : webhooks) {
            candidates.putIfAbsent(webhook.getId(), webhook);
        }
    }

    private UUID extractUuidFromPayload(DomainEvent event, String key) {
        if (event.payload() == null || !event.payload().containsKey(key)) {
            return null;
        }
        Object val = event.payload().get(key);
        if (val instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID for {} in event payload: {}", key, s);
            }
        }
        return null;
    }

    private UUID extractRuleId(DomainEvent event) {
        // If the aggregate is a Rule, use aggregateId
        if ("Rule".equalsIgnoreCase(event.aggregateType()) && event.aggregateId() != null) {
            try {
                return UUID.fromString(event.aggregateId());
            } catch (IllegalArgumentException e) {
                // not a UUID
            }
        }
        // Also check payload
        return extractUuidFromPayload(event, "ruleId");
    }

    /**
     * Check if a webhook is subscribed to the given event type.
     * Supports exact match, wildcard "*", and prefix matching "rule.*".
     */
    private boolean isSubscribedToEvent(WebhookEntity webhook, String eventType) {
        List<String> subscribedEvents = deserializeEvents(webhook.getEvents());
        if (subscribedEvents.isEmpty()) {
            return false;
        }
        if (subscribedEvents.contains("*")) {
            return true;
        }
        for (String subscribed : subscribedEvents) {
            if (subscribed.equals(eventType)) {
                return true;
            }
            if (subscribed.endsWith(".*")) {
                String prefix = subscribed.substring(0, subscribed.length() - 1);
                if (eventType.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> deserializeEvents(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            log.warn("Failed to deserialize webhook events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
