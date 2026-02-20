package io.ruleforge.management.repository;

import io.ruleforge.management.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, UUID> {

    List<TagEntity> findByOrgId(UUID orgId);

    Optional<TagEntity> findByOrgIdAndName(UUID orgId, String name);
}
