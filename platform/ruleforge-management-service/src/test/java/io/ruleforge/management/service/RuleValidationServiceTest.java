package io.ruleforge.management.service;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.management.dto.TestResultDto;
import io.ruleforge.management.dto.TestRuleRequest;
import io.ruleforge.management.dto.ValidationResultDto;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleValidationServiceTest {

    @Mock private RuleRepository ruleRepository;
    @Mock private RuleVersionRepository ruleVersionRepository;
    @Mock private RuleForgeEngine ruleForgeEngine;

    private RuleValidationService validationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validationService = new RuleValidationService(
                ruleRepository, ruleVersionRepository, objectMapper, ruleForgeEngine);
    }

    @Test
    void validateRule_validDecisionTable_returnsValid() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("DECISION_TABLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition("{\"columns\":[],\"rows\":[]}");
        version.setInputModel("{}");
        version.setOutputModel("{}");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        ValidationResultDto result = validationService.validateRule(ruleId, 1);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validateRule_missingColumns_returnsError() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("DECISION_TABLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition("{\"rows\":[]}");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        ValidationResultDto result = validationService.validateRule(ruleId, 1);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> "MISSING_COLUMNS".equals(e.code())));
    }

    @Test
    void validateRule_missingDefinition_returnsError() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("DECISION_TABLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition(null);
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        ValidationResultDto result = validationService.validateRule(ruleId, 1);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> "MISSING_DEFINITION".equals(e.code())));
    }

    @Test
    void validateRule_scorecardMissingCharacteristics_returnsError() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("SCORECARD");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition("{}");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        ValidationResultDto result = validationService.validateRule(ruleId, 1);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> "MISSING_CATEGORIES".equals(e.code())));
    }

    @Test
    void validateRule_ensembleMissingSubRules_returnsError() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("ENSEMBLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition("{}");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        ValidationResultDto result = validationService.validateRule(ruleId, 1);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> "MISSING_MEMBERS".equals(e.code())));
    }

    @Test
    void testRule_emptyDefinition_returnsFailure() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setType("DECISION_TABLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setDefinition("");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        TestRuleRequest request = new TestRuleRequest(Map.of("age", 25), null);
        TestResultDto result = validationService.testRule(ruleId, 1, request);

        assertFalse(result.success());
        assertEquals("Rule definition is empty", result.error());
    }

    @Test
    void testRule_engineEvaluatesSuccessfully() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = new RuleEntity();
        rule.setId(ruleId);
        rule.setName("Test Rule");
        rule.setAlias("test-rule");
        rule.setType("DECISION_TABLE");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleVersionEntity version = new RuleVersionEntity();
        version.setVersion(1);
        version.setDefinition("{\"columns\":[],\"rows\":[]}");
        when(ruleVersionRepository.findByRuleIdAndVersion(ruleId, 1)).thenReturn(Optional.of(version));

        CompiledRule compiledRule = mock(CompiledRule.class);
        when(ruleForgeEngine.compile(any())).thenReturn(compiledRule);

        RuleResult engineResult = new RuleResult(
                Map.of("result", "approved"),
                new RuleResultMetadata(Duration.ofMillis(5), 3, 1, 0, "FIRST_MATCH"),
                List.of(),
                null
        );
        when(ruleForgeEngine.evaluate(eq(compiledRule), any())).thenReturn(engineResult);

        TestRuleRequest request = new TestRuleRequest(Map.of("age", 25), null);
        TestResultDto result = validationService.testRule(ruleId, 1, request);

        assertTrue(result.success());
        assertEquals("approved", result.output().get("result"));
    }
}
