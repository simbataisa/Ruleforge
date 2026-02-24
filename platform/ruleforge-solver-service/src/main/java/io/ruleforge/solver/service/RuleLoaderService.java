package io.ruleforge.solver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.InputSchema;
import io.ruleforge.core.model.OutputSchema;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.solver.entity.SolverRuleEntity;
import io.ruleforge.solver.entity.SolverRuleVersionEntity;
import io.ruleforge.solver.repository.SolverRuleRepository;
import io.ruleforge.solver.repository.SolverRuleVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Loads rule definitions from the database and compiles them using the engine.
 * Resolves rule identifiers (UUID or alias) and enforces tenant scoping.
 */
@Service
public class RuleLoaderService {

    private static final Logger log = LoggerFactory.getLogger(RuleLoaderService.class);
    private static final String PUBLISHED_STATUS = "PUBLISHED";

    private final SolverRuleRepository ruleRepository;
    private final SolverRuleVersionRepository versionRepository;
    private final RuleForgeEngine engine;
    private final ObjectMapper objectMapper;

    public RuleLoaderService(SolverRuleRepository ruleRepository,
                             SolverRuleVersionRepository versionRepository,
                             RuleForgeEngine engine,
                             ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    /**
     * Loads and compiles a rule by ID or alias, optionally at a specific version.
     * If version is null, the latest published version is used.
     * The rule must belong to the current tenant's space (from TenantContext).
     *
     * @param ruleIdOrAlias a UUID string (rule ID) or an alias string
     * @param version       specific version number, or null for latest published
     * @return the compiled rule ready for evaluation
     * @throws EntityNotFoundException if the rule or version cannot be found
     */
    @Transactional(readOnly = true)
    public CompiledRule loadRule(String ruleIdOrAlias, Integer version) {
        SolverRuleEntity rule = resolveRule(ruleIdOrAlias);

        SolverRuleVersionEntity ruleVersion;
        if (version != null) {
            ruleVersion = versionRepository.findByRuleIdAndVersion(rule.getId(), version)
                    .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                            rule.getId() + " v" + version));
        } else {
            ruleVersion = versionRepository
                    .findFirstByRuleIdAndStatusOrderByVersionDesc(rule.getId(), PUBLISHED_STATUS)
                    .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                            rule.getId() + " (latest published)"));
        }

        return compileFromVersion(rule, ruleVersion);
    }

    /**
     * Returns the resolved rule entity for the given identifier.
     * Used by other services that need rule metadata without compilation.
     */
    @Transactional(readOnly = true)
    public SolverRuleEntity resolveRule(String ruleIdOrAlias) {
        UUID spaceId = getSpaceId();

        Optional<SolverRuleEntity> rule;
        if (isUUID(ruleIdOrAlias)) {
            UUID ruleId = UUID.fromString(ruleIdOrAlias);
            rule = spaceId != null
                    ? ruleRepository.findByIdAndSpaceId(ruleId, spaceId)
                    : ruleRepository.findById(ruleId);
        } else {
            rule = spaceId != null
                    ? ruleRepository.findByAliasAndSpaceId(ruleIdOrAlias, spaceId)
                    : ruleRepository.findByAlias(ruleIdOrAlias);
        }

        return rule.orElseThrow(() -> new EntityNotFoundException("Rule", ruleIdOrAlias));
    }

    private CompiledRule compileFromVersion(SolverRuleEntity rule, SolverRuleVersionEntity ruleVersion) {
        try {
            String definitionJson = ruleVersion.getDefinition();
            if (definitionJson == null || definitionJson.isBlank()) {
                throw new IllegalStateException(
                        "Rule version %s v%d has no definition".formatted(rule.getId(), ruleVersion.getVersion()));
            }

            Map<String, Object> definitionMap = objectMapper.readValue(
                    definitionJson, new TypeReference<>() {});

            InputSchema inputSchema = null;
            if (ruleVersion.getInputModel() != null && !ruleVersion.getInputModel().isBlank()) {
                inputSchema = parseInputSchema(ruleVersion.getInputModel());
            }

            OutputSchema outputSchema = null;
            if (ruleVersion.getOutputModel() != null && !ruleVersion.getOutputModel().isBlank()) {
                outputSchema = parseOutputSchema(ruleVersion.getOutputModel());
            }

            RuleType ruleType = mapRuleType(rule.getType());

            RuleDefinition definition = new RuleDefinition(
                    rule.getId().toString(),
                    rule.getName(),
                    rule.getAlias(),
                    ruleType,
                    ruleVersion.getVersion(),
                    RuleMetadata.defaults(),
                    inputSchema,
                    outputSchema,
                    definitionMap
            );

            log.debug("Compiling rule {} v{} (type={})", rule.getId(), ruleVersion.getVersion(), ruleType);
            return engine.compile(definition);

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to compile rule %s v%d: %s".formatted(
                            rule.getId(), ruleVersion.getVersion(), e.getMessage()), e);
        }
    }

    private UUID getSpaceId() {
        TenantContext.TenantInfo tenant = TenantContext.get();
        return tenant != null ? tenant.spaceId() : null;
    }

    /**
     * Parses input_model JSON that may be either:
     * - Structured: {"fields": {...}, "required": [...]}
     * - Simple flat map: {"fieldName": "TYPE", ...}
     */
    private InputSchema parseInputSchema(String json) throws Exception {
        try {
            return objectMapper.readValue(json, InputSchema.class);
        } catch (Exception e) {
            // Fall back to simple flat map format: {"fieldName": "TYPE", ...}
            Map<String, String> flatMap = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, InputSchema.FieldDefinition> fields = new LinkedHashMap<>();
            for (var entry : flatMap.entrySet()) {
                FactType type = parseFactType(entry.getValue());
                fields.put(entry.getKey(), new InputSchema.FieldDefinition(entry.getKey(), type, null, null));
            }
            return new InputSchema(fields, Set.copyOf(flatMap.keySet()));
        }
    }

    /**
     * Parses output_model JSON that may be either:
     * - Structured: {"fields": {...}}
     * - Simple flat map: {"fieldName": "TYPE", ...}
     */
    private OutputSchema parseOutputSchema(String json) throws Exception {
        try {
            return objectMapper.readValue(json, OutputSchema.class);
        } catch (Exception e) {
            // Fall back to simple flat map format
            Map<String, String> flatMap = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, OutputSchema.FieldDefinition> fields = new LinkedHashMap<>();
            for (var entry : flatMap.entrySet()) {
                FactType type = parseFactType(entry.getValue());
                fields.put(entry.getKey(), new OutputSchema.FieldDefinition(entry.getKey(), type, null));
            }
            return new OutputSchema(fields);
        }
    }

    private static final Map<String, RuleType> RULE_TYPE_MAP = Map.ofEntries(
            Map.entry("TABLE", RuleType.DECISION_TABLE),
            Map.entry("DECISION_TABLE", RuleType.DECISION_TABLE),
            Map.entry("TREE", RuleType.DECISION_TREE),
            Map.entry("DECISION_TREE", RuleType.DECISION_TREE),
            Map.entry("SCRIPT", RuleType.SCRIPTING_RULE),
            Map.entry("SCRIPTING_RULE", RuleType.SCRIPTING_RULE),
            Map.entry("FLOW", RuleType.RULE_FLOW),
            Map.entry("RULE_FLOW", RuleType.RULE_FLOW),
            Map.entry("SCORECARD", RuleType.SCORECARD),
            Map.entry("CEP", RuleType.CEP_RULE),
            Map.entry("CEP_RULE", RuleType.CEP_RULE),
            Map.entry("DMN", RuleType.DMN_RULE),
            Map.entry("DMN_RULE", RuleType.DMN_RULE),
            Map.entry("ENSEMBLE", RuleType.ENSEMBLE),
            Map.entry("HIERARCHY", RuleType.HIERARCHY)
    );

    private RuleType mapRuleType(String dbType) {
        RuleType mapped = RULE_TYPE_MAP.get(dbType.toUpperCase());
        if (mapped == null) {
            throw new IllegalArgumentException("Unknown rule type: " + dbType);
        }
        return mapped;
    }

    private FactType parseFactType(String typeName) {
        try {
            return FactType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FactType.ANY;
        }
    }

    private boolean isUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
