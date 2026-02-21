import json
import logging

from app.models.requests import GenerateTestDataRequest
from app.models.responses import GenerateTestDataResponse, TestCase
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

TEST_DATA_TOOL = {
    "name": "provide_test_cases",
    "description": "Return structured test cases for a rule",
    "input_schema": {
        "type": "object",
        "properties": {
            "test_cases": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "inputs": {
                            "type": "object",
                            "description": "Input facts for the test case",
                        },
                        "expected_description": {
                            "type": "string",
                            "description": "What this test case verifies",
                        },
                    },
                    "required": ["inputs", "expected_description"],
                },
            },
        },
        "required": ["test_cases"],
    },
}

SYSTEM_PROMPT = """You are a test data generation expert for the RuleForge BRMS platform.
Given a rule definition, generate comprehensive test cases covering:
1. Normal cases — typical inputs that match each condition
2. Boundary values — values at the edges of numeric/date ranges
3. Null cases — missing or null inputs for each condition
4. Edge cases — empty strings, zero values, extreme numbers
5. Combined conditions — inputs that test multiple conditions simultaneously

Each test case should have realistic input values and a description of what it verifies."""


async def generate_test_data(
    request: GenerateTestDataRequest,
) -> GenerateTestDataResponse:
    user_message = (
        f"Generate {request.count} test cases for rule ID: {request.rule_id}\n\n"
        f"Rule definition:\n```json\n{json.dumps(request.rule_definition, indent=2)}\n```"
    )

    result = await llm_client.call_with_tools(
        system_prompt=SYSTEM_PROMPT,
        user_message=user_message,
        tools=[TEST_DATA_TOOL],
    )

    test_cases = [
        TestCase(
            inputs=tc.get("inputs", {}),
            expected_description=tc.get("expected_description", ""),
        )
        for tc in result.get("test_cases", [])
    ]

    return GenerateTestDataResponse(test_cases=test_cases)
