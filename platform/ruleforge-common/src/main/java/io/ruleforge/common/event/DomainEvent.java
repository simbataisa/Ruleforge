package io.ruleforge.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base record for all domain events published to Kafka.
 *
 * @param eventId       unique event identifier
 * @param eventType     dot-notation event type, e.g. "rule.created", "user.registered"
 * @param aggregateType the aggregate root type, e.g. "Rule", "User"
 * @param aggregateId   the aggregate root ID
 * @param orgId         organisation that owns the aggregate
 * @param actorId       user or system principal that triggered the event
 * @param timestamp     when the event was created
 * @param payload       arbitrary event-specific data
 */
public record DomainEvent(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        UUID orgId,
        UUID actorId,
        Instant timestamp,
        Map<String, Object> payload
) {

    /**
     * Convenience factory that auto-generates eventId and timestamp.
     */
    public static DomainEvent create(String eventType, String aggregateType, String aggregateId,
                                     UUID orgId, UUID actorId, Map<String, Object> payload) {
        return new DomainEvent(
                UUID.randomUUID().toString(),
                eventType,
                aggregateType,
                aggregateId,
                orgId,
                actorId,
                Instant.now(),
                payload
        );
    }
}
