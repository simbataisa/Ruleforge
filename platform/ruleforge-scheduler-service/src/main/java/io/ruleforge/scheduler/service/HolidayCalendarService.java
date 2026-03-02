package io.ruleforge.scheduler.service;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.scheduler.dto.*;
import io.ruleforge.scheduler.entity.HolidayCalendarEntity;
import io.ruleforge.scheduler.entity.HolidayEntryEntity;
import io.ruleforge.scheduler.repository.HolidayCalendarRepository;
import io.ruleforge.scheduler.repository.HolidayEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class HolidayCalendarService {

    private final HolidayCalendarRepository calendarRepository;
    private final HolidayEntryRepository entryRepository;

    public HolidayCalendarService(HolidayCalendarRepository calendarRepository,
                                   HolidayEntryRepository entryRepository) {
        this.calendarRepository = calendarRepository;
        this.entryRepository = entryRepository;
    }

    public HolidayCalendarDto create(CreateHolidayCalendarRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        UUID spaceId = request.spaceId() != null ? request.spaceId()
                : tenant.spaceId() != null ? tenant.spaceId()
                : UUID.fromString("d0000000-0000-0000-0000-000000000001");

        HolidayCalendarEntity entity = new HolidayCalendarEntity();
        entity.setSpaceId(spaceId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setMarket(request.market() != null ? request.market() : "GLOBAL");
        entity.setRegion(request.region());
        if (request.weekendDays() != null) {
            entity.setWeekendDays(request.weekendDays());
        }
        entity.setCreatedBy(request.createdBy() != null ? request.createdBy() : tenant.userId());
        HolidayCalendarEntity saved = calendarRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public HolidayCalendarDto findById(UUID id) {
        return calendarRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Holiday calendar not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendarDto> findAll(String market) {
        List<HolidayCalendarEntity> calendars;
        if (market != null && !market.isBlank()) {
            calendars = calendarRepository.findByMarket(market);
        } else {
            calendars = calendarRepository.findAll();
        }
        return calendars.stream().map(this::toDto).collect(Collectors.toList());
    }

    public HolidayCalendarDto update(UUID id, UpdateHolidayCalendarRequest request) {
        HolidayCalendarEntity entity = calendarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday calendar not found: " + id));
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.market() != null) entity.setMarket(request.market());
        if (request.region() != null) entity.setRegion(request.region());
        if (request.weekendDays() != null) entity.setWeekendDays(request.weekendDays());
        if (request.enabled() != null) entity.setEnabled(request.enabled());
        return toDto(calendarRepository.save(entity));
    }

    public void delete(UUID id) {
        calendarRepository.deleteById(id);
    }

    // --- Entry operations ---

    public HolidayEntryDto addEntry(UUID calendarId, CreateHolidayEntryRequest request) {
        calendarRepository.findById(calendarId)
                .orElseThrow(() -> new RuntimeException("Holiday calendar not found: " + calendarId));

        HolidayEntryEntity entity = new HolidayEntryEntity();
        entity.setCalendarId(calendarId);
        entity.setDate(request.date());
        entity.setName(request.name());
        entity.setEntryType(request.entryType() != null ? request.entryType() : "PUBLIC_HOLIDAY");
        entity.setRecurring(request.recurring() != null && request.recurring());
        return toEntryDto(entryRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<HolidayEntryDto> getEntries(UUID calendarId, LocalDate from, LocalDate to) {
        List<HolidayEntryEntity> entries;
        if (from != null && to != null) {
            entries = entryRepository.findByCalendarIdAndDateBetween(calendarId, from, to);
        } else {
            entries = entryRepository.findByCalendarId(calendarId);
        }
        return entries.stream().map(this::toEntryDto).collect(Collectors.toList());
    }

    public void deleteEntry(UUID calendarId, UUID entryId) {
        entryRepository.deleteById(entryId);
    }

    // --- Check operations ---

    @Transactional(readOnly = true)
    public Map<String, Object> checkDate(UUID calendarId, LocalDate date) {
        HolidayCalendarEntity calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new RuntimeException("Holiday calendar not found: " + calendarId));

        boolean isHoliday = entryRepository.existsByCalendarIdAndDate(calendarId, date);
        boolean isWeekend = isWeekend(calendar, date);
        boolean isBusinessDay = !isHoliday && !isWeekend;

        String holidayName = null;
        if (isHoliday) {
            holidayName = entryRepository.findByCalendarIdAndDateBetween(calendarId, date, date)
                    .stream().findFirst().map(HolidayEntryEntity::getName).orElse(null);
        }

        return Map.of(
                "date", date.toString(),
                "isHoliday", isHoliday,
                "isWeekend", isWeekend,
                "isBusinessDay", isBusinessDay,
                "holidayName", holidayName != null ? holidayName : "",
                "calendarName", calendar.getName()
        );
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(UUID calendarId, LocalDate date) {
        return entryRepository.existsByCalendarIdAndDate(calendarId, date);
    }

    @Transactional(readOnly = true)
    public boolean isWeekend(UUID calendarId, LocalDate date) {
        HolidayCalendarEntity calendar = calendarRepository.findById(calendarId).orElse(null);
        if (calendar == null) return false;
        return isWeekend(calendar, date);
    }

    private boolean isWeekend(HolidayCalendarEntity calendar, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayName = dayOfWeek.name();
        return calendar.getWeekendDays() != null && calendar.getWeekendDays().contains(dayName);
    }

    private HolidayCalendarDto toDto(HolidayCalendarEntity e) {
        long entryCount = entryRepository.findByCalendarId(e.getId()).size();
        return new HolidayCalendarDto(e.getId(), e.getSpaceId(), e.getName(), e.getDescription(),
                e.getMarket(), e.getRegion(), e.getWeekendDays(), e.isEnabled(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt(), entryCount);
    }

    private HolidayEntryDto toEntryDto(HolidayEntryEntity e) {
        return new HolidayEntryDto(e.getId(), e.getCalendarId(), e.getDate(),
                e.getName(), e.getEntryType(), e.isRecurring(), e.getCreatedAt());
    }
}
