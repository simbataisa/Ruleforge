import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.llm_client import llm_client as app_llm_client

client = TestClient(app)


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "ok"
    assert "llm_configured" in data


def test_cors_uses_explicit_localhost_origin_with_credentials():
    middleware = next(
        item for item in app.user_middleware if item.cls.__name__ == "CORSMiddleware"
    )

    assert "*" not in middleware.kwargs["allow_origins"]
    assert "http://localhost:3000" in middleware.kwargs["allow_origins"]
    assert middleware.kwargs["allow_credentials"] is True


def test_generate_rule_no_api_key():
    with patch("app.services.llm_client.settings.anthropic_api_key", ""):
        resp = client.post(
            "/v1/ai/generate-rule",
            json={
                "description": "Approve loans over 50k",
                "rule_type": "decision_table",
                "space_id": "space-1",
            },
        )
        assert resp.status_code == 503
        assert "ANTHROPIC_API_KEY" in resp.json()["detail"]


def _mock_tool_response(tool_input: dict):
    """Create a mock Anthropic response with tool_use content."""
    block = MagicMock()
    block.type = "tool_use"
    block.input = tool_input

    response = MagicMock()
    response.content = [block]
    response.usage = MagicMock(input_tokens=100, output_tokens=200)
    return response


def _mock_text_response(text: str):
    """Create a mock Anthropic response with text content."""
    block = MagicMock()
    block.type = "text"
    block.text = text

    response = MagicMock()
    response.content = [block]
    response.usage = MagicMock(input_tokens=50, output_tokens=150)
    return response


@pytest.mark.asyncio
async def test_generate_rule():
    tool_output = {
        "name": "loan_approval",
        "description": "Approve loans based on amount",
        "ruleType": "DECISION_TABLE",
        "definition": {
            "hitPolicy": "FIRST_MATCH",
            "conditionColumns": [
                {"name": "amount", "factPath": "$.loan.amount", "dataType": "NUMBER"}
            ],
            "actionColumns": [{"name": "decision", "dataType": "STRING"}],
            "rows": [
                {
                    "conditions": [{"operator": "GT", "values": ["50000"]}],
                    "actions": [{"value": "MANUAL_REVIEW"}],
                }
            ],
        },
    }

    mock_response = _mock_tool_response(tool_output)
    mock_client = MagicMock()
    mock_client.messages = MagicMock()
    mock_client.messages.create = AsyncMock(return_value=mock_response)

    with (
        patch("app.services.llm_client.settings.anthropic_api_key", "test-key"),
        patch.object(app_llm_client, "_client", mock_client),
    ):
        resp = client.post(
            "/v1/ai/generate-rule",
            json={
                "description": "Approve loans over 50k for manual review",
                "rule_type": "decision_table",
                "space_id": "space-1",
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "rule_definition" in data
        assert data["rule_definition"]["ruleType"] == "DECISION_TABLE"


@pytest.mark.asyncio
async def test_explain_rule():
    explanation = "# Loan Approval Rule\nThis rule checks loan amounts and routes large loans to manual review."

    mock_response = _mock_text_response(explanation)
    mock_client = MagicMock()
    mock_client.messages = MagicMock()
    mock_client.messages.create = AsyncMock(return_value=mock_response)

    with (
        patch("app.services.llm_client.settings.anthropic_api_key", "test-key"),
        patch.object(app_llm_client, "_client", mock_client),
    ):
        resp = client.post(
            "/v1/ai/explain-rule",
            json={
                "rule_id": "rule-123",
                "rule_definition": {"name": "test_rule", "ruleType": "DECISION_TABLE"},
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "explanation" in data
        assert "Loan Approval" in data["explanation"]


@pytest.mark.asyncio
async def test_optimize_rule():
    tool_output = {
        "analysis": "Rule has 2 redundant rows.",
        "suggestions": [
            {
                "type": "removal",
                "description": "Row 3 is shadowed by Row 1",
                "impact": "Improves clarity",
            }
        ],
    }

    mock_response = _mock_tool_response(tool_output)
    mock_client = MagicMock()
    mock_client.messages = MagicMock()
    mock_client.messages.create = AsyncMock(return_value=mock_response)

    with (
        patch("app.services.llm_client.settings.anthropic_api_key", "test-key"),
        patch.object(app_llm_client, "_client", mock_client),
    ):
        resp = client.post(
            "/v1/ai/optimize-rule",
            json={"rule_id": "rule-123", "decision_logs": []},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["suggestions"]) == 1
        assert data["suggestions"][0]["type"] == "removal"


@pytest.mark.asyncio
async def test_generate_test_data():
    tool_output = {
        "test_cases": [
            {
                "inputs": {"loan_amount": 60000, "credit_score": 750},
                "expected_description": "High loan amount with good credit",
            },
            {
                "inputs": {"loan_amount": 0, "credit_score": None},
                "expected_description": "Edge case: zero amount with null credit score",
            },
        ]
    }

    mock_response = _mock_tool_response(tool_output)
    mock_client = MagicMock()
    mock_client.messages = MagicMock()
    mock_client.messages.create = AsyncMock(return_value=mock_response)

    with (
        patch("app.services.llm_client.settings.anthropic_api_key", "test-key"),
        patch.object(app_llm_client, "_client", mock_client),
    ):
        resp = client.post(
            "/v1/ai/generate-test-data",
            json={
                "rule_id": "rule-123",
                "rule_definition": {"name": "test_rule"},
                "count": 5,
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["test_cases"]) == 2


@pytest.mark.asyncio
async def test_detect_conflicts_insufficient_rules():
    """Test conflict detection with rules that can't be fetched."""
    with (
        patch("app.services.llm_client.settings.anthropic_api_key", "test-key"),
        patch("app.services.conflict_detector._fetch_rule", new_callable=AsyncMock, return_value=None),
    ):
        resp = client.post(
            "/v1/ai/detect-conflicts",
            json={"space_id": "space-1", "rule_ids": ["rule-1", "rule-2"]},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["conflicts"]) == 0
        assert "at least 2" in data["summary"]
