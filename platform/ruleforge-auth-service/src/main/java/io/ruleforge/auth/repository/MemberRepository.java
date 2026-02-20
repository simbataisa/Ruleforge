package io.ruleforge.auth.repository;

import io.ruleforge.auth.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, UUID> {

    Optional<MemberEntity> findByOrgIdAndUserId(UUID orgId, UUID userId);

    List<MemberEntity> findByUserId(UUID userId);

    List<MemberEntity> findByOrgId(UUID orgId);

    boolean existsByOrgIdAndUserId(UUID orgId, UUID userId);
}
