package io.ruleforge.management.service;

import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateVersionRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.dto.UpdateVersionRequest;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);
    private static final String TOPIC_RULE_LIFECYCLE = "rule.lifecycle";

    private final RuleVersionRepository versionRepository;
    private final RuleRepository ruleRepository;
    private final EventPublisher eventPublisher;

    public VersionService(RuleVersionRepository versionRepository,
                          RuleRepository ruleRepository,
                          EventPublisher eventPublisher) {
        this.versionRepository = versionRepository;
        this.ruleRepository = ruleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RuleVersionDto createVersion(UUID ruleId, CreateVersionRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);

        int nextVersion = rule.getLatestVersion() + 1;

        RuleVersionEntity version = new RuleVersionEntity();
        version.setRuleId(ruleId);
        version.setVersion(nextVersion);
        version.setDefinition(request.definition() != null ? request.definition() : "{}");
        version.setInputModel(request.inputModel() != null ? request.inputModel() : "{}");
        version.setOutputModel(request.outputModel() != null ? request.outputModel() : "{}");
        version.setStatus("DRAFT");
        version.setChangeNote(request.changeNote());
        version.setCreatedBy(tenant.userId());
        version = versionRepository.save(version);

        // Update the rule's latest version
        rule.setLatestVersion(nextVersion);
        ruleRepository.save(rule);

        log.info("Version created: ruleId={}, version={}", ruleId, nextVersion);

        return toDto(version);
    }

    @Transactional
    public RuleVersionDto updateVersion(UUID ruleId, int version, UpdateVersionRequest request) {
        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"DRAFT".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only DRAFT versions can be updated. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        if (request.definition() != null) {
            versionEntity.setDefinition(request.definition());
        }
        if (request.inputModel() != null) {
            versionEntity.setInputModel(request.inputModel());
        }
        if (request.outputModel() != null) {
            versionEntity.setOutputModel(request.outputModel());
        }
        if (request.changeNote() != null) {
            versionEntity.setChangeNote(request.changeNote());
        }

        versionEntity = versionRepository.save(versionEntity);

        log.info("Version updated: ruleId={}, version={}", ruleId, version);

        return toDto(versionEntity);
    }

    @Transactional(readOnly = true)
    public RuleVersionDto getVersion(UUID ruleId, int version) {
        RuleVersionEntity entity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<RuleVersionDto> listVersions(UUID ruleId) {
        findRuleOrThrow(ruleId);
        return versionRepository.findByRuleId(ruleId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RuleVersionDto publishVersion(UUID ruleId, int version) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"DRAFT".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only DRAFT versions can be published. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        // Deprecate any currently published versions
        List<RuleVersionEntity> publishedVersions = versionRepository.findByRuleIdAndStatus(ruleId, "PUBLISHED");
        for (RuleVersionEntity published : publishedVersions) {
            published.setStatus("DEPRECATED");
            versionRepository.save(published);
            log.info("Version deprecated: ruleId={}, version={}", ruleId, published.getVersion());
        }

        // Publish the target version
        versionEntity.setStatus("PUBLISHED");
        versionEntity.setPublishedAt(Instant.now());
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version published: ruleId={}, version={}", ruleId, version);

        // Publish domain event
        try {
            DomainEvent event = DomainEvent.create(
                    "rule.version.published",
                    "Rule",
                    ruleId.toString(),
                    tenant.orgId(),
                    tenant.userId(),
                    Map.of(
                            "version", version,
                            "name", rule.getName(),
                            "alias", rule.getAlias()
                    )
            );
            eventPublisher.publish(TOPIC_RULE_LIFECYCLE, event);
        } catch (Exception e) {
            log.warn("Failed to publish version.published event: {}", e.getMessage());
        }

        return toDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto deprecateVersion(UUID ruleId, int version) {
        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PUBLISHED".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PUBLISHED versions can be deprecated. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        versionEntity.setStatus("DEPRECATED");
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version deprecated: ruleId={}, version={}", ruleId, version);

        return toDto(versionEntity);
    }

    private RuleEntity findRuleOrThrow(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));
    }

    @Transactional
    public RuleVersionDto requestApproval(UUID ruleId, int version) {
        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"DRAFT".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only DRAFT versions can be submitted for approval. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        versionEntity.setStatus("PENDING_APPROVAL");
        versionEntity.setRejectionReason(null);
        versionEntity = versionRepository.save(versionEntity);

        log.info("Approval requested: ruleId={}, version={}", ruleId, version);

        return toDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto approve(UUID ruleId, int version, UUID approverId) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PENDING_APPROVAL".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL versions can be approved. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        // Deprecate any currently published versions
        List<RuleVersionEntity> publishedVersions = versionRepository.findByRuleIdAndStatus(ruleId, "PUBLISHED");
        for (RuleVersionEntity published : publishedVersions) {
            published.setStatus("DEPRECATED");
            versionRepository.save(published);
            log.info("Version deprecated: ruleId={}, version={}", ruleId, published.getVersion());
        }

        // Approve and publish the version
        versionEntity.setStatus("PUBLISHED");
        versionEntity.setApprovedBy(approverId);
        versionEntity.setApprovedAt(Instant.now());
        versionEntity.setPublishedAt(Instant.now());
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version approved and published: ruleId={}, version={}, approvedBy={}", ruleId, version, approverId);

        // Publish domain event
        try {
            DomainEvent event = DomainEvent.create(
                    "rule.version.approved",
                    "Rule",
                    ruleId.toString(),
                    tenant.orgId(),
                    tenant.userId(),
                    Map.of(
                            "version", version,
                            "name", rule.getName(),
                            "alias", rule.getAlias(),
                            "approvedBy", approverId.toString()
                    )
            );
            eventPublisher.publish(TOPIC_RULE_LIFECYCLE, event);
        } catch (Exception e) {
            log.warn("Failed to publish version.approved event: {}", e.getMessage());
        }

        return toDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto reject(UUID ruleId, int version, String reason) {
        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PENDING_APPROVAL".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL versions can be rejected. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        versionEntity.setStatus("DRAFT");
        versionEntity.setRejectionReason(reason);
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version rejected: ruleId={}, version={}, reason={}", ruleId, version, reason);

        return toDto(versionEntity);
    }

    private RuleVersionDto toDto(RuleVersionEntity entity) {
        return new RuleVersionDto(
                entity.getId(),
                entity.getRuleId(),
                entity.getVersion(),
                entity.getDefinition(),
                entity.getInputModel(),
                entity.getOutputModel(),
                entity.getStatus(),
                entity.getChangeNote(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason()
        );
    }
}
