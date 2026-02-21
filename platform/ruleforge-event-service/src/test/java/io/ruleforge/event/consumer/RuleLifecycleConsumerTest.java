package io.ruleforge.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventService;
import io.ruleforge.event.service.WebhookDispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleLifecycleConsumerTest {

    @Mock private AuditEventService auditEventService;
    @Mock private WebhookDispatcher webhookDispatcher;

    private RuleLifecycleConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new RuleLifecycleConsumer(auditEventService, webhookDispatcher, objectMapper);
    }

    @Test
    void onRuleLifecycleEvent_parsesAndStoresEvent() throws Exception {
        UUID spaceId = UUID.randomUUID();
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                "rule.created",
                "Rule",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Map.of("spaceId", spaceId.toString(), "name", "Test Rule")
        );

        String message = objectMapper.writeValueAsString(event);

        consumer.onRuleLifecycleEvent(message);

        verify(auditEventService).recordEvent(any(DomainEvent.class));
        verify(webhookDispatcher).dispatch(eq(spaceId), any(DomainEvent.class));
    }

    @Test
    void onRuleLifecycleEvent_invalidJson_doesNotThrow() {
        consumer.onRuleLifecycleEvent("not-valid-json");

        verifyNoInteractions(auditEventService);
        verifyNoInteractions(webhookDispatcher);
    }

    @Test
    void onRuleLifecycleEvent_noSpaceIdInPayload_storesButDoesNotDispatchWebhook() throws Exception {
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                "rule.deleted",
                "Rule",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Map.of("name", "Deleted Rule")
        );

        String message = objectMapper.writeValueAsString(event);

        consumer.onRuleLifecycleEvent(message);

        verify(auditEventService).recordEvent(any(DomainEvent.class));
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }
}
