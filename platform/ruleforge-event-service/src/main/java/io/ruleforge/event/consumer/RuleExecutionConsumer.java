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
public class RuleExecutionConsumer extends AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionConsumer.class);

    public RuleExecutionConsumer(AuditEventService auditEventService,
                                 WebhookDispatcher webhookDispatcher,
                                 ObjectMapper objectMapper) {
        super(auditEventService, webhookDispatcher, objectMapper);
    }

    @KafkaListener(topics = "rule.execution", groupId = "event-service")
    public void onRuleExecutionEvent(String message) {
        log.debug("Received rule.execution event");
        DomainEvent event = parseEvent(message);
        if (event == null) {
            return;
        }

        // Store as audit event - high volume topic, but we persist all for now.
        // In production, consider sampling or aggregation for very high throughput.
        processAndStore(event);
        dispatchWebhooks(event);
        log.debug("Processed rule.execution event: type={}, aggregateId={}",
                event.eventType(), event.aggregateId());
    }
}
