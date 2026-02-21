package io.ruleforge.management.service;

import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateVersionRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock private RuleVersionRepository versionRepository;
    @Mock private RuleRepository ruleRepository;
    @Mock private EventPublisher eventPublisher;

    private VersionService versionService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID ruleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        versionService = new VersionService(versionRepository, ruleRepository, eventPublisher);
        TenantContext.set(new TenantContext.TenantInfo(orgId, spaceId, userId, "OWNER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createVersion_incrementsVersionNumber() {
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setLatestVersion(2);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        CreateVersionRequest request = new CreateVersionRequest("{}", null, null, "New version");
        when(versionRepository.save(any(RuleVersionEntity.class))).thenAnswer(inv -> {
            RuleVersionEntity v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            v.setCreatedAt(Instant.now());
            return v;
        });
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleVersionDto result = versionService.createVersion(ruleId, request);

        assertEquals(3, result.version());
        assertEquals("DRAFT", result.status());
    }

    @Test
    void publishVersion_changesStatusToPublished() {
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setName("Test Rule");
        rule.setAlias("test-rule");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity versionEntity = new RuleVersionEntity();
        versionEntity.setId(UUID.randomUUID());
        versionEntity.setRuleId(ruleId);
        versionEntity.setVersion(1);
        versionEntity.setStatus("DRAFT");
        versionEntity.setCreatedAt(Instant.now());
        when(versionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(versionEntity));
        when(versionRepository.findByRuleIdAndStatus(ruleId, "PUBLISHED")).thenReturn(List.of());
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleVersionDto result = versionService.publishVersion(ruleId, 1);

        assertEquals("PUBLISHED", result.status());
        assertNotNull(result.publishedAt());
    }

    @Test
    void publishVersion_throwsConflictForNonDraftVersion() {
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity versionEntity = new RuleVersionEntity();
        versionEntity.setStatus("PUBLISHED");
        when(versionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(versionEntity));

        assertThrows(ConflictException.class, () -> versionService.publishVersion(ruleId, 1));
    }

    @Test
    void publishVersion_deprecatesExistingPublishedVersions() {
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setName("Test Rule");
        rule.setAlias("test-rule");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity draftVersion = new RuleVersionEntity();
        draftVersion.setId(UUID.randomUUID());
        draftVersion.setRuleId(ruleId);
        draftVersion.setVersion(2);
        draftVersion.setStatus("DRAFT");
        draftVersion.setCreatedAt(Instant.now());
        when(versionRepository.findByRuleIdAndVersion(ruleId, 2)).thenReturn(Optional.of(draftVersion));

        RuleVersionEntity existingPublished = new RuleVersionEntity();
        existingPublished.setVersion(1);
        existingPublished.setStatus("PUBLISHED");
        when(versionRepository.findByRuleIdAndStatus(ruleId, "PUBLISHED"))
                .thenReturn(List.of(existingPublished));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        versionService.publishVersion(ruleId, 2);

        // Should save 3 times: deprecate v1, publish v2
        verify(versionRepository, times(2)).save(any(RuleVersionEntity.class));
    }

    @Test
    void deprecateVersion_throwsConflictForNonPublishedVersion() {
        RuleVersionEntity versionEntity = new RuleVersionEntity();
        versionEntity.setStatus("DRAFT");
        when(versionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(versionEntity));

        assertThrows(ConflictException.class, () -> versionService.deprecateVersion(ruleId, 1));
    }

    @Test
    void getVersion_throwsNotFoundForMissingVersion() {
        when(versionRepository.findByRuleIdAndVersion(ruleId, 99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> versionService.getVersion(ruleId, 99));
    }
}
