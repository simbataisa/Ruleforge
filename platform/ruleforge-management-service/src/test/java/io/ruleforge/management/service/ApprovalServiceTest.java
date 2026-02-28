package io.ruleforge.management.service;

import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.management.dto.ApprovalRequestDto;
import io.ruleforge.management.dto.AssigneeStatusDto;
import io.ruleforge.management.entity.ApprovalRequestAssigneeEntity;
import io.ruleforge.management.entity.ApprovalRequestEntity;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.UserEntity;
import io.ruleforge.management.repository.ApprovalRequestAssigneeRepository;
import io.ruleforge.management.repository.ApprovalRequestRepository;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import io.ruleforge.management.repository.SpaceApprovalSettingsRepository;
import io.ruleforge.management.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private ApprovalRequestAssigneeRepository assigneeRepository;
    @Mock private SpaceApprovalSettingsRepository settingsRepository;
    @Mock private RuleRepository ruleRepository;
    @Mock private RuleVersionRepository versionRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private UserRepository userRepository;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(
                approvalRequestRepository,
                assigneeRepository,
                settingsRepository,
                ruleRepository,
                versionRepository,
                eventPublisher,
                userRepository
        );
    }

    // --- toApprovalRequestDto ---

    @Test
    void toApprovalRequestDto_enrichesRequestedByName_usingFullName() {
        UUID requestId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ApprovalRequestEntity entity = buildApprovalRequest(requestId, ruleId, requesterId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(buildRule(ruleId, "My Rule", "TABLE")));
        when(assigneeRepository.findByRequestId(requestId)).thenReturn(List.of());
        when(userRepository.findAllById(anyList())).thenReturn(List.of(buildUser(requesterId, "Alice Smith", null)));

        ApprovalRequestDto dto = approvalService.toApprovalRequestDto(entity);

        assertThat(dto.requestedBy()).isEqualTo(requesterId);
        assertThat(dto.requestedByName()).isEqualTo("Alice Smith");
        assertThat(dto.ruleName()).isEqualTo("My Rule");
    }

    @Test
    void toApprovalRequestDto_fallsBackToEmail_whenFullNameIsNull() {
        UUID requestId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ApprovalRequestEntity entity = buildApprovalRequest(requestId, ruleId, requesterId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());
        when(assigneeRepository.findByRequestId(requestId)).thenReturn(List.of());
        when(userRepository.findAllById(anyList())).thenReturn(List.of(buildUser(requesterId, null, "alice@example.com")));

        ApprovalRequestDto dto = approvalService.toApprovalRequestDto(entity);

        assertThat(dto.requestedByName()).isEqualTo("alice@example.com");
    }

    @Test
    void toApprovalRequestDto_enrichesAssigneeName() {
        UUID requestId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        ApprovalRequestEntity entity = buildApprovalRequest(requestId, ruleId, requesterId);
        ApprovalRequestAssigneeEntity assigneeEntity = buildAssignee(requestId, assigneeId, "PENDING");

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());
        when(assigneeRepository.findByRequestId(requestId)).thenReturn(List.of(assigneeEntity));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(
                buildUser(requesterId, "Alice Smith", null),
                buildUser(assigneeId, "Bob Jones", null)
        ));

        ApprovalRequestDto dto = approvalService.toApprovalRequestDto(entity);

        assertThat(dto.assignees()).hasSize(1);
        AssigneeStatusDto assigneeDto = dto.assignees().get(0);
        assertThat(assigneeDto.memberId()).isEqualTo(assigneeId);
        assertThat(assigneeDto.name()).isEqualTo("Bob Jones");
        assertThat(assigneeDto.status()).isEqualTo("PENDING");
    }

    @Test
    void toApprovalRequestDto_handlesUnknownUser_gracefully() {
        UUID requestId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ApprovalRequestEntity entity = buildApprovalRequest(requestId, ruleId, requesterId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());
        when(assigneeRepository.findByRequestId(requestId)).thenReturn(List.of());
        // User not found in DB
        when(userRepository.findAllById(anyList())).thenReturn(List.of());

        ApprovalRequestDto dto = approvalService.toApprovalRequestDto(entity);

        assertThat(dto.requestedByName()).isNull();
    }

    // --- AssigneeStatusDto field names ---

    @Test
    void assigneeStatusDto_hasMemberIdField() {
        UUID memberId = UUID.randomUUID();
        AssigneeStatusDto dto = new AssigneeStatusDto(memberId, "Test User", "PENDING", null);

        assertThat(dto.memberId()).isEqualTo(memberId);
        assertThat(dto.name()).isEqualTo("Test User");
        assertThat(dto.status()).isEqualTo("PENDING");
        assertThat(dto.respondedAt()).isNull();
    }

    // --- Helpers ---

    private ApprovalRequestEntity buildApprovalRequest(UUID id, UUID ruleId, UUID requestedBy) {
        ApprovalRequestEntity e = new ApprovalRequestEntity();
        setField(e, "id", id);
        e.setRuleId(ruleId);
        e.setRequestedBy(requestedBy);
        e.setStatus("PENDING");
        e.setRequestedAt(Instant.now());
        return e;
    }

    private ApprovalRequestAssigneeEntity buildAssignee(UUID requestId, UUID assigneeId, String status) {
        ApprovalRequestAssigneeEntity e = new ApprovalRequestAssigneeEntity();
        e.setRequestId(requestId);
        e.setAssigneeId(assigneeId);
        e.setStatus(status);
        return e;
    }

    private RuleEntity buildRule(UUID id, String name, String type) {
        RuleEntity e = new RuleEntity();
        setField(e, "id", id);
        e.setName(name);
        e.setType(type);
        return e;
    }

    private UserEntity buildUser(UUID id, String fullName, String email) {
        UserEntity e = new UserEntity();
        setField(e, "id", id);
        setField(e, "fullName", fullName);
        setField(e, "email", email);
        return e;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
