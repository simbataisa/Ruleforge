package io.ruleforge.scheduler.controller;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.scheduler.dto.*;
import io.ruleforge.scheduler.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ScheduleDto> create(@Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ScheduleDto>> list(@RequestParam(required = false) UUID spaceId) {
        if (spaceId == null) {
            return ResponseEntity.ok(scheduleService.findAll());
        }
        return ResponseEntity.ok(scheduleService.findBySpaceId(spaceId));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of("success", true, "data", scheduleService.getStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleDto> update(@PathVariable UUID id,
                                               @RequestBody UpdateScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ScheduleDto> pause(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ScheduleDto> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.resume(id));
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<ScheduleRunDto> trigger(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.triggerManually(id));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<Page<ScheduleRunDto>> getRuns(@PathVariable UUID id,
                                                         Pageable pageable) {
        return ResponseEntity.ok(scheduleService.getRuns(id, pageable));
    }
}
