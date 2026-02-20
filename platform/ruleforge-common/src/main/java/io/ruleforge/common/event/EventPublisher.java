package io.ruleforge.common.event;

/**
 * Abstraction for publishing domain events to a messaging system.
 */
public interface EventPublisher {

    /**
     * Publish a domain event to the given topic.
     *
     * @param topic the target topic (e.g. Kafka topic name)
     * @param event the domain event to publish
     */
    void publish(String topic, DomainEvent event);
}
