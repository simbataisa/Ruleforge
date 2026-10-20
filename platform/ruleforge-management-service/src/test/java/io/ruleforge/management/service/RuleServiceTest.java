package io.ruleforge.management.service;

import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateRuleRequest;
import io.ruleforge.management.dto.RuleDto;
import io.ruleforge.management.dto.UpdateRuleRequest;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.entity.FolderEntity;
import io.ruleforge.management.repository.FolderRepository;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleTagRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import io.ruleforge.management.repository.SpaceRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock private RuleRepository ruleRepository;
    @Mock private RuleVersionRepository ruleVersionRepository;
    @Mock private RuleTagRepository ruleTagRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private FolderRepository folderRepository;

    private RuleService ruleService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(ruleRepository, ruleVersionRepository,
                ruleTagRepository, eventPublisher, spaceRepository, folderRepository);
        TenantContext.set(new TenantContext.TenantInfo(orgId, spaceId, userId, "OWNER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRule_savesRuleAndVersion() {
        CreateRuleRequest request = new CreateRuleRequest(
                "Test Rule", "test-rule", "TABLE",
                null, null, "{}", null, null, "Initial version"
        );

        when(ruleRepository.findByAlias("test-rule")).thenReturn(Optional.empty());

        RuleEntity savedRule = createRuleEntity("Test Rule", "test-rule");
        when(ruleRepository.save(any(RuleEntity.class))).thenReturn(savedRule);
        when(ruleVersionRepository.save(any(RuleVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleDto result = ruleService.createRule(spaceId, request);

        assertNotNull(result);
        assertEquals("Test Rule", result.name());
        assertEquals("test-rule", result.alias());
        assertEquals("ACTIVE", result.status());

        verify(ruleRepository).save(any(RuleEntity.class));
        verify(ruleVersionRepository).save(any(RuleVersionEntity.class));
    }

    @Test
    void createRule_throwsConflictOnDuplicateAlias() {
        CreateRuleRequest request = new CreateRuleRequest(
                "Test Rule", "existing-alias", "TABLE",
                null, null, null, null, null, null
        );

        when(ruleRepository.findByAlias("existing-alias"))
                .thenReturn(Optional.of(createRuleEntity("Existing", "existing-alias")));

        assertThrows(ConflictException.class, () -> ruleService.createRule(spaceId, request));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void getRule_returnsDto() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("My Rule", "my-rule");
        entity.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        RuleDto result = ruleService.getRule(ruleId);

        assertEquals("My Rule", result.name());
        assertEquals("my-rule", result.alias());
    }

    @Test
    void getRule_throwsNotFoundForMissingRule() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ruleService.getRule(ruleId));
    }

    @Test
    void deleteRule_softDeletesSetsStatusDeleted() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        entity.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ruleService.deleteRule(ruleId);

        ArgumentCaptor<RuleEntity> captor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleRepository).save(captor.capture());
        assertEquals("DELETED", captor.getValue().getStatus());
    }

    @Test
    void lockRule_setsLockedFields() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        entity.setId(ruleId);
        entity.setLocked(false);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleDto result = ruleService.lockRule(ruleId, userId);

        assertTrue(result.locked());
    }

    @Test
    void lockRule_throwsConflictWhenAlreadyLocked() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        entity.setId(ruleId);
        entity.setLocked(true);
        entity.setLockedBy(UUID.randomUUID());

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));

        assertThrows(ConflictException.class, () -> ruleService.lockRule(ruleId, userId));
    }

    @Test
    void unlockRule_clearsLockedFields() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        entity.setId(ruleId);
        entity.setLocked(true);
        entity.setLockedBy(userId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleDto result = ruleService.unlockRule(ruleId, userId);

        assertFalse(result.locked());
    }

    @Test
    void unlockRule_throwsConflictWhenLockedByDifferentUser() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        entity.setId(ruleId);
        entity.setLocked(true);
        entity.setLockedBy(UUID.randomUUID());

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(entity));

        assertThrows(ConflictException.class, () -> ruleService.unlockRule(ruleId, userId));
    }

    @Test
    void listRulesForOrgAdvanced_withSpaceId_restrictsQueryToThatSpace() {
        List<SpaceEntity> orgSpaces = List.of(space(spaceId), space(UUID.randomUUID()));
        when(spaceRepository.findByOrgId(orgId)).thenReturn(orgSpaces);
        when(ruleRepository.findByAdvancedFilters(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ruleService.listRulesForOrgAdvanced(orgId, spaceId, null, null, null, null, null, "loan",
                null, null, null, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<Collection<UUID>> spaceIds = ArgumentCaptor.captor();
        verify(ruleRepository).findByAdvancedFilters(spaceIds.capture(), any(), eq("loan"), any(), anyBoolean(),
                any(), anyBoolean(), any(), any(), any(), any(), anyBoolean(), any(), any());
        assertEquals(List.of(spaceId), List.copyOf(spaceIds.getValue()));
    }

    @Test
    void listRulesForOrgAdvanced_withSpaceIdOutsideOrg_returnsEmptyWithoutQuerying() {
        List<SpaceEntity> orgSpaces = List.of(space(spaceId));
        when(spaceRepository.findByOrgId(orgId)).thenReturn(orgSpaces);

        PageResponse<RuleDto> result = ruleService.listRulesForOrgAdvanced(orgId, UUID.randomUUID(), null, null,
                null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertTrue(result.content().isEmpty());
        verifyNoInteractions(ruleRepository);
    }

    @Test
    void listRules_forSpace_usesFilteredQueryScopedToThatSpace() {
        when(ruleRepository.findByFilters(any(), any(), any(), anyBoolean(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ruleService.listRules(spaceId, null, "DRAFT", PageRequest.of(0, 20));

        verify(ruleRepository).findByFilters(eq(List.of(spaceId)), isNull(), eq(List.of()), eq(true),
                eq(List.of("DRAFT")), eq(false), any());
    }

    @Test
    void updateRule_rejectsWhenLockedByAnotherUser() {
        RuleEntity entity = lockedRule(UUID.randomUUID());
        when(ruleRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThrows(ConflictException.class, () -> ruleService.updateRule(entity.getId(),
                new UpdateRuleRequest("Renamed", null, null, null, null)));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void updateRule_allowsTheLockOwner() {
        RuleEntity entity = lockedRule(userId);
        when(ruleRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleDto result = ruleService.updateRule(entity.getId(),
                new UpdateRuleRequest("Renamed", null, null, null, null));

        assertEquals("Renamed", result.name());
        assertEquals(userId, result.lockedBy());
    }

    @Test
    void deleteRule_rejectsWhenLockedByAnotherUser() {
        RuleEntity entity = lockedRule(UUID.randomUUID());
        when(ruleRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThrows(ConflictException.class, () -> ruleService.deleteRule(entity.getId()));
        assertNotEquals("DELETED", entity.getStatus());
    }

    @Test
    void updateRule_movesRuleIntoFolderOfTheSameSpace() {
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        UUID folderId = UUID.randomUUID();
        FolderEntity folder = folder(folderId, spaceId);
        when(ruleRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleDto result = ruleService.updateRule(entity.getId(),
                new UpdateRuleRequest(null, null, null, folderId, null));

        assertEquals(folderId, result.folderId());
        assertEquals(spaceId, result.spaceId());
    }

    @Test
    void updateRule_rejectsFolderFromAnotherSpace() {
        RuleEntity entity = createRuleEntity("Rule", "rule-alias");
        UUID folderId = UUID.randomUUID();
        FolderEntity folder = folder(folderId, UUID.randomUUID());
        when(ruleRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        assertThrows(ConflictException.class, () -> ruleService.updateRule(entity.getId(),
                new UpdateRuleRequest(null, null, null, folderId, null)));
        verify(ruleRepository, never()).save(any());
    }

    private FolderEntity folder(UUID id, UUID folderSpaceId) {
        FolderEntity folder = new FolderEntity();
        folder.setId(id);
        folder.setSpaceId(folderSpaceId);
        return folder;
    }

    private RuleEntity lockedRule(UUID lockedBy) {
        RuleEntity entity = createRuleEntity("Locked Rule", "locked-rule");
        entity.setLocked(true);
        entity.setLockedBy(lockedBy);
        return entity;
    }

    private SpaceEntity space(UUID id) {
        SpaceEntity space = mock(SpaceEntity.class);
        lenient().when(space.getId()).thenReturn(id);
        return space;
    }

    private RuleEntity createRuleEntity(String name, String alias) {
        RuleEntity entity = new RuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setSpaceId(spaceId);
        entity.setName(name);
        entity.setAlias(alias);
        entity.setType("TABLE");
        entity.setStatus("ACTIVE");
        entity.setLatestVersion(1);
        entity.setLocked(false);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
