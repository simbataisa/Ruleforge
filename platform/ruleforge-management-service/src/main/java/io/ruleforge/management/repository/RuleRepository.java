package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {

    Page<RuleEntity> findBySpaceId(UUID spaceId, Pageable pageable);

    Page<RuleEntity> findBySpaceIdAndFolderId(UUID spaceId, UUID folderId, Pageable pageable);

    Optional<RuleEntity> findByAlias(String alias);

    Page<RuleEntity> findBySpaceIdAndStatus(UUID spaceId, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdAndFolderIdAndStatus(UUID spaceId, UUID folderId, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdIn(Collection<UUID> spaceIds, Pageable pageable);

    Page<RuleEntity> findBySpaceIdInAndStatus(Collection<UUID> spaceIds, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdInAndFolderId(Collection<UUID> spaceIds, UUID folderId, Pageable pageable);

    @Query("""
        SELECT r FROM RuleEntity r
        WHERE r.spaceId IN :spaceIds
          AND (:folderId IS NULL OR r.folderId = :folderId)
          AND (:status IS NULL OR r.status = :status)
          AND (:type IS NULL OR r.type = :type)
        """)
    Page<RuleEntity> findByFilters(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("folderId") UUID folderId,
        @Param("status") String status,
        @Param("type") String type,
        Pageable pageable
    );

    @Query("""
        SELECT r FROM RuleEntity r
        WHERE r.spaceId IN :spaceIds
          AND (:folderId IS NULL OR r.folderId = :folderId)
          AND (:status IS NULL OR r.status = :status)
          AND (:type IS NULL OR r.type = :type)
          AND lower(r.name) LIKE lower(concat('%', :search, '%'))
        """)
    Page<RuleEntity> findByFiltersAndSearch(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("folderId") UUID folderId,
        @Param("status") String status,
        @Param("type") String type,
        @Param("search") String search,
        Pageable pageable
    );
}
