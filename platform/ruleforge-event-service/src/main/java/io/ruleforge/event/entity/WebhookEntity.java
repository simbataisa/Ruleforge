package io.ruleforge.event.entity;

import io.ruleforge.event.model.WebhookAuthType;
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

import java.math.BigDecimal;
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

    @Column(name = "auth_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WebhookAuthType authType = WebhookAuthType.SIGNING_SECRET;

    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    @Column(name = "retry_initial_delay_seconds", nullable = false)
    private int retryInitialDelaySeconds = 30;

    @Column(name = "retry_backoff_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal retryBackoffMultiplier = new BigDecimal("2.0");

    @Column(name = "retry_max_attempts", nullable = false)
    private int retryMaxAttempts = 5;

    @Column(name = "retry_on_status_codes", nullable = false)
    private String retryOnStatusCodes = "429,408,500,502,503,504";

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

    public WebhookAuthType getAuthType() { return authType; }
    public void setAuthType(WebhookAuthType authType) { this.authType = authType; }

    /** Never log this value — it contains encrypted credentials. */
    public String getAuthConfig() { return authConfig; }
    public void setAuthConfig(String authConfig) { this.authConfig = authConfig; }

    public int getRetryInitialDelaySeconds() { return retryInitialDelaySeconds; }
    public void setRetryInitialDelaySeconds(int retryInitialDelaySeconds) { this.retryInitialDelaySeconds = retryInitialDelaySeconds; }

    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }

    public int getRetryMaxAttempts() { return retryMaxAttempts; }
    public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }

    public String getRetryOnStatusCodes() { return retryOnStatusCodes; }
    public void setRetryOnStatusCodes(String retryOnStatusCodes) { this.retryOnStatusCodes = retryOnStatusCodes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "WebhookEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", scopeType=" + scopeType +
                ", scopeId=" + scopeId +
                ", orgId=" + orgId +
                ", url='" + url + '\'' +
                ", active=" + active +
                ", authType=" + authType +
                ", consecutiveFailures=" + consecutiveFailures +
                ", circuitOpenedAt=" + circuitOpenedAt +
                ", lastDeliveredAt=" + lastDeliveredAt +
                ", retryMaxAttempts=" + retryMaxAttempts +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
        // authConfig and secret intentionally excluded — they contain encrypted credentials
    }
}
