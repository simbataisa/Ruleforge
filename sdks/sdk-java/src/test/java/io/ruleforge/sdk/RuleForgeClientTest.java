package io.ruleforge.sdk;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleForgeClientTest {

    private static WireMockServer wireMock;
    private static RuleForgeClient client;

    @BeforeAll
    static void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        client = new RuleForgeClient("http://localhost:" + wireMock.port(), "test-api-key");
    }

    @AfterAll
    static void tearDown() {
        wireMock.stop();
    }

    // --- Solver ---

    @Test
    void solve_returnsResult() {
        stubFor(post(urlPathEqualTo("/v1/solve/my-rule"))
                .willReturn(okJson("{\"output\":{\"approved\":true},\"matchedRules\":[],\"executionTimeMs\":5}")));

        Map<String, Object> result = client.solver().solve("my-rule", Map.of("age", 30));

        assertThat(result).containsKey("output");
        assertThat(result.get("executionTimeMs")).isEqualTo(5);
    }

    @Test
    void solveBulk_returnsResults() {
        stubFor(post(urlPathEqualTo("/v1/solve/bulk/my-rule"))
                .willReturn(okJson("[{\"output\":{\"approved\":true}},{\"output\":{\"approved\":false}}]")));

        List<Map<String, Object>> results = client.solver().solveBulk("my-rule",
                List.of(Map.of("age", 30), Map.of("age", 17)));

        assertThat(results).hasSize(2);
    }

    // --- Rules ---

    @Test
    void listRules_returnsRules() {
        stubFor(get(urlPathEqualTo("/v1/spaces/space-1/rules"))
                .willReturn(okJson("{\"content\":[{\"id\":\"r1\",\"name\":\"Test Rule\"}],\"totalElements\":1}")));

        Map<String, Object> result = client.rules().listRules("space-1");

        assertThat(result).containsKey("content");
    }

    @Test
    void getRule_returnsRule() {
        stubFor(get(urlPathEqualTo("/v1/rules/r1"))
                .willReturn(okJson("{\"id\":\"r1\",\"name\":\"Test Rule\"}")));

        Map<String, Object> result = client.rules().getRule("r1");

        assertThat(result).containsEntry("id", "r1");
    }

    // --- Analytics ---

    @Test
    void getMetrics_returnsMetrics() {
        stubFor(get(urlPathEqualTo("/v1/analytics/metrics"))
                .willReturn(okJson("{\"totalExecutions\":1000,\"avgLatencyMs\":12.5}")));

        Map<String, Object> result = client.analytics().getMetrics();

        assertThat(result).containsKey("totalExecutions");
    }

    @Test
    void getRuleStats_returnsStats() {
        stubFor(get(urlPathEqualTo("/v1/analytics/rules/r1/stats"))
                .willReturn(okJson("{\"hitCount\":500,\"avgLatencyMs\":8.2,\"errorRate\":0.01}")));

        Map<String, Object> result = client.analytics().getRuleStats("r1");

        assertThat(result).containsKey("hitCount");
    }

    @Test
    void getDecisionLogs_returnsLogs() {
        stubFor(get(urlPathMatching("/v1/analytics/decision-logs.*"))
                .willReturn(okJson("[{\"id\":\"log1\",\"ruleId\":\"r1\",\"timestamp\":\"2026-01-15T10:00:00Z\"}]")));

        List<Map<String, Object>> logs = client.analytics().getDecisionLogs("r1", 0, 10);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0)).containsEntry("id", "log1");
    }

    // --- Schedules ---

    @Test
    void listSchedules_returnsSchedules() {
        stubFor(get(urlPathEqualTo("/v1/schedules"))
                .willReturn(okJson("[{\"id\":\"s1\",\"cronExpression\":\"0 0 * * *\"}]")));

        List<Map<String, Object>> schedules = client.schedules().listSchedules();

        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0)).containsEntry("id", "s1");
    }

    @Test
    void createSchedule_returnsCreated() {
        stubFor(post(urlPathEqualTo("/v1/schedules"))
                .willReturn(okJson("{\"id\":\"s2\",\"cronExpression\":\"0 */5 * * *\",\"ruleId\":\"r1\"}")));

        Map<String, Object> result = client.schedules().createSchedule(
                Map.of("cronExpression", "0 */5 * * *", "ruleId", "r1"));

        assertThat(result).containsEntry("id", "s2");
    }

    @Test
    void deleteSchedule_succeeds() {
        stubFor(delete(urlPathEqualTo("/v1/schedules/s1"))
                .willReturn(aResponse().withStatus(204)));

        client.schedules().deleteSchedule("s1");

        verify(deleteRequestedFor(urlPathEqualTo("/v1/schedules/s1"))
                .withHeader("X-API-Key", equalTo("test-api-key")));
    }

    // --- AI ---

    @Test
    void explainRule_returnsExplanation() {
        stubFor(post(urlPathEqualTo("/v1/ai/explain"))
                .willReturn(okJson("{\"summary\":\"This rule checks loan eligibility\",\"conditions\":[\"age >= 18\"]}")));

        Map<String, Object> result = client.ai().explainRule("r1");

        assertThat(result).containsKey("summary");
    }

    @Test
    void generateRule_returnsDefinition() {
        stubFor(post(urlPathEqualTo("/v1/ai/generate-rule"))
                .willReturn(okJson("{\"id\":\"generated-1\",\"name\":\"Auto Loan Rule\",\"type\":\"DECISION_TABLE\"}")));

        Map<String, Object> result = client.ai().generateRule(
                "Create a rule that approves loans for applicants over 18 with income > 50000",
                "DECISION_TABLE");

        assertThat(result).containsEntry("type", "DECISION_TABLE");
    }

    @Test
    void generateTestData_returnsData() {
        stubFor(post(urlPathEqualTo("/v1/ai/generate-test-data"))
                .willReturn(okJson("{\"testData\":[{\"age\":25,\"income\":60000},{\"age\":17,\"income\":30000}]}")));

        Map<String, Object> result = client.ai().generateTestData("r1", 2);

        assertThat(result).containsKey("testData");
    }

    // --- Error handling ---

    @Test
    void solve_throwsOnServerError() {
        stubFor(post(urlPathEqualTo("/v1/solve/bad-rule"))
                .willReturn(aResponse().withStatus(500).withBody("{\"error\":\"Internal Server Error\"}")));

        assertThatThrownBy(() -> client.solver().solve("bad-rule", Map.of()))
                .isInstanceOf(RuleForgeClientException.class)
                .hasMessageContaining("500");
    }

    @Test
    void apiKey_sentInHeader() {
        stubFor(get(urlPathEqualTo("/v1/analytics/metrics"))
                .willReturn(okJson("{}")));

        client.analytics().getMetrics();

        verify(getRequestedFor(urlPathEqualTo("/v1/analytics/metrics"))
                .withHeader("X-API-Key", equalTo("test-api-key")));
    }
}
