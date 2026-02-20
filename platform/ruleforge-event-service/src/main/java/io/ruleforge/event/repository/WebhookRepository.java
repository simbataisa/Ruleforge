package io.ruleforge.event.repository;

import io.ruleforge.event.entity.WebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<WebhookEntity, UUID> {

    List<WebhookEntity> findBySpaceIdAndActiveTrue(UUID spaceId);

    List<WebhookEntity> findBySpaceId(UUID spaceId);

    Optional<WebhookEntity> findByIdAndSpaceId(UUID id, UUID spaceId);
}
