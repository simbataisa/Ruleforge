package io.ruleforge.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.repository.AuditEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock private AuditEventRepository auditEventRepository;

    private AuditEventService auditEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditEventService = new AuditEventService(auditEventRepository, objectMapper);
    }

    @Test
    void recordEvent_savesEntityWithCorrectFields() {
        UUID orgId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String aggregateId = UUID.randomUUID().toString();

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                "rule.created",
                "Rule",
                aggregateId,
                orgId,
                actorId,
                Instant.now(),
                Map.of("name", "Test Rule", "type", "DECISION_TABLE")
        );

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEventEntity saved = captor.getValue();
        assertEquals(orgId, saved.getOrgId());
        assertEquals("rule.created", saved.getEventType());
        assertEquals(actorId, saved.getActorId());
        assertEquals("Rule", saved.getResourceType());
        assertEquals(aggregateId, saved.getResourceId());
        assertNotNull(saved.getChanges());
        assertTrue(saved.getChanges().contains("DECISION_TABLE"));
    }

    @Test
    void recordEvent_handlesNullPayload() {
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                "user.logged_in",
                "User",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                null
        );

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEventEntity saved = captor.getValue();
        assertNull(saved.getChanges());
    }

    @Test
    void recordEvent_serializesPayloadToJson() {
        Map<String, Object> payload = Map.of(
                "version", 3,
                "alias", "my-rule"
        );

        DomainEvent event = DomainEvent.create(
                "rule.version.published", "Rule",
                UUID.randomUUID().toString(),
                UUID.randomUUID(), UUID.randomUUID(),
                payload
        );

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(captor.capture());

        String changes = captor.getValue().getChanges();
        assertNotNull(changes);
        assertTrue(changes.contains("my-rule"));
        assertTrue(changes.contains("3"));
    }
}
