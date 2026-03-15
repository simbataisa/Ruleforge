package io.ruleforge.event.entity;

import io.ruleforge.event.model.WebhookScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private WebhookScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "url", nullable = false)
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "events", columnDefinition = "jsonb", nullable = false)
    private String events;

    @Column(name = "secret", nullable = false)
    private String secret;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "circuit_opened_at")
    private OffsetDateTime circuitOpenedAt;

    @Column(name = "last_delivered_at")
    private OffsetDateTime lastDeliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebhookEntity() {}

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public WebhookScopeType getScopeType() { return scopeType; }
    public void setScopeType(WebhookScopeType scopeType) { this.scopeType = scopeType; }

    public UUID getScopeId() { return scopeId; }
    public void setScopeId(UUID scopeId) { this.scopeId = scopeId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public OffsetDateTime getCircuitOpenedAt() { return circuitOpenedAt; }
    public void setCircuitOpenedAt(OffsetDateTime circuitOpenedAt) { this.circuitOpenedAt = circuitOpenedAt; }

    public OffsetDateTime getLastDeliveredAt() { return lastDeliveredAt; }
    public void setLastDeliveredAt(OffsetDateTime lastDeliveredAt) { this.lastDeliveredAt = lastDeliveredAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
