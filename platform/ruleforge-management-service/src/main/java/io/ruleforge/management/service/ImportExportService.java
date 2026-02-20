package io.ruleforge.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.management.dto.ExportFormat;
import io.ruleforge.management.dto.ImportResult;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * Excel export is stubbed for Phase 2; full implementation requires Apache POI.
     */
    public byte[] exportRuleToExcel(UUID ruleId, UUID spaceId) {
        // TODO: Phase 2 - implement Excel export using Apache POI
        throw new UnsupportedOperationException("Excel export is not yet implemented. Use JSON format.");
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
