package io.ruleforge.management.repository;

import io.ruleforge.management.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, UUID> {

    List<MemberEntity> findByOrgIdAndRoleIn(UUID orgId, Collection<String> roles);
}
