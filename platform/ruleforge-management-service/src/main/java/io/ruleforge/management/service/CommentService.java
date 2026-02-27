package io.ruleforge.management.service;

import io.ruleforge.management.dto.RuleVersionCommentDto;
import io.ruleforge.management.entity.RuleVersionCommentEntity;
import io.ruleforge.management.repository.RuleVersionCommentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final RuleVersionCommentRepository commentRepository;
    private final RuleService ruleService;

    public CommentService(RuleVersionCommentRepository commentRepository, RuleService ruleService) {
        this.commentRepository = commentRepository;
        this.ruleService = ruleService;
    }

    @Transactional
    public RuleVersionCommentDto addComment(UUID ruleId, int version, UUID authorId, String body, UUID parentId) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);

        RuleVersionCommentEntity entity = new RuleVersionCommentEntity();
        entity.setRuleId(ruleId);
        entity.setVersion(version);
        entity.setAuthorId(authorId);
        entity.setBody(body);
        entity.setParentId(parentId);

        entity = commentRepository.save(entity);
        log.info("Comment added: id={}, ruleId={}, version={}", entity.getId(), ruleId, version);

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<RuleVersionCommentDto> getComments(UUID ruleId, int version) {
        // Verify tenant owns this rule
        ruleService.getRule(ruleId);

        return commentRepository.findByRuleIdAndVersionOrderByCreatedAtAsc(ruleId, version).stream()
                .map(this::toDto)
                .toList();
    }

    private RuleVersionCommentDto toDto(RuleVersionCommentEntity entity) {
        return new RuleVersionCommentDto(
                entity.getId(),
                entity.getRuleId(),
                entity.getVersion(),
                entity.getAuthorId(),
                entity.getBody(),
                entity.getParentId(),
                entity.getCreatedAt()
        );
    }
}
