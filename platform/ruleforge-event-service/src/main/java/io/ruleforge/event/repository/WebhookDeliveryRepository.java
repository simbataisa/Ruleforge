package io.ruleforge.event.repository;

import io.ruleforge.event.entity.WebhookDeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, UUID> {

    Page<WebhookDeliveryEntity> findByWebhookIdOrderByDeliveredAtDesc(UUID webhookId, Pageable pageable);
}
