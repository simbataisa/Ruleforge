package io.ruleforge.event.controller;

import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.dto.AuditEventDto;
import io.ruleforge.event.service.AuditEventService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    private final AuditEventService auditEventService;

    public AuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditEventDto>> listEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID orgId = TenantContext.get().orgId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AuditEventDto> events = auditEventService.getEvents(orgId, eventType, pageable);
        return ApiResponse.ok(events);
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    public ApiResponse<PageResponse<AuditEventDto>> getResourceEvents(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID orgId = TenantContext.get().orgId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AuditEventDto> events = auditEventService.getResourceEvents(
                orgId, resourceType, resourceId, pageable);
        return ApiResponse.ok(events);
    }
}
