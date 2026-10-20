package io.ruleforge.event.service;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookOutboxRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Atomically resets an outbox entry for retry. Using a single @Transactional method
     * prevents a race condition where OutboxPoller could claim the entry between the
     * find and save operations if they ran in separate auto-commit transactions.
     */
    @Transactional
    public WebhookOutboxEntity resetForRetry(UUID outboxId, UUID webhookId) {
        WebhookOutboxEntity entry = outboxRepository.findByIdAndWebhookId(outboxId, webhookId)
                .orElseThrow(() -> new EntityNotFoundException("WebhookOutbox", outboxId.toString()));
        entry.setStatus("PENDING");
        entry.setAttemptCount(0);
        entry.setNextAttemptAt(OffsetDateTime.now());
        return outboxRepository.save(entry);
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
