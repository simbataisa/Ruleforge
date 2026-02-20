package io.ruleforge.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventService;
import io.ruleforge.event.service.WebhookDispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RuleLifecycleConsumer extends AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RuleLifecycleConsumer.class);

    public RuleLifecycleConsumer(AuditEventService auditEventService,
                                 WebhookDispatcher webhookDispatcher,
                                 ObjectMapper objectMapper) {
        super(auditEventService, webhookDispatcher, objectMapper);
    }

    @KafkaListener(topics = "rule.lifecycle", groupId = "event-service")
    public void onRuleLifecycleEvent(String message) {
        log.debug("Received rule.lifecycle event");
        DomainEvent event = parseEvent(message);
        if (event == null) {
            return;
        }

        processAndStore(event);
        dispatchWebhooks(event);
        log.debug("Processed rule.lifecycle event: type={}, aggregateId={}",
                event.eventType(), event.aggregateId());
    }
}
