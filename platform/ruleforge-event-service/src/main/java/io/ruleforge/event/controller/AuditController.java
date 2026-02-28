package io.ruleforge.event.controller;

import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.dto.AuditEventDto;
import io.ruleforge.event.dto.AuditQueryFilter;
import io.ruleforge.event.service.AuditEventService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
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
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        AuditQueryFilter filter = new AuditQueryFilter(
                tenant.orgId(), eventType, resourceType, null, actorId, search, from, to);
        return ApiResponse.ok(auditEventService.getEvents(filter, pageable));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void exportCsv(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletResponse response) throws IOException {
        TenantContext.TenantInfo tenant = TenantContext.get();
        AuditQueryFilter filter = new AuditQueryFilter(
                tenant.orgId(), eventType, resourceType, null, actorId, search, from, to);
        Pageable pageable = Pageable.ofSize(10000);
        PageResponse<AuditEventDto> events = auditEventService.getEvents(filter, pageable);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-log.csv");

        PrintWriter writer = response.getWriter();
        writer.println("id,createdAt,eventType,actorName,resourceType,resourceId,resourceName");
        for (AuditEventDto event : events.content()) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    event.id() != null ? event.id() : "",
                    event.createdAt() != null ? event.createdAt().toString() : "",
                    csvEscape(event.eventType()),
                    csvEscape(event.actorName()),
                    csvEscape(event.resourceType()),
                    csvEscape(event.resourceId()),
                    csvEscape(event.resourceName()));
        }
        writer.flush();
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    public ApiResponse<PageResponse<AuditEventDto>> getResourceEvents(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID orgId = TenantContext.get().orgId();
        PageResponse<AuditEventDto> events = auditEventService.getResourceEvents(
                orgId, resourceType, resourceId, Pageable.ofSize(size).withPage(page));
        return ApiResponse.ok(events);
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
