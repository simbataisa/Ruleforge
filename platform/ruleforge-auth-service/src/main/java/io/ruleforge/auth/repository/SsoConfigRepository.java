package io.ruleforge.auth.repository;

import io.ruleforge.auth.entity.SsoConfigEntity;
import io.ruleforge.auth.entity.SsoProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SsoConfigRepository extends JpaRepository<SsoConfigEntity, UUID> {
    List<SsoConfigEntity> findByOrgId(UUID orgId);
    Optional<SsoConfigEntity> findByOrgIdAndProviderType(UUID orgId, SsoProviderType providerType);
    Optional<SsoConfigEntity> findByOrgIdAndEnabledTrue(UUID orgId);
}
