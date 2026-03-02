package io.ruleforge.scheduler.controller;

import io.ruleforge.scheduler.dto.*;
import io.ruleforge.scheduler.service.HolidayCalendarService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/holiday-calendars")
public class HolidayCalendarController {

    private final HolidayCalendarService holidayCalendarService;

    public HolidayCalendarController(HolidayCalendarService holidayCalendarService) {
        this.holidayCalendarService = holidayCalendarService;
    }

    @PostMapping
    public ResponseEntity<HolidayCalendarDto> create(@Valid @RequestBody CreateHolidayCalendarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayCalendarService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<HolidayCalendarDto>> list(@RequestParam(required = false) String market) {
        return ResponseEntity.ok(holidayCalendarService.findAll(market));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayCalendarDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(holidayCalendarService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolidayCalendarDto> update(@PathVariable UUID id,
                                                      @RequestBody UpdateHolidayCalendarRequest request) {
        return ResponseEntity.ok(holidayCalendarService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        holidayCalendarService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entries")
    public ResponseEntity<HolidayEntryDto> addEntry(@PathVariable UUID id,
                                                     @Valid @RequestBody CreateHolidayEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayCalendarService.addEntry(id, request));
    }

    @GetMapping("/{id}/entries")
    public ResponseEntity<List<HolidayEntryDto>> getEntries(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(holidayCalendarService.getEntries(id, from, to));
    }

    @PostMapping("/{id}/entries/bulk")
    public ResponseEntity<BulkImportResultDto> bulkImportEntries(
            @PathVariable UUID id,
            @RequestBody List<CreateHolidayEntryRequest> entries) {
        return ResponseEntity.ok(holidayCalendarService.bulkImportEntries(id, entries));
    }

    @PutMapping("/{id}/entries/{entryId}")
    public ResponseEntity<HolidayEntryDto> updateEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @RequestBody UpdateHolidayEntryRequest request) {
        return ResponseEntity.ok(holidayCalendarService.updateEntry(id, entryId, request));
    }

    @DeleteMapping("/{id}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable UUID id, @PathVariable UUID entryId) {
        holidayCalendarService.deleteEntry(id, entryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/check")
    public ResponseEntity<Map<String, Object>> checkDate(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(holidayCalendarService.checkDate(id, date));
    }
}
