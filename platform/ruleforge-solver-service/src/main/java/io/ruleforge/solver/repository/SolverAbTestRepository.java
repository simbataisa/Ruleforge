package io.ruleforge.solver.repository;

import io.ruleforge.solver.entity.SolverAbTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolverAbTestRepository extends JpaRepository<SolverAbTestEntity, UUID> {

    Optional<SolverAbTestEntity> findByRuleIdAndStatus(UUID ruleId, String status);
}
