package io.ruleforge.event.repository;

import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.model.WebhookScopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<WebhookEntity, UUID> {

    List<WebhookEntity> findByScopeTypeAndScopeId(WebhookScopeType scopeType, UUID scopeId);

    List<WebhookEntity> findByOrgId(UUID orgId);

    List<WebhookEntity> findByOrgIdAndActiveTrue(UUID orgId);

    Optional<WebhookEntity> findByIdAndOrgId(UUID id, UUID orgId);

    List<WebhookEntity> findByAuthTypeAndAuthConfigIsNullAndSecretIsNotNull(WebhookAuthType authType);
}
