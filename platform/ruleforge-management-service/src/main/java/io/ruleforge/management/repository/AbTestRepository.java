package io.ruleforge.management.repository;

import io.ruleforge.management.entity.AbTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbTestRepository extends JpaRepository<AbTestEntity, UUID> {

    List<AbTestEntity> findByRuleId(UUID ruleId);

    List<AbTestEntity> findBySpaceId(UUID spaceId);

    Optional<AbTestEntity> findByRuleIdAndStatus(UUID ruleId, String status);
}
