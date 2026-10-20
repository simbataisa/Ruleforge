package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleVersionRepository extends JpaRepository<RuleVersionEntity, UUID> {

    List<RuleVersionEntity> findByRuleId(UUID ruleId);

    Optional<RuleVersionEntity> findByRuleIdAndVersion(UUID ruleId, int version);

    List<RuleVersionEntity> findByRuleIdAndStatus(UUID ruleId, String status);

    Optional<RuleVersionEntity> findTopByRuleIdOrderByVersionDesc(UUID ruleId);

    boolean existsByRuleIdAndStatus(UUID ruleId, String status);
}
