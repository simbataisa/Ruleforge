package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleTagRepository extends JpaRepository<RuleTagEntity, UUID> {

    List<RuleTagEntity> findByRuleId(UUID ruleId);

    List<RuleTagEntity> findByTagId(UUID tagId);

    void deleteByRuleIdAndTagId(UUID ruleId, UUID tagId);
}
