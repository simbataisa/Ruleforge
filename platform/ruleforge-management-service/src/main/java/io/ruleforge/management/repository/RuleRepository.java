package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {

    Page<RuleEntity> findBySpaceId(UUID spaceId, Pageable pageable);

    Page<RuleEntity> findBySpaceIdAndFolderId(UUID spaceId, UUID folderId, Pageable pageable);

    Optional<RuleEntity> findByAlias(String alias);

    Optional<RuleEntity> findFirstByOrderByCreatedAtAsc();

    Page<RuleEntity> findBySpaceIdAndStatus(UUID spaceId, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdAndFolderIdAndStatus(UUID spaceId, UUID folderId, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdIn(Collection<UUID> spaceIds, Pageable pageable);

    Page<RuleEntity> findBySpaceIdInAndStatus(Collection<UUID> spaceIds, String status, Pageable pageable);

    Page<RuleEntity> findBySpaceIdInAndFolderId(Collection<UUID> spaceIds, UUID folderId, Pageable pageable);

    @Query("""
        SELECT r FROM RuleEntity r
        WHERE r.spaceId IN :spaceIds
          AND (:folderId IS NULL OR r.folderId = :folderId)
          AND (:typesEmpty = true OR r.type IN :types)
          AND (:statusesEmpty = true OR r.status IN :statuses)
        """)
    Page<RuleEntity> findByFilters(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("folderId") UUID folderId,
        @Param("types") Collection<String> types,
        @Param("typesEmpty") boolean typesEmpty,
        @Param("statuses") Collection<String> statuses,
        @Param("statusesEmpty") boolean statusesEmpty,
        Pageable pageable
    );

    @Query("""
        SELECT r FROM RuleEntity r
        WHERE r.spaceId IN :spaceIds
          AND (:folderId IS NULL OR r.folderId = :folderId)
          AND (:typesEmpty = true OR r.type IN :types)
          AND (:statusesEmpty = true OR r.status IN :statuses)
          AND lower(r.name) LIKE lower(concat('%', :search, '%'))
        """)
    Page<RuleEntity> findByFiltersAndSearch(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("folderId") UUID folderId,
        @Param("types") Collection<String> types,
        @Param("typesEmpty") boolean typesEmpty,
        @Param("statuses") Collection<String> statuses,
        @Param("statusesEmpty") boolean statusesEmpty,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Advanced search query supporting multi-field text search, tag containment,
     * date range, creator filter, and version status filter.
     */
    @Query(value = """
        SELECT r.* FROM rules r
        WHERE r.space_id IN (:spaceIds)
          AND (:folderId IS NULL OR r.folder_id = CAST(:folderId AS uuid))
          AND (:typesEmpty = true OR r.type = ANY(CAST(:types AS text[])))
          AND (:statusesEmpty = true OR r.status = ANY(CAST(:statuses AS text[])))
          AND (:search IS NULL OR (
               lower(r.name) LIKE lower(concat('%', :search, '%'))
               OR lower(COALESCE(r.description, '')) LIKE lower(concat('%', :search, '%'))
               OR lower(r.alias) LIKE lower(concat('%', :search, '%'))
          ))
          AND (:createdBy IS NULL OR r.created_by = CAST(:createdBy AS uuid))
          AND (CAST(:updatedAfter AS timestamptz) IS NULL OR r.updated_at >= CAST(:updatedAfter AS timestamptz))
          AND (CAST(:updatedBefore AS timestamptz) IS NULL OR r.updated_at <= CAST(:updatedBefore AS timestamptz))
          AND (:tagsEmpty = true OR r.tags @> CAST(:tags AS text[]))
          AND (:hasVersion IS NULL OR EXISTS (
               SELECT 1 FROM rule_versions rv
               WHERE rv.rule_id = r.id AND rv.status = :hasVersion
          ))
        """,
        countQuery = """
        SELECT count(*) FROM rules r
        WHERE r.space_id IN (:spaceIds)
          AND (:folderId IS NULL OR r.folder_id = CAST(:folderId AS uuid))
          AND (:typesEmpty = true OR r.type = ANY(CAST(:types AS text[])))
          AND (:statusesEmpty = true OR r.status = ANY(CAST(:statuses AS text[])))
          AND (:search IS NULL OR (
               lower(r.name) LIKE lower(concat('%', :search, '%'))
               OR lower(COALESCE(r.description, '')) LIKE lower(concat('%', :search, '%'))
               OR lower(r.alias) LIKE lower(concat('%', :search, '%'))
          ))
          AND (:createdBy IS NULL OR r.created_by = CAST(:createdBy AS uuid))
          AND (CAST(:updatedAfter AS timestamptz) IS NULL OR r.updated_at >= CAST(:updatedAfter AS timestamptz))
          AND (CAST(:updatedBefore AS timestamptz) IS NULL OR r.updated_at <= CAST(:updatedBefore AS timestamptz))
          AND (:tagsEmpty = true OR r.tags @> CAST(:tags AS text[]))
          AND (:hasVersion IS NULL OR EXISTS (
               SELECT 1 FROM rule_versions rv
               WHERE rv.rule_id = r.id AND rv.status = :hasVersion
          ))
        """,
        nativeQuery = true)
    Page<RuleEntity> findByAdvancedFilters(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("folderId") UUID folderId,
        @Param("search") String search,
        @Param("types") String[] types,
        @Param("typesEmpty") boolean typesEmpty,
        @Param("statuses") String[] statuses,
        @Param("statusesEmpty") boolean statusesEmpty,
        @Param("createdBy") UUID createdBy,
        @Param("updatedAfter") Instant updatedAfter,
        @Param("updatedBefore") Instant updatedBefore,
        @Param("tags") String[] tags,
        @Param("tagsEmpty") boolean tagsEmpty,
        @Param("hasVersion") String hasVersion,
        Pageable pageable
    );
}
