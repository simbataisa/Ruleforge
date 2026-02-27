package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleVersionCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleVersionCommentRepository extends JpaRepository<RuleVersionCommentEntity, UUID> {

    List<RuleVersionCommentEntity> findByRuleIdAndVersionOrderByCreatedAtAsc(UUID ruleId, int version);
}
