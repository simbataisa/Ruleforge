import json
import logging
from pathlib import Path

from app.models.requests import ExplainRuleRequest
from app.models.responses import ExplainRuleResponse
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "explain_rule.txt"


async def explain_rule(request: ExplainRuleRequest) -> ExplainRuleResponse:
    system_prompt = PROMPT_PATH.read_text()

    user_message = (
        f"Explain the following rule (ID: {request.rule_id}):\n\n"
        f"```json\n{json.dumps(request.rule_definition, indent=2)}\n```"
    )

    response_text = await llm_client.call(
        system_prompt=system_prompt,
        user_message=user_message,
    )

    lines = response_text.strip().split("\n")
    summary = lines[0].lstrip("# ").strip() if lines else ""

    return ExplainRuleResponse(
        explanation=response_text,
        summary=summary,
    )
