"""Tests for the RuleForge Python SDK client."""

from __future__ import annotations

import httpx
import pytest
import respx

from ruleforge_sdk import RuleForgeClient

BASE_URL = "https://api.ruleforge.io"
API_KEY = "test-api-key"


@pytest.fixture
def client() -> RuleForgeClient:
    return RuleForgeClient(base_url=BASE_URL, api_key=API_KEY)


# --- Solver ---


@respx.mock
def test_solve(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/solve/my-rule").mock(
        return_value=httpx.Response(
            200,
            json={"output": {"approved": True}, "matchedRules": [], "executionTimeMs": 5},
        )
    )
    result = client.solve("my-rule", {"age": 30})
    assert result["output"]["approved"] is True
    assert result["executionTimeMs"] == 5


@respx.mock
def test_solve_bulk(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/solve/bulk/my-rule").mock(
        return_value=httpx.Response(
            200,
            json=[{"output": {"approved": True}}, {"output": {"approved": False}}],
        )
    )
    results = client.solve_bulk("my-rule", [{"age": 30}, {"age": 17}])
    assert len(results) == 2


# --- Rules ---


@respx.mock
def test_list_rules(client: RuleForgeClient) -> None:
    respx.get(f"{BASE_URL}/v1/spaces/space-1/rules").mock(
        return_value=httpx.Response(
            200,
            json={"data": {"content": [{"id": "r1", "name": "Test Rule"}], "totalElements": 1}},
        )
    )
    rules = client.list_rules("space-1")
    assert len(rules) == 1
    assert rules[0]["id"] == "r1"


@respx.mock
def test_get_rule(client: RuleForgeClient) -> None:
    respx.get(f"{BASE_URL}/v1/rules/r1").mock(
        return_value=httpx.Response(200, json={"data": {"id": "r1", "name": "Test Rule"}})
    )
    rule = client.get_rule("r1")
    assert rule["id"] == "r1"


# --- Analytics ---


@respx.mock
def test_get_metrics(client: RuleForgeClient) -> None:
    respx.get(f"{BASE_URL}/v1/analytics/metrics").mock(
        return_value=httpx.Response(200, json={"totalExecutions": 1000, "avgLatencyMs": 12.5})
    )
    metrics = client.get_metrics()
    assert metrics["totalExecutions"] == 1000


@respx.mock
def test_get_rule_stats(client: RuleForgeClient) -> None:
    respx.get(f"{BASE_URL}/v1/analytics/rules/r1/stats").mock(
        return_value=httpx.Response(200, json={"hitCount": 500, "avgLatencyMs": 8.2, "errorRate": 0.01})
    )
    stats = client.get_rule_stats("r1")
    assert stats["hitCount"] == 500
    assert stats["errorRate"] == 0.01


@respx.mock
def test_get_decision_logs(client: RuleForgeClient) -> None:
    respx.get(url__regex=r".*/v1/analytics/decision-logs.*").mock(
        return_value=httpx.Response(
            200,
            json=[{"id": "log1", "ruleId": "r1", "timestamp": "2026-01-15T10:00:00Z"}],
        )
    )
    logs = client.get_decision_logs(rule_id="r1", page=0, size=10)
    assert len(logs) == 1
    assert logs[0]["id"] == "log1"


@respx.mock
def test_get_decision_logs_no_filter(client: RuleForgeClient) -> None:
    respx.get(url__regex=r".*/v1/analytics/decision-logs.*").mock(
        return_value=httpx.Response(200, json=[])
    )
    logs = client.get_decision_logs()
    assert logs == []


# --- Schedules ---


@respx.mock
def test_list_schedules(client: RuleForgeClient) -> None:
    respx.get(f"{BASE_URL}/v1/schedules").mock(
        return_value=httpx.Response(
            200, json=[{"id": "s1", "cronExpression": "0 0 * * *"}]
        )
    )
    schedules = client.list_schedules()
    assert len(schedules) == 1
    assert schedules[0]["id"] == "s1"


@respx.mock
def test_create_schedule(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/schedules").mock(
        return_value=httpx.Response(
            200, json={"id": "s2", "cronExpression": "0 */5 * * *", "ruleId": "r1"}
        )
    )
    result = client.create_schedule(
        {"cronExpression": "0 */5 * * *", "ruleId": "r1"}
    )
    assert result["id"] == "s2"


@respx.mock
def test_delete_schedule(client: RuleForgeClient) -> None:
    respx.delete(f"{BASE_URL}/v1/schedules/s1").mock(
        return_value=httpx.Response(204)
    )
    client.delete_schedule("s1")  # should not raise


# --- AI ---


@respx.mock
def test_explain_rule(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/ai/explain").mock(
        return_value=httpx.Response(
            200,
            json={"summary": "Checks loan eligibility", "conditions": ["age >= 18"]},
        )
    )
    result = client.explain_rule("r1")
    assert "summary" in result


@respx.mock
def test_generate_rule(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/ai/generate-rule").mock(
        return_value=httpx.Response(
            200,
            json={"id": "gen-1", "name": "Auto Loan Rule", "type": "DECISION_TABLE"},
        )
    )
    result = client.generate_rule("Approve loans for age > 18", "DECISION_TABLE")
    assert result["type"] == "DECISION_TABLE"


@respx.mock
def test_generate_test_data(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/ai/generate-test-data").mock(
        return_value=httpx.Response(
            200,
            json={"testData": [{"age": 25, "income": 60000}, {"age": 17, "income": 30000}]},
        )
    )
    result = client.generate_test_data("r1", count=2)
    assert len(result["testData"]) == 2


# --- Error handling ---


@respx.mock
def test_solve_server_error(client: RuleForgeClient) -> None:
    respx.post(f"{BASE_URL}/v1/solve/bad-rule").mock(
        return_value=httpx.Response(500, json={"error": "Internal Server Error"})
    )
    with pytest.raises(httpx.HTTPStatusError):
        client.solve("bad-rule", {})


@respx.mock
def test_auth_header_sent(client: RuleForgeClient) -> None:
    route = respx.get(f"{BASE_URL}/v1/analytics/metrics").mock(
        return_value=httpx.Response(200, json={})
    )
    client.get_metrics()
    assert route.calls[0].request.headers["X-API-Key"] == API_KEY
