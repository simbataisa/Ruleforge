package io.ruleforge.analytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

/**
 * Elasticsearch document representing a single rule execution (decision) log entry.
 * Indexed into the "ruleforge-decisions" index (dynamic per-day indices are handled
 * at the service layer using the index-prefix config property).
 */
@Document(indexName = "ruleforge-decisions")
public class DecisionLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String ruleId;

    @Field(type = FieldType.Keyword)
    private String ruleName;

    @Field(type = FieldType.Keyword)
    private String ruleType;

    @Field(type = FieldType.Integer)
    private int version;

    @Field(type = FieldType.Keyword)
    private String orgId;

    @Field(type = FieldType.Keyword)
    private String spaceId;

    @Field(type = FieldType.Object, enabled = false)
    private Map<String, Object> inputFacts;

    @Field(type = FieldType.Object, enabled = false)
    private Map<String, Object> outputResult;

    @Field(type = FieldType.Long)
    private long executionTimeMs;

    @Field(type = FieldType.Integer)
    private int rulesEvaluated;

    @Field(type = FieldType.Integer)
    private int rulesFired;

    @Field(type = FieldType.Keyword)
    private String strategy;

    @Field(type = FieldType.Keyword)
    private String correlationId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String errorMessage;

    @Field(type = FieldType.Date)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String abTestId;

    @Field(type = FieldType.Keyword)
    private String abTestVersion;

    // --- Constructors ---

    public DecisionLogDocument() {}

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public Map<String, Object> getInputFacts() { return inputFacts; }
    public void setInputFacts(Map<String, Object> inputFacts) { this.inputFacts = inputFacts; }

    public Map<String, Object> getOutputResult() { return outputResult; }
    public void setOutputResult(Map<String, Object> outputResult) { this.outputResult = outputResult; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public int getRulesEvaluated() { return rulesEvaluated; }
    public void setRulesEvaluated(int rulesEvaluated) { this.rulesEvaluated = rulesEvaluated; }

    public int getRulesFired() { return rulesFired; }
    public void setRulesFired(int rulesFired) { this.rulesFired = rulesFired; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getAbTestId() { return abTestId; }
    public void setAbTestId(String abTestId) { this.abTestId = abTestId; }

    public String getAbTestVersion() { return abTestVersion; }
    public void setAbTestVersion(String abTestVersion) { this.abTestVersion = abTestVersion; }
}
