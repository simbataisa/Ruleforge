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
public class UserActivityConsumer extends AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserActivityConsumer.class);

    public UserActivityConsumer(AuditEventService auditEventService,
                                WebhookDispatcher webhookDispatcher,
                                ObjectMapper objectMapper) {
        super(auditEventService, webhookDispatcher, objectMapper);
    }

    @KafkaListener(topics = "user.activity", groupId = "event-service")
    public void onUserActivityEvent(String message) {
        log.debug("Received user.activity event");
        DomainEvent event = parseEvent(message);
        if (event == null) {
            return;
        }

        processAndStore(event);
        log.debug("Processed user.activity event: type={}, actorId={}",
                event.eventType(), event.actorId());
    }
}
