package io.ruleforge.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.management.dto.ExportFormat;
import io.ruleforge.management.dto.ImportResult;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ImportExportService {

    private final ObjectMapper objectMapper;
    private final RuleRepository ruleRepository;
    private final RuleVersionRepository ruleVersionRepository;

    public ImportExportService(ObjectMapper objectMapper,
                                RuleRepository ruleRepository,
                                RuleVersionRepository ruleVersionRepository) {
        this.objectMapper = objectMapper;
        this.ruleRepository = ruleRepository;
        this.ruleVersionRepository = ruleVersionRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportRuleToJson(UUID ruleId, UUID spaceId) throws Exception {
        RuleEntity rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        List<RuleVersionEntity> versions = ruleVersionRepository.findByRuleId(ruleId);

        Map<String, Object> exportData = buildRuleExportMap(rule, versions);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportData);
    }

    @Transactional(readOnly = true)
    public byte[] exportBulkToJson(List<UUID> ruleIds, UUID spaceId) throws Exception {
        List<Map<String, Object>> exportList = new ArrayList<>();
        for (UUID ruleId : ruleIds) {
            ruleRepository.findById(ruleId).ifPresent(rule -> {
                List<RuleVersionEntity> versions = ruleVersionRepository.findByRuleId(ruleId);
                exportList.add(buildRuleExportMap(rule, versions));
            });
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exportList);
    }

    public ImportResult importRulesFromJson(byte[] data, UUID spaceId, UUID createdBy) throws Exception {
        // If spaceId not provided, use the first available space from existing rules
        if (spaceId == null) {
            spaceId = ruleRepository.findFirstByOrderByCreatedAtAsc()
                    .map(RuleEntity::getSpaceId).orElse(null);
        }
        List<Map<String, Object>> ruleList;
        try {
            // Try parsing as array first
            ruleList = objectMapper.readValue(data, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            // Fall back to single object
            Map<String, Object> single = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
            ruleList = List.of(single);
        }

        int totalRules = ruleList.size();
        int importedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> ruleData : ruleList) {
            try {
                String alias = (String) ruleData.get("alias");
                // Skip if alias already exists in this space
                if (alias != null && ruleRepository.findByAlias(alias).isPresent()) {
                    skippedCount++;
                    errors.add("Skipped rule with alias '" + alias + "': already exists");
                    continue;
                }

                RuleEntity entity = new RuleEntity();
                entity.setSpaceId(spaceId);
                entity.setCreatedBy(createdBy);
                entity.setName((String) ruleData.getOrDefault("name", "Imported Rule"));
                entity.setAlias(alias != null ? alias : "imported-" + UUID.randomUUID().toString().substring(0, 8));
                entity.setType((String) ruleData.getOrDefault("type", "DECISION_TABLE"));
                entity.setStatus("DRAFT");
                entity.setLatestVersion(1);
                entity.setLocked(false);

                Object folderIdObj = ruleData.get("folderId");
                if (folderIdObj != null) {
                    entity.setFolderId(UUID.fromString(folderIdObj.toString()));
                }

                RuleEntity savedRule = ruleRepository.save(entity);

                // Create initial version from exported definition
                RuleVersionEntity version = new RuleVersionEntity();
                version.setRuleId(savedRule.getId());
                version.setVersion(1);
                version.setStatus("DRAFT");
                version.setCreatedBy(createdBy);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> versions = (List<Map<String, Object>>) ruleData.get("versions");
                if (versions != null && !versions.isEmpty()) {
                    Map<String, Object> latestVersion = versions.get(versions.size() - 1);
                    Object definitionObj = latestVersion.get("definition");
                    if (definitionObj != null) {
                        version.setDefinition(objectMapper.writeValueAsString(definitionObj));
                    }
                    Object inputModelObj = latestVersion.get("inputModel");
                    if (inputModelObj != null) {
                        version.setInputModel(objectMapper.writeValueAsString(inputModelObj));
                    }
                    Object outputModelObj = latestVersion.get("outputModel");
                    if (outputModelObj != null) {
                        version.setOutputModel(objectMapper.writeValueAsString(outputModelObj));
                    }
                    Object changeNoteObj = latestVersion.get("changeNote");
                    if (changeNoteObj != null) {
                        version.setChangeNote(changeNoteObj.toString());
                    }
                }

                ruleVersionRepository.save(version);
                importedCount++;
            } catch (Exception ex) {
                failedCount++;
                errors.add("Failed to import rule: " + ex.getMessage());
            }
        }

        return new ImportResult(totalRules, importedCount, skippedCount, failedCount, errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportRuleToExcel(UUID ruleId, UUID spaceId) throws Exception {
        RuleEntity rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        List<RuleVersionEntity> versions = ruleVersionRepository.findByRuleId(ruleId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);

            // Sheet 1: Rule Metadata
            Sheet metadataSheet = workbook.createSheet("Metadata");
            writeMetadataSheet(metadataSheet, rule, headerStyle);

            // Sheet 2: Versions
            Sheet versionsSheet = workbook.createSheet("Versions");
            writeVersionsSheet(versionsSheet, versions, headerStyle);

            // Sheet 3: Definition (for the latest version)
            if (!versions.isEmpty()) {
                RuleVersionEntity latestVersion = versions.get(versions.size() - 1);
                if (latestVersion.getDefinition() != null && !latestVersion.getDefinition().isBlank()) {
                    Sheet definitionSheet = workbook.createSheet("Definition");
                    writeDefinitionSheet(definitionSheet, rule.getType(), latestVersion.getDefinition(), headerStyle);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void writeMetadataSheet(Sheet sheet, RuleEntity rule, CellStyle headerStyle) {
        String[][] metadata = {
            {"Property", "Value"},
            {"ID", rule.getId().toString()},
            {"Name", rule.getName()},
            {"Alias", rule.getAlias()},
            {"Type", rule.getType()},
            {"Status", rule.getStatus()},
            {"Latest Version", String.valueOf(rule.getLatestVersion())},
            {"Locked", String.valueOf(rule.isLocked())},
            {"Created At", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : ""},
            {"Updated At", rule.getUpdatedAt() != null ? rule.getUpdatedAt().toString() : ""}
        };

        for (int i = 0; i < metadata.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < metadata[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(metadata[i][j]);
                if (i == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeVersionsSheet(Sheet sheet, List<RuleVersionEntity> versions, CellStyle headerStyle) {
        String[] headers = {"Version", "Status", "Change Note", "Created At", "Published At"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < versions.size(); i++) {
            RuleVersionEntity v = versions.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(v.getVersion());
            row.createCell(1).setCellValue(v.getStatus());
            row.createCell(2).setCellValue(v.getChangeNote() != null ? v.getChangeNote() : "");
            row.createCell(3).setCellValue(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
            row.createCell(4).setCellValue(v.getPublishedAt() != null ? v.getPublishedAt().toString() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeDefinitionSheet(Sheet sheet, String ruleType, String definitionJson, CellStyle headerStyle) {
        try {
            JsonNode definition = objectMapper.readTree(definitionJson);

            if ("DECISION_TABLE".equals(ruleType) && definition.has("columns") && definition.has("rows")) {
                writeDecisionTableDefinition(sheet, definition, headerStyle);
            } else {
                // Generic JSON key-value dump
                writeGenericDefinition(sheet, definition, headerStyle);
            }
        } catch (Exception e) {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Failed to parse definition: " + e.getMessage());
        }
    }

    private void writeDecisionTableDefinition(Sheet sheet, JsonNode definition, CellStyle headerStyle) {
        JsonNode columns = definition.get("columns");
        JsonNode rows = definition.get("rows");

        // Write column headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            JsonNode col = columns.get(i);
            Cell cell = headerRow.createCell(i);
            String headerText = col.has("name") ? col.get("name").asText() : "Column " + i;
            if (col.has("type")) {
                headerText += " (" + col.get("type").asText() + ")";
            }
            cell.setCellValue(headerText);
            cell.setCellStyle(headerStyle);
        }

        // Write data rows
        for (int r = 0; r < rows.size(); r++) {
            Row dataRow = sheet.createRow(r + 1);
            JsonNode rowNode = rows.get(r);
            JsonNode cells = rowNode.has("cells") ? rowNode.get("cells") : rowNode;

            if (cells.isArray()) {
                for (int c = 0; c < cells.size(); c++) {
                    dataRow.createCell(c).setCellValue(cells.get(c).asText());
                }
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeGenericDefinition(Sheet sheet, JsonNode definition, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        Cell keyHeader = headerRow.createCell(0);
        keyHeader.setCellValue("Key");
        keyHeader.setCellStyle(headerStyle);
        Cell valueHeader = headerRow.createCell(1);
        valueHeader.setCellValue("Value");
        valueHeader.setCellStyle(headerStyle);

        int rowIndex = 1;
        Iterator<Map.Entry<String, JsonNode>> fields = definition.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            String value = entry.getValue().isTextual()
                    ? entry.getValue().asText()
                    : entry.getValue().toString();
            row.createCell(1).setCellValue(value);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private Map<String, Object> buildRuleExportMap(RuleEntity rule, List<RuleVersionEntity> versions) {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("id", rule.getId().toString());
        exportData.put("name", rule.getName());
        exportData.put("alias", rule.getAlias());
        exportData.put("type", rule.getType());
        exportData.put("status", rule.getStatus());
        exportData.put("latestVersion", rule.getLatestVersion());
        exportData.put("folderId", rule.getFolderId() != null ? rule.getFolderId().toString() : null);
        exportData.put("createdAt", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
        exportData.put("updatedAt", rule.getUpdatedAt() != null ? rule.getUpdatedAt().toString() : null);

        List<Map<String, Object>> versionList = new ArrayList<>();
        for (RuleVersionEntity v : versions) {
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("version", v.getVersion());
            vMap.put("status", v.getStatus());
            vMap.put("definition", tryParseJson(v.getDefinition()));
            vMap.put("inputModel", tryParseJson(v.getInputModel()));
            vMap.put("outputModel", tryParseJson(v.getOutputModel()));
            vMap.put("changeNote", v.getChangeNote());
            vMap.put("createdAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
            versionList.add(vMap);
        }
        exportData.put("versions", versionList);

        return exportData;
    }

    private Object tryParseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
