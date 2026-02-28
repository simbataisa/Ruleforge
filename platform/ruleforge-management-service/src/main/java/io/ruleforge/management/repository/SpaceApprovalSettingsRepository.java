package io.ruleforge.management.repository;

import io.ruleforge.management.entity.SpaceApprovalSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpaceApprovalSettingsRepository extends JpaRepository<SpaceApprovalSettingsEntity, UUID> {

    Optional<SpaceApprovalSettingsEntity> findBySpaceId(UUID spaceId);
}
