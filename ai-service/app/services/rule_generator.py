import json
import logging
from pathlib import Path

from app.models.requests import GenerateRuleRequest
from app.models.responses import GenerateRuleResponse
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "generate_rule.txt"

RULE_DEFINITION_TOOL = {
    "name": "create_rule_definition",
    "description": "Create a RuleForge rule definition from a natural language description",
    "input_schema": {
        "type": "object",
        "properties": {
            "name": {
                "type": "string",
                "description": "Rule name in snake_case",
            },
            "description": {
                "type": "string",
                "description": "Human-readable description of the rule",
            },
            "ruleType": {
                "type": "string",
                "enum": ["DECISION_TABLE", "DECISION_TREE"],
            },
            "definition": {
                "type": "object",
                "description": "The full rule definition object",
            },
        },
        "required": ["name", "description", "ruleType", "definition"],
    },
}


async def generate_rule(request: GenerateRuleRequest) -> GenerateRuleResponse:
    system_prompt = PROMPT_PATH.read_text()

    user_message = (
        f"Generate a {request.rule_type} rule for the following requirement:\n\n"
        f"{request.description}"
    )

    tool_result = await llm_client.call_with_tools(
        system_prompt=system_prompt,
        user_message=user_message,
        tools=[RULE_DEFINITION_TOOL],
    )

    explanation = (
        f"Generated a {tool_result.get('ruleType', request.rule_type)} rule "
        f"named '{tool_result.get('name', 'unnamed')}': "
        f"{tool_result.get('description', '')}"
    )

    return GenerateRuleResponse(
        rule_definition=tool_result,
        explanation=explanation,
    )
