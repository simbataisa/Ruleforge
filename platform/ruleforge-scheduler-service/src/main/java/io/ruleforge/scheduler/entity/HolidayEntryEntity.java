package io.ruleforge.scheduler.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "holiday_entries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"calendar_id", "date"})
})
public class HolidayEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "calendar_id", nullable = false)
    private UUID calendarId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType = "PUBLIC_HOLIDAY";

    @Column(nullable = false)
    private boolean recurring = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCalendarId() { return calendarId; }
    public void setCalendarId(UUID calendarId) { this.calendarId = calendarId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public Instant getCreatedAt() { return createdAt; }
}
