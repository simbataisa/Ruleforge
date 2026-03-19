package io.ruleforge.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.service.AuditEventProcessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleLifecycleConsumerTest {

    @Mock private AuditEventProcessor auditEventProcessor;
    @Mock private Acknowledgment acknowledgment;

    private RuleLifecycleConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new RuleLifecycleConsumer(auditEventProcessor, objectMapper);
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

        consumer.onRuleLifecycleEvent(message, acknowledgment);

        verify(auditEventProcessor).processEvent(any(DomainEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onRuleLifecycleEvent_invalidJson_doesNotThrow() {
        consumer.onRuleLifecycleEvent("not-valid-json", acknowledgment);

        verifyNoInteractions(auditEventProcessor);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onRuleLifecycleEvent_noSpaceIdInPayload_storesAndResolvesWebhooks() throws Exception {
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

        consumer.onRuleLifecycleEvent(message, acknowledgment);

        verify(auditEventProcessor).processEvent(any(DomainEvent.class));
        verify(acknowledgment).acknowledge();
    }
}
