package io.ruleforge.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "space_approval_settings")
public class SpaceApprovalSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "space_id", nullable = false, unique = true)
    private UUID spaceId;

    @Column(name = "default_approvers", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private UUID[] defaultApprovers = {};

    @Column(name = "sla_hours", nullable = false)
    private int slaHours = 48;

    @Column(name = "require_all_approvers", nullable = false)
    private boolean requireAllApprovers = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(UUID spaceId) {
        this.spaceId = spaceId;
    }

    public UUID[] getDefaultApprovers() {
        return defaultApprovers;
    }

    public void setDefaultApprovers(UUID[] defaultApprovers) {
        this.defaultApprovers = defaultApprovers;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(int slaHours) {
        this.slaHours = slaHours;
    }

    public boolean isRequireAllApprovers() {
        return requireAllApprovers;
    }

    public void setRequireAllApprovers(boolean requireAllApprovers) {
        this.requireAllApprovers = requireAllApprovers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
