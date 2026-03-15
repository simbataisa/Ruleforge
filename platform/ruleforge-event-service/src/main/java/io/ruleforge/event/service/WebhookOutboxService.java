package io.ruleforge.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookOutboxRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enqueues webhook delivery requests into the outbox table
 * for reliable, transactional at-least-once delivery.
 */
@Service
@Transactional
public class WebhookOutboxService {

    private static final Logger log = LoggerFactory.getLogger(WebhookOutboxService.class);

    private final WebhookOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public WebhookOutboxService(WebhookOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(DomainEvent event, List<WebhookEntity> matchedWebhooks) {
        for (WebhookEntity webhook : matchedWebhooks) {
            WebhookOutboxEntity outbox = new WebhookOutboxEntity();
            outbox.setWebhookId(webhook.getId());
            outbox.setOrgId(webhook.getOrgId());
            outbox.setEventType(event.eventType());
            outbox.setPayload(toPayloadMap(event));
            outbox.setStatus("PENDING");
            outbox.setAttemptCount(0);
            outboxRepository.save(outbox);
            log.debug("Enqueued outbox entry for webhook {} event {}", webhook.getId(), event.eventType());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toPayloadMap(DomainEvent event) {
        // Convert the DomainEvent to a Map for JSONB storage
        Map<String, Object> map = objectMapper.convertValue(event, LinkedHashMap.class);
        return map;
    }
}
