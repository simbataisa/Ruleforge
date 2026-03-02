package io.ruleforge.scheduler.repository;

import io.ruleforge.scheduler.entity.HolidayEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayEntryRepository extends JpaRepository<HolidayEntryEntity, UUID> {

    List<HolidayEntryEntity> findByCalendarId(UUID calendarId);

    List<HolidayEntryEntity> findByCalendarIdAndDateBetween(UUID calendarId, LocalDate start, LocalDate end);

    boolean existsByCalendarIdAndDate(UUID calendarId, LocalDate date);

    void deleteByCalendarId(UUID calendarId);
}
