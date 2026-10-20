package io.ruleforge.event.consumer;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class RuleLifecycleConsumer extends AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RuleLifecycleConsumer.class);

    public RuleLifecycleConsumer(AuditEventProcessor auditEventProcessor,
                                 ObjectMapper objectMapper) {
        super(auditEventProcessor, objectMapper);
    }

    @KafkaListener(topics = "rule.lifecycle", groupId = "event-service")
    public void onRuleLifecycleEvent(String message, Acknowledgment ack) {
        log.debug("Received rule.lifecycle event");
        DomainEvent event = parseEvent(message);
        if (event == null) {
            ack.acknowledge();
            return;
        }

        processEvent(event);
        ack.acknowledge();
        log.debug("Processed rule.lifecycle event: type={}, aggregateId={}",
                event.eventType(), event.aggregateId());
    }
}
