package io.ruleforge.event.consumer;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class providing shared event-handling logic for all Kafka consumers
 * in the event service.
 *
 * IMPORTANT: The @KafkaListener methods in subclasses call processEvent() first
 * (which delegates to AuditEventProcessor, a separate Spring bean, so the
 * @Transactional annotation on that bean is honoured by the AOP proxy), then
 * call ack.acknowledge() AFTER the transaction commits. This ensures Kafka
 * offset is only committed once the outbox entries are persisted.
 */
abstract class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    protected final AuditEventProcessor auditEventProcessor;
    protected final ObjectMapper objectMapper;

    protected AuditEventConsumer(AuditEventProcessor auditEventProcessor,
                                  ObjectMapper objectMapper) {
        this.auditEventProcessor = auditEventProcessor;
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
     * Delegates to AuditEventProcessor (a separate Spring bean) so that the
     * @Transactional on processEvent() is intercepted by the Spring AOP proxy.
     */
    protected void processEvent(DomainEvent event) {
        auditEventProcessor.processEvent(event);
    }
}
