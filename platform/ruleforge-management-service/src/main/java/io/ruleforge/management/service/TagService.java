package io.ruleforge.management.service;

import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.management.dto.CreateTagRequest;
import io.ruleforge.management.dto.TagDto;
import io.ruleforge.management.entity.RuleTagEntity;
import io.ruleforge.management.entity.TagEntity;
import io.ruleforge.management.repository.RuleTagRepository;
import io.ruleforge.management.repository.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;
    private final RuleTagRepository ruleTagRepository;

    public TagService(TagRepository tagRepository, RuleTagRepository ruleTagRepository) {
        this.tagRepository = tagRepository;
        this.ruleTagRepository = ruleTagRepository;
    }

    @Transactional
    public TagDto createTag(UUID orgId, CreateTagRequest request) {
        tagRepository.findByOrgIdAndName(orgId, request.name()).ifPresent(existing -> {
            throw new ConflictException("Tag with name '%s' already exists in this organization".formatted(request.name()));
        });

        TagEntity tag = new TagEntity();
        tag.setOrgId(orgId);
        tag.setName(request.name());
        tag.setColor(request.color());
        tag = tagRepository.save(tag);

        log.info("Tag created: id={}, name={}, orgId={}", tag.getId(), tag.getName(), orgId);

        return toDto(tag);
    }

    @Transactional(readOnly = true)
    public List<TagDto> listTags(UUID orgId) {
        return tagRepository.findByOrgId(orgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TagDto updateTag(UUID tagId, CreateTagRequest request) {
        TagEntity tag = findTagOrThrow(tagId);

        if (request.name() != null) {
            tag.setName(request.name());
        }
        if (request.color() != null) {
            tag.setColor(request.color());
        }
        tag = tagRepository.save(tag);

        log.info("Tag updated: id={}", tagId);

        return toDto(tag);
    }

    @Transactional
    public void deleteTag(UUID tagId) {
        TagEntity tag = findTagOrThrow(tagId);

        // Remove all rule-tag associations
        List<RuleTagEntity> associations = ruleTagRepository.findByTagId(tagId);
        ruleTagRepository.deleteAll(associations);

        tagRepository.delete(tag);
        log.info("Tag deleted: id={}", tagId);
    }

    @Transactional
    public void addTagToRule(UUID ruleId, UUID tagId) {
        findTagOrThrow(tagId);

        // Check if already associated
        List<RuleTagEntity> existing = ruleTagRepository.findByRuleId(ruleId).stream()
                .filter(rt -> rt.getTagId().equals(tagId))
                .toList();

        if (!existing.isEmpty()) {
            throw new ConflictException("Tag is already associated with this rule");
        }

        RuleTagEntity ruleTag = new RuleTagEntity();
        ruleTag.setRuleId(ruleId);
        ruleTag.setTagId(tagId);
        ruleTagRepository.save(ruleTag);

        log.info("Tag {} added to rule {}", tagId, ruleId);
    }

    @Transactional
    public void removeTagFromRule(UUID ruleId, UUID tagId) {
        ruleTagRepository.deleteByRuleIdAndTagId(ruleId, tagId);
        log.info("Tag {} removed from rule {}", tagId, ruleId);
    }

    private TagEntity findTagOrThrow(UUID tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag", tagId.toString()));
    }

    private TagDto toDto(TagEntity tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getColor());
    }
}
