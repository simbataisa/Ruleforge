package io.ruleforge.auth.repository;

import io.ruleforge.auth.entity.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, UUID> {

    List<SpaceEntity> findByOrgId(UUID orgId);

    @Query(value = "SELECT COUNT(*) FROM rules WHERE space_id = :spaceId", nativeQuery = true)
    long countRulesBySpaceId(@Param("spaceId") UUID spaceId);

    @Query(value = """
            SELECT r.space_id as spaceId, COUNT(*) as ruleCount
            FROM rules r
            WHERE r.space_id IN :spaceIds
            GROUP BY r.space_id
            """, nativeQuery = true)
    List<Object[]> countRulesBySpaceIds(@Param("spaceIds") List<UUID> spaceIds);
}
