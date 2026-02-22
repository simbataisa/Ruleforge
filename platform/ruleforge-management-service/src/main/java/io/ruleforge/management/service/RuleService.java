package io.ruleforge.management.service;

import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateRuleRequest;
import io.ruleforge.management.dto.RuleDto;
import io.ruleforge.management.dto.UpdateRuleRequest;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleTagEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.entity.TagEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleTagRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import io.ruleforge.management.repository.SpaceRepository;
import io.ruleforge.management.repository.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleService.class);
    private static final String TOPIC_RULE_LIFECYCLE = "rule.lifecycle";

    private final RuleRepository ruleRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final RuleTagRepository ruleTagRepository;
    private final TagRepository tagRepository;
    private final EventPublisher eventPublisher;
    private final SpaceRepository spaceRepository;

    public RuleService(RuleRepository ruleRepository,
                       RuleVersionRepository ruleVersionRepository,
                       RuleTagRepository ruleTagRepository,
                       TagRepository tagRepository,
                       EventPublisher eventPublisher,
                       SpaceRepository spaceRepository) {
        this.ruleRepository = ruleRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.ruleTagRepository = ruleTagRepository;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
        this.spaceRepository = spaceRepository;
    }

    @Transactional
    public RuleDto createRule(UUID spaceId, CreateRuleRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();

        // Auto-generate alias if not provided
        String alias = request.alias();
        if (alias == null || alias.isBlank()) {
            alias = request.name().toLowerCase()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9_-]", "");
        }

        // Check alias uniqueness
        final String finalAlias = alias;
        ruleRepository.findByAlias(finalAlias).ifPresent(existing -> {
            throw new ConflictException("Rule with alias '%s' already exists".formatted(finalAlias));
        });

        // Create the rule entity
        RuleEntity rule = new RuleEntity();
        rule.setSpaceId(spaceId);
        rule.setFolderId(request.folderId());
        rule.setName(request.name());
        rule.setAlias(finalAlias);
        rule.setDescription(request.description());
        rule.setType(request.type());
        rule.setStatus("DRAFT");
        rule.setLatestVersion(1);
        rule.setLocked(false);
        rule.setCreatedBy(tenant.userId());
        rule = ruleRepository.save(rule);

        // Create initial DRAFT version
        RuleVersionEntity version = new RuleVersionEntity();
        version.setRuleId(rule.getId());
        version.setVersion(1);
        version.setDefinition(request.definition() != null ? request.definition() : "{}");
        version.setInputModel(request.inputModel() != null ? request.inputModel() : "{}");
        version.setOutputModel(request.outputModel() != null ? request.outputModel() : "{}");
        version.setStatus("DRAFT");
        version.setChangeNote(request.changeNote());
        version.setCreatedBy(tenant.userId());
        ruleVersionRepository.save(version);

        log.info("Rule created: id={}, name={}, alias={}", rule.getId(), rule.getName(), rule.getAlias());

        // Publish domain event
        publishEvent("rule.created", rule, tenant);

        return toDto(rule);
    }

    @Transactional(readOnly = true)
    public RuleDto getRule(UUID ruleId) {
        RuleEntity rule = findRuleOrThrow(ruleId);
        return toDto(rule);
    }

    @Transactional(readOnly = true)
    public PageResponse<RuleDto> listRules(UUID spaceId, UUID folderId, String status, Pageable pageable) {
        Page<RuleEntity> page;

        if (folderId != null && status != null) {
            page = ruleRepository.findBySpaceIdAndFolderIdAndStatus(spaceId, folderId, status, pageable);
        } else if (folderId != null) {
            page = ruleRepository.findBySpaceIdAndFolderId(spaceId, folderId, pageable);
        } else if (status != null) {
            page = ruleRepository.findBySpaceIdAndStatus(spaceId, status, pageable);
        } else {
            page = ruleRepository.findBySpaceId(spaceId, pageable);
        }

        List<RuleDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<RuleDto> listRulesForOrg(UUID orgId, UUID folderId, String status, Pageable pageable) {
        List<UUID> spaceIds = spaceRepository.findByOrgId(orgId).stream()
                .map(io.ruleforge.management.entity.SpaceEntity::getId)
                .toList();

        if (spaceIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, pageable.getPageSize(), 0, 0);
        }

        Page<RuleEntity> page;
        if (folderId != null) {
            page = ruleRepository.findBySpaceIdInAndFolderId(spaceIds, folderId, pageable);
        } else if (status != null) {
            page = ruleRepository.findBySpaceIdInAndStatus(spaceIds, status, pageable);
        } else {
            page = ruleRepository.findBySpaceIdIn(spaceIds, pageable);
        }

        List<RuleDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public RuleDto updateRule(UUID ruleId, UpdateRuleRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);

        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.alias() != null) {
            ruleRepository.findByAlias(request.alias())
                    .filter(existing -> !existing.getId().equals(ruleId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Rule with alias '%s' already exists".formatted(request.alias()));
                    });
            rule.setAlias(request.alias());
        }
        if (request.description() != null) {
            rule.setDescription(request.description());
        }
        if (request.folderId() != null) {
            rule.setFolderId(request.folderId());
        }

        rule = ruleRepository.save(rule);
        log.info("Rule updated: id={}", ruleId);

        publishEvent("rule.updated", rule, tenant);

        return toDto(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);
        rule.setStatus("DELETED");
        ruleRepository.save(rule);

        log.info("Rule soft-deleted: id={}", ruleId);
        publishEvent("rule.deleted", rule, tenant);
    }

    @Transactional
    public RuleDto lockRule(UUID ruleId, UUID userId) {
        RuleEntity rule = findRuleOrThrow(ruleId);

        if (rule.isLocked()) {
            throw new ConflictException("Rule is already locked by user %s".formatted(rule.getLockedBy()));
        }

        rule.setLocked(true);
        rule.setLockedBy(userId);
        rule.setLockedAt(Instant.now());
        rule = ruleRepository.save(rule);

        log.info("Rule locked: id={}, by={}", ruleId, userId);
        return toDto(rule);
    }

    @Transactional
    public RuleDto unlockRule(UUID ruleId, UUID userId) {
        RuleEntity rule = findRuleOrThrow(ruleId);

        if (!rule.isLocked()) {
            throw new ConflictException("Rule is not locked");
        }
        if (!rule.getLockedBy().equals(userId)) {
            throw new ConflictException("Rule is locked by a different user");
        }

        rule.setLocked(false);
        rule.setLockedBy(null);
        rule.setLockedAt(null);
        rule = ruleRepository.save(rule);

        log.info("Rule unlocked: id={}, by={}", ruleId, userId);
        return toDto(rule);
    }

    private RuleEntity findRuleOrThrow(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));
    }

    private RuleDto toDto(RuleEntity rule) {
        List<String> tagNames = ruleTagRepository.findByRuleId(rule.getId()).stream()
                .map(RuleTagEntity::getTagId)
                .map(tagRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(opt -> opt.get().getName())
                .toList();

        return new RuleDto(
                rule.getId(),
                rule.getName(),
                rule.getAlias(),
                rule.getDescription(),
                rule.getType(),
                rule.getStatus(),
                rule.getLatestVersion(),
                rule.isLocked(),
                rule.getFolderId(),
                rule.getCreatedAt(),
                rule.getUpdatedAt(),
                tagNames
        );
    }

    private void publishEvent(String eventType, RuleEntity rule, TenantContext.TenantInfo tenant) {
        try {
            DomainEvent event = DomainEvent.create(
                    eventType,
                    "Rule",
                    rule.getId().toString(),
                    tenant.orgId(),
                    tenant.userId(),
                    Map.of(
                            "name", rule.getName(),
                            "alias", rule.getAlias(),
                            "type", rule.getType(),
                            "status", rule.getStatus(),
                            "spaceId", rule.getSpaceId().toString()
                    )
            );
            eventPublisher.publish(TOPIC_RULE_LIFECYCLE, event);
        } catch (Exception e) {
            log.warn("Failed to publish event {}: {}", eventType, e.getMessage());
        }
    }
}
