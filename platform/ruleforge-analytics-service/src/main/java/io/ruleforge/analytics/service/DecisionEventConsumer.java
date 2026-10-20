package io.ruleforge.analytics.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.ruleforge.analytics.model.DecisionLogDocument;
import io.ruleforge.analytics.repository.DecisionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer that listens on the "rule.execution" topic and indexes each
 * execution event as a {@link DecisionLogDocument} in Elasticsearch.
 *
 * Expected Kafka event fields:
 *   ruleId, ruleName, ruleType, version, orgId, spaceId, inputFacts, outputResult,
 *   executionTimeMs, rulesEvaluated, rulesFired, strategy, correlationId, status,
 *   errorMessage (optional), timestamp
 */
@Service
public class DecisionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DecisionEventConsumer.class);

    private final DecisionLogRepository decisionLogRepository;
    private final ObjectMapper objectMapper;

    public DecisionEventConsumer(DecisionLogRepository decisionLogRepository,
                                 ObjectMapper objectMapper) {
        this.decisionLogRepository = decisionLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "rule.execution", groupId = "analytics-consumer-group")
    public void consume(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(
                    message, new TypeReference<Map<String, Object>>() {});

            DecisionLogDocument doc = mapToDocument(event);
            decisionLogRepository.save(doc);

            log.debug("Indexed decision log for ruleId={} correlationId={}",
                    doc.getRuleId(), doc.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to process rule.execution event — skipping. message={}", message, e);
        }
    }

    @SuppressWarnings("unchecked")
    private DecisionLogDocument mapToDocument(Map<String, Object> event) {
        DecisionLogDocument doc = new DecisionLogDocument();

        doc.setId(UUID.randomUUID().toString());
        doc.setRuleId(getString(event, "ruleId"));
        doc.setRuleName(getString(event, "ruleName"));
        doc.setRuleType(getString(event, "ruleType"));
        doc.setVersion(getInt(event, "version"));
        doc.setOrgId(getString(event, "orgId"));
        doc.setSpaceId(getString(event, "spaceId"));
        doc.setExecutionTimeMs(getLong(event, "executionTimeMs"));
        doc.setRulesEvaluated(getInt(event, "rulesEvaluated"));
        doc.setRulesFired(getInt(event, "rulesFired"));
        doc.setStrategy(getString(event, "strategy"));
        doc.setCorrelationId(getString(event, "correlationId"));
        doc.setStatus(getString(event, "status"));
        doc.setErrorMessage(getString(event, "errorMessage"));
        doc.setAbTestId(getString(event, "abTestId"));
        doc.setAbTestVersion(getString(event, "abTestVersion"));

        Object inputFacts = event.get("inputFacts");
        if (inputFacts instanceof Map) {
            doc.setInputFacts((Map<String, Object>) inputFacts);
        }

        Object outputResult = event.get("outputResult");
        if (outputResult instanceof Map) {
            doc.setOutputResult((Map<String, Object>) outputResult);
        }

        String timestampStr = getString(event, "timestamp");
        if (timestampStr != null) {
            try {
                doc.setTimestamp(Instant.parse(timestampStr));
            } catch (Exception e) {
                doc.setTimestamp(Instant.now());
            }
        } else {
            doc.setTimestamp(Instant.now());
        }

        return doc;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
