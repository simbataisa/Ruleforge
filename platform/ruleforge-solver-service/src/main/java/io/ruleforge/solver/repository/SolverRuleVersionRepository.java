package io.ruleforge.solver.repository;

import io.ruleforge.solver.entity.SolverRuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only repository for accessing rule version data.
 * Used to load rule definitions and compiled artifacts for evaluation.
 */
@Repository
public interface SolverRuleVersionRepository extends JpaRepository<SolverRuleVersionEntity, UUID> {

    Optional<SolverRuleVersionEntity> findByRuleIdAndVersion(UUID ruleId, int version);

    Optional<SolverRuleVersionEntity> findByRuleIdAndStatus(UUID ruleId, String status);

    /**
     * Find the latest published version for a given rule.
     * Orders by version descending and returns the first match with the given status.
     */
    Optional<SolverRuleVersionEntity> findFirstByRuleIdAndStatusOrderByVersionDesc(UUID ruleId, String status);
}
