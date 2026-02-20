package io.ruleforge.management.controller;

import io.ruleforge.management.dto.ImportResult;
import io.ruleforge.management.service.ImportExportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/rules")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importRules(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID spaceId,
            @RequestParam UUID createdBy) throws Exception {
        ImportResult result = importExportService.importRulesFromJson(
            file.getBytes(), spaceId, createdBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportRule(
            @PathVariable UUID id,
            @RequestParam UUID spaceId,
            @RequestParam(defaultValue = "JSON") String format) throws Exception {
        byte[] data = importExportService.exportRuleToJson(id, spaceId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("Content-Disposition", "attachment; filename=\"rule-" + id + ".json\"")
            .body(data);
    }

    @PostMapping("/export-bulk")
    public ResponseEntity<byte[]> exportBulk(
            @RequestBody List<UUID> ruleIds,
            @RequestParam UUID spaceId) throws Exception {
        byte[] data = importExportService.exportBulkToJson(ruleIds, spaceId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("Content-Disposition", "attachment; filename=\"rules-export.json\"")
            .body(data);
    }
}
