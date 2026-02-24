package io.ruleforge.management.repository;

import io.ruleforge.management.entity.SampleInputEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SampleInputRepository extends JpaRepository<SampleInputEntity, UUID> {

    List<SampleInputEntity> findByRuleIdOrderBySortOrder(UUID ruleId);

    Optional<SampleInputEntity> findByRuleIdAndName(UUID ruleId, String name);

    int countByRuleId(UUID ruleId);
}
