package io.ruleforge.scheduler.workflow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "workflows")
public class WorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> definition;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType = "CRON";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private Map<String, Object> triggerConfig = new HashMap<>();

    @Column(name = "holiday_calendar_id")
    private UUID holidayCalendarId;

    @Column(name = "skip_on_holiday", nullable = false)
    private String skipOnHoliday = "SKIP";

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
    public Map<String, Object> getDefinition() { return definition; }
    public void setDefinition(Map<String, Object> definition) { this.definition = definition; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public Map<String, Object> getTriggerConfig() { return triggerConfig; }
    public void setTriggerConfig(Map<String, Object> triggerConfig) { this.triggerConfig = triggerConfig; }
    public UUID getHolidayCalendarId() { return holidayCalendarId; }
    public void setHolidayCalendarId(UUID holidayCalendarId) { this.holidayCalendarId = holidayCalendarId; }
    public String getSkipOnHoliday() { return skipOnHoliday; }
    public void setSkipOnHoliday(String skipOnHoliday) { this.skipOnHoliday = skipOnHoliday; }
}
