package io.ruleforge.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.service.AuditEventService;
import io.ruleforge.event.service.WebhookOutboxService;
import io.ruleforge.event.service.WebhookScopeResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Base class providing shared event-handling logic for all Kafka consumers
 * in the event service.
 *
 * IMPORTANT: The @KafkaListener methods in subclasses call processEvent() first
 * (which runs within a transaction), then call ack.acknowledge() AFTER the
 * transaction commits. This ensures Kafka offset is only committed once the
 * outbox entries are persisted.
 */
abstract class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    protected final AuditEventService auditEventService;
    protected final WebhookScopeResolver webhookScopeResolver;
    protected final WebhookOutboxService webhookOutboxService;
    protected final ObjectMapper objectMapper;

    protected AuditEventConsumer(AuditEventService auditEventService,
                                 WebhookScopeResolver webhookScopeResolver,
                                 WebhookOutboxService webhookOutboxService,
                                 ObjectMapper objectMapper) {
        this.auditEventService = auditEventService;
        this.webhookScopeResolver = webhookScopeResolver;
        this.webhookOutboxService = webhookOutboxService;
        this.objectMapper = objectMapper;
    }

    protected DomainEvent parseEvent(String message) {
        try {
            return objectMapper.readValue(message, DomainEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse DomainEvent from message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Processes the event within a transaction: records the audit event and
     * enqueues outbox entries for matching webhooks.
     * Must NOT receive the Acknowledgment parameter.
     */
    @Transactional
    protected void processEvent(DomainEvent event) {
        auditEventService.recordEvent(event);
        List<WebhookEntity> matched = webhookScopeResolver.resolve(event);
        if (!matched.isEmpty()) {
            webhookOutboxService.enqueue(event, matched);
        }
    }
}
