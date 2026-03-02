package io.ruleforge.scheduler.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "schedules")
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(nullable = false)
    private String name;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_template", columnDefinition = "jsonb")
    private Map<String, Object> inputTemplate = new HashMap<>();

    @Column(nullable = false)
    private boolean enabled = true;

    private String description;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "rule_type")
    private String ruleType;

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
    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public Map<String, Object> getInputTemplate() { return inputTemplate; }
    public void setInputTemplate(Map<String, Object> inputTemplate) { this.inputTemplate = inputTemplate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public UUID getWorkflowId() { return workflowId; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
    public UUID getHolidayCalendarId() { return holidayCalendarId; }
    public void setHolidayCalendarId(UUID holidayCalendarId) { this.holidayCalendarId = holidayCalendarId; }
    public String getSkipOnHoliday() { return skipOnHoliday; }
    public void setSkipOnHoliday(String skipOnHoliday) { this.skipOnHoliday = skipOnHoliday; }
}
