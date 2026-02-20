package io.ruleforge.connector.repository;

import io.ruleforge.connector.entity.ConnectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectorRepository extends JpaRepository<ConnectorEntity, UUID> {

    List<ConnectorEntity> findBySpaceId(UUID spaceId);

    List<ConnectorEntity> findBySpaceIdAndEnabledTrue(UUID spaceId);

    Optional<ConnectorEntity> findByIdAndSpaceId(UUID id, UUID spaceId);
}
