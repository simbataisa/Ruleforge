package io.ruleforge.solver.repository;

import io.ruleforge.solver.entity.SolverRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only repository for accessing rule metadata.
 * Used to resolve rule IDs and aliases for the solver.
 */
@Repository
public interface SolverRuleRepository extends JpaRepository<SolverRuleEntity, UUID> {

    Optional<SolverRuleEntity> findByAlias(String alias);

    Optional<SolverRuleEntity> findByIdAndSpaceId(UUID id, UUID spaceId);

    Optional<SolverRuleEntity> findByAliasAndSpaceId(String alias, UUID spaceId);
}
