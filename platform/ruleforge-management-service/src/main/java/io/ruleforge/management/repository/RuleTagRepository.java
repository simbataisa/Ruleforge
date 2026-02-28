package io.ruleforge.management.repository;

import io.ruleforge.management.entity.RuleTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleTagRepository extends JpaRepository<RuleTagEntity, UUID> {

    List<RuleTagEntity> findByRuleId(UUID ruleId);

    Optional<RuleTagEntity> findByRuleIdAndTag(UUID ruleId, String tag);

    void deleteByRuleIdAndTag(UUID ruleId, String tag);

    void deleteByRuleId(UUID ruleId);

    @Query(value = """
        SELECT rt.tag, COUNT(*) as rule_count
        FROM rule_tags rt
        JOIN rules r ON r.id = rt.rule_id
        JOIN spaces s ON s.id = r.space_id
        WHERE s.org_id = :orgId
          AND (:prefix IS NULL OR lower(rt.tag) LIKE lower(concat(:prefix, '%')))
        GROUP BY rt.tag
        ORDER BY rule_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTagsWithCount(
        @Param("orgId") UUID orgId,
        @Param("prefix") String prefix,
        @Param("limit") int limit
    );
}
