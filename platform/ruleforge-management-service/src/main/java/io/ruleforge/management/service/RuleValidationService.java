package io.ruleforge.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.management.dto.TestResultDto;
import io.ruleforge.management.dto.TestRuleRequest;
import io.ruleforge.management.dto.ValidationResultDto;
import io.ruleforge.management.dto.ValidationResultDto.ErrorDto;
import io.ruleforge.management.dto.ValidationResultDto.WarningDto;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuleValidationService {

    private static final Logger log = LoggerFactory.getLogger(RuleValidationService.class);

    private final RuleRepository ruleRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final ObjectMapper objectMapper;
    private final RuleForgeEngine ruleForgeEngine;

    public RuleValidationService(RuleRepository ruleRepository,
                                 RuleVersionRepository ruleVersionRepository,
                                 ObjectMapper objectMapper,
                                 RuleForgeEngine ruleForgeEngine) {
        this.ruleRepository = ruleRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.objectMapper = objectMapper;
        this.ruleForgeEngine = ruleForgeEngine;
    }

    @Transactional(readOnly = true)
    public ValidationResultDto validateRule(UUID ruleId, int version) {
        RuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));

        RuleVersionEntity versionEntity = ruleVersionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        List<ErrorDto> errors = new ArrayList<>();
        List<WarningDto> warnings = new ArrayList<>();

        // Validate definition is valid JSON
        if (versionEntity.getDefinition() == null || versionEntity.getDefinition().isBlank()) {
            errors.add(new ErrorDto("definition", "Definition is required", "MISSING_DEFINITION"));
        } else {
            try {
                JsonNode definitionNode = objectMapper.readTree(versionEntity.getDefinition());

                // Basic structural checks based on rule type
                validateStructure(rule.getType(), definitionNode, errors, warnings);
            } catch (JsonProcessingException e) {
                errors.add(new ErrorDto("definition", "Definition is not valid JSON: " + e.getMessage(),
                        "INVALID_JSON"));
            }
        }

        // Validate input model is valid JSON if present
        if (versionEntity.getInputModel() != null && !versionEntity.getInputModel().isBlank()) {
            try {
                objectMapper.readTree(versionEntity.getInputModel());
            } catch (JsonProcessingException e) {
                errors.add(new ErrorDto("inputModel", "Input model is not valid JSON: " + e.getMessage(),
                        "INVALID_JSON"));
            }
        } else {
            warnings.add(new WarningDto("inputModel", "No input model defined", "MISSING_INPUT_MODEL"));
        }

        // Validate output model is valid JSON if present
        if (versionEntity.getOutputModel() != null && !versionEntity.getOutputModel().isBlank()) {
            try {
                objectMapper.readTree(versionEntity.getOutputModel());
            } catch (JsonProcessingException e) {
                errors.add(new ErrorDto("outputModel", "Output model is not valid JSON: " + e.getMessage(),
                        "INVALID_JSON"));
            }
        } else {
            warnings.add(new WarningDto("outputModel", "No output model defined", "MISSING_OUTPUT_MODEL"));
        }

        boolean valid = errors.isEmpty();
        log.info("Rule validation completed: ruleId={}, version={}, valid={}, errors={}, warnings={}",
                ruleId, version, valid, errors.size(), warnings.size());

        return new ValidationResultDto(valid, errors, warnings);
    }

    @Transactional(readOnly = true)
    public TestResultDto testRule(UUID ruleId, int version, TestRuleRequest request) {
        RuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));

        RuleVersionEntity versionEntity = ruleVersionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        long start = System.currentTimeMillis();

        try {
            if (versionEntity.getDefinition() == null || versionEntity.getDefinition().isBlank()) {
                return new TestResultDto(
                        Map.of(),
                        System.currentTimeMillis() - start,
                        List.of("No definition to evaluate"),
                        false,
                        "Rule definition is empty"
                );
            }

            // Parse the definition JSON into a Map
            Map<String, Object> definitionMap = objectMapper.readValue(
                    versionEntity.getDefinition(), new TypeReference<Map<String, Object>>() {});

            // Map the entity type string to the engine RuleType enum
            RuleType ruleType = mapRuleType(rule.getType());

            // Build a RuleDefinition for the engine
            RuleDefinition ruleDefinition = new RuleDefinition(
                    ruleId.toString(),
                    rule.getName(),
                    rule.getAlias(),
                    ruleType,
                    versionEntity.getVersion(),
                    RuleMetadata.defaults(),
                    null,
                    null,
                    definitionMap
            );

            // Compile and evaluate using the engine
            var compiledRule = ruleForgeEngine.compile(ruleDefinition);
            RuleResult result = ruleForgeEngine.evaluate(
                    ruleId.toString(), versionEntity.getVersion(), request.input());

            List<String> logs = new ArrayList<>();
            logs.add("Engine evaluation completed successfully");
            if (result.metadata() != null) {
                logs.add("Rules evaluated: " + result.metadata().rulesEvaluated());
                logs.add("Rules fired: " + result.metadata().rulesFired());
                if (result.metadata().executionTime() != null) {
                    logs.add("Engine execution time: " + result.metadata().executionTime().toMillis() + "ms");
                }
            }

            return new TestResultDto(
                    result.output(),
                    System.currentTimeMillis() - start,
                    logs,
                    true,
                    null
            );
        } catch (JsonProcessingException e) {
            return new TestResultDto(
                    Map.of(),
                    System.currentTimeMillis() - start,
                    List.of("Failed to parse rule definition"),
                    false,
                    "Invalid rule definition JSON: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Engine evaluation failed for rule {} v{}", ruleId, version, e);
            return new TestResultDto(
                    Map.of(),
                    System.currentTimeMillis() - start,
                    List.of("Engine evaluation failed: " + e.getMessage()),
                    false,
                    e.getMessage()
            );
        }
    }

    private RuleType mapRuleType(String type) {
        return switch (type) {
            case "DECISION_TABLE" -> RuleType.DECISION_TABLE;
            case "DECISION_TREE" -> RuleType.DECISION_TREE;
            case "EXPRESSION" -> RuleType.SCRIPTING_RULE;
            case "SCRIPT" -> RuleType.SCRIPTING_RULE;
            case "FLOW" -> RuleType.RULE_FLOW;
            case "SCORECARD" -> RuleType.SCORECARD;
            case "ENSEMBLE" -> RuleType.ENSEMBLE;
            case "DMN" -> RuleType.DMN_RULE;
            case "CEP" -> RuleType.CEP_RULE;
            default -> throw new IllegalArgumentException("Unsupported rule type: " + type);
        };
    }

    private void validateStructure(String ruleType, JsonNode definition,
                                   List<ErrorDto> errors, List<WarningDto> warnings) {
        switch (ruleType) {
            case "DECISION_TABLE" -> {
                if (!definition.has("columns")) {
                    errors.add(new ErrorDto("definition.columns", "Decision table must have columns",
                            "MISSING_COLUMNS"));
                }
                if (!definition.has("rows")) {
                    warnings.add(new WarningDto("definition.rows", "Decision table has no rows",
                            "EMPTY_ROWS"));
                }
            }
            case "DECISION_TREE" -> {
                if (!definition.has("root")) {
                    errors.add(new ErrorDto("definition.root", "Decision tree must have a root node",
                            "MISSING_ROOT"));
                }
            }
            case "EXPRESSION" -> {
                if (!definition.has("expression")) {
                    errors.add(new ErrorDto("definition.expression", "Expression rule must have an expression",
                            "MISSING_EXPRESSION"));
                }
            }
            case "SCRIPT" -> {
                if (!definition.has("script")) {
                    errors.add(new ErrorDto("definition.script", "Script rule must have a script body",
                            "MISSING_SCRIPT"));
                }
            }
            case "FLOW" -> {
                if (!definition.has("nodes")) {
                    errors.add(new ErrorDto("definition.nodes", "Flow rule must have nodes",
                            "MISSING_NODES"));
                }
                if (!definition.has("edges")) {
                    warnings.add(new WarningDto("definition.edges", "Flow rule has no edges defined",
                            "MISSING_EDGES"));
                }
            }
            case "SCORECARD" -> {
                if (!definition.has("characteristics")) {
                    errors.add(new ErrorDto("definition.characteristics",
                            "Scorecard must have characteristics", "MISSING_CHARACTERISTICS"));
                }
                if (!definition.has("initialScore")) {
                    warnings.add(new WarningDto("definition.initialScore",
                            "Scorecard has no initial score defined", "MISSING_INITIAL_SCORE"));
                }
            }
            case "ENSEMBLE" -> {
                if (!definition.has("subRules")) {
                    errors.add(new ErrorDto("definition.subRules",
                            "Ensemble must have sub-rules defined", "MISSING_SUB_RULES"));
                }
                if (!definition.has("strategy")) {
                    warnings.add(new WarningDto("definition.strategy",
                            "Ensemble has no aggregation strategy defined", "MISSING_STRATEGY"));
                }
            }
            default -> warnings.add(new WarningDto("type",
                    "Unknown rule type '%s', skipping structural validation".formatted(ruleType),
                    "UNKNOWN_TYPE"));
        }
    }
}
