package io.ruleforge.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventService;
import io.ruleforge.event.service.WebhookDispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Base class providing shared event-handling logic for all Kafka consumers
 * in the event service.
 */
abstract class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    protected final AuditEventService auditEventService;
    protected final WebhookDispatcher webhookDispatcher;
    protected final ObjectMapper objectMapper;

    protected AuditEventConsumer(AuditEventService auditEventService,
                                 WebhookDispatcher webhookDispatcher,
                                 ObjectMapper objectMapper) {
        this.auditEventService = auditEventService;
        this.webhookDispatcher = webhookDispatcher;
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

    protected void processAndStore(DomainEvent event) {
        auditEventService.recordEvent(event);
    }

    protected void dispatchWebhooks(DomainEvent event) {
        if (event.orgId() != null) {
            // Dispatch with orgId as the space context; real spaceId can come from payload
            UUID spaceId = extractSpaceId(event);
            if (spaceId != null) {
                webhookDispatcher.dispatch(spaceId, event);
            }
        }
    }

    private UUID extractSpaceId(DomainEvent event) {
        if (event.payload() != null && event.payload().containsKey("spaceId")) {
            Object spaceIdVal = event.payload().get("spaceId");
            if (spaceIdVal instanceof String s) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid spaceId in event payload: {}", s);
                }
            }
        }
        return null;
    }
}
