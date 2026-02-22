package io.ruleforge.management.repository;

import io.ruleforge.management.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, UUID> {

    List<FolderEntity> findBySpaceId(UUID spaceId);

    List<FolderEntity> findBySpaceIdAndParentId(UUID spaceId, UUID parentId);

    List<FolderEntity> findBySpaceIdAndParentIdIsNull(UUID spaceId);

    List<FolderEntity> findBySpaceIdIn(Collection<UUID> spaceIds);
}
