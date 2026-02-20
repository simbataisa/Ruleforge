package io.ruleforge.auth.repository;

import io.ruleforge.auth.entity.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, UUID> {

    List<SpaceEntity> findByOrgId(UUID orgId);
}
