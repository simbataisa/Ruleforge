package io.ruleforge.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * No-op fallback {@link EventPublisher} used when Kafka is not available.
 * Logs events at debug level instead of publishing them.
 */
@Component
@ConditionalOnMissingBean(KafkaEventPublisher.class)
public class NoOpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(String topic, DomainEvent event) {
        log.debug("NoOp: would publish event {} to topic {}", event.eventType(), topic);
    }
}
