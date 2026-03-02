package io.ruleforge.scheduler.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "holiday_calendars")
public class HolidayCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(nullable = false, length = 10)
    private String market = "GLOBAL";

    @Column(length = 50)
    private String region;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weekend_days", columnDefinition = "jsonb", nullable = false)
    private List<String> weekendDays = List.of("SATURDAY", "SUNDAY");

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

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
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public List<String> getWeekendDays() { return weekendDays; }
    public void setWeekendDays(List<String> weekendDays) { this.weekendDays = weekendDays; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
