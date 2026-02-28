package io.ruleforge.management.service;

import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.management.dto.TagAutocompleteDto;
import io.ruleforge.management.entity.RuleTagEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleTagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final RuleTagRepository ruleTagRepository;
    private final RuleRepository ruleRepository;

    public TagService(RuleTagRepository ruleTagRepository, RuleRepository ruleRepository) {
        this.ruleTagRepository = ruleTagRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public List<TagAutocompleteDto> autocomplete(UUID orgId, String prefix, int limit) {
        return ruleTagRepository.findTagsWithCount(orgId, prefix, limit).stream()
                .map(row -> new TagAutocompleteDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Transactional
    public List<String> addTagsToRule(UUID ruleId, List<String> tags) {
        ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));

        for (String tag : tags) {
            String normalizedTag = tag.trim().toLowerCase();
            if (normalizedTag.isEmpty() || normalizedTag.length() > 100) {
                continue;
            }
            // Idempotent: skip if already exists
            if (ruleTagRepository.findByRuleIdAndTag(ruleId, normalizedTag).isEmpty()) {
                RuleTagEntity entity = new RuleTagEntity();
                entity.setRuleId(ruleId);
                entity.setTag(normalizedTag);
                ruleTagRepository.save(entity);
            }
        }

        log.info("Tags added to rule {}: {}", ruleId, tags);
        return getTagsForRule(ruleId);
    }

    @Transactional
    public void removeTagFromRule(UUID ruleId, String tag) {
        ruleTagRepository.deleteByRuleIdAndTag(ruleId, tag.trim().toLowerCase());
        log.info("Tag '{}' removed from rule {}", tag, ruleId);
    }

    @Transactional(readOnly = true)
    public List<String> getTagsForRule(UUID ruleId) {
        return ruleTagRepository.findByRuleId(ruleId).stream()
                .map(RuleTagEntity::getTag)
                .sorted()
                .toList();
    }

    @Transactional
    public List<String> setTagsForRule(UUID ruleId, List<String> tags) {
        ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));

        // Remove all existing tags
        ruleTagRepository.deleteByRuleId(ruleId);

        // Add new tags
        for (String tag : tags) {
            String normalizedTag = tag.trim().toLowerCase();
            if (normalizedTag.isEmpty() || normalizedTag.length() > 100) {
                continue;
            }
            RuleTagEntity entity = new RuleTagEntity();
            entity.setRuleId(ruleId);
            entity.setTag(normalizedTag);
            ruleTagRepository.save(entity);
        }

        log.info("Tags set for rule {}: {}", ruleId, tags);
        return getTagsForRule(ruleId);
    }
}
