package io.ruleforge.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sso_configs")
public class SsoConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "provider_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SsoProviderType providerType;

    @Column(name = "metadata_url")
    private String metadataUrl;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret_encrypted")
    private String clientSecretEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_mapping", columnDefinition = "jsonb")
    private Map<String, String> attributeMapping = new HashMap<>();

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public SsoProviderType getProviderType() { return providerType; }
    public void setProviderType(SsoProviderType providerType) { this.providerType = providerType; }

    public String getMetadataUrl() { return metadataUrl; }
    public void setMetadataUrl(String metadataUrl) { this.metadataUrl = metadataUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecretEncrypted() { return clientSecretEncrypted; }
    public void setClientSecretEncrypted(String clientSecretEncrypted) { this.clientSecretEncrypted = clientSecretEncrypted; }

    public Map<String, String> getAttributeMapping() { return attributeMapping; }
    public void setAttributeMapping(Map<String, String> attributeMapping) { this.attributeMapping = attributeMapping; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
