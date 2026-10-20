package io.ruleforge.event.service;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.AuditEventEntity;
import io.ruleforge.event.store.AuditEventStore;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock private AuditEventStore auditEventStore;

    private AuditEventService auditEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditEventService = new AuditEventService(auditEventStore, objectMapper);
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

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventStore).save(captor.capture());

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

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventStore).save(captor.capture());

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

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventStore).save(captor.capture());

        String changes = captor.getValue().getChanges();
        assertNotNull(changes);
        assertTrue(changes.contains("my-rule"));
        assertTrue(changes.contains("3"));
    }

    @Test
    void recordEvent_extractsActorNameAndResourceNameFromPayload() {
        Map<String, Object> payload = Map.of(
                "actorName", "Alice Admin",
                "name", "Loan Eligibility",
                "actorType", "API_KEY",
                "spaceId", "d0000000-0000-0000-0000-000000000001"
        );

        DomainEvent event = DomainEvent.create(
                "rule.created", "Rule",
                UUID.randomUUID().toString(),
                UUID.randomUUID(), UUID.randomUUID(),
                payload
        );

        auditEventService.recordEvent(event);

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventStore).save(captor.capture());

        AuditEventEntity saved = captor.getValue();
        assertEquals("Alice Admin", saved.getActorName());
        assertEquals("Loan Eligibility", saved.getResourceName());
        assertEquals("API_KEY", saved.getActorType());
        assertEquals(UUID.fromString("d0000000-0000-0000-0000-000000000001"), saved.getSpaceId());
    }
}
