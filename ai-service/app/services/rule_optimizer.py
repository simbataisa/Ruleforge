import json
import logging
from pathlib import Path

from app.models.requests import OptimizeRuleRequest
from app.models.responses import OptimizeRuleResponse, OptimizeSuggestion
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "optimize_rule.txt"

OPTIMIZATION_TOOL = {
    "name": "provide_optimization_suggestions",
    "description": "Return structured optimization suggestions for a rule",
    "input_schema": {
        "type": "object",
        "properties": {
            "analysis": {
                "type": "string",
                "description": "Overall analysis of the rule",
            },
            "suggestions": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "type": {
                            "type": "string",
                            "enum": [
                                "consolidation",
                                "removal",
                                "reorder",
                                "data_type_fix",
                            ],
                        },
                        "description": {"type": "string"},
                        "impact": {"type": "string"},
                    },
                    "required": ["type", "description"],
                },
            },
        },
        "required": ["analysis", "suggestions"],
    },
}


async def optimize_rule(request: OptimizeRuleRequest) -> OptimizeRuleResponse:
    system_prompt = PROMPT_PATH.read_text()

    logs_summary = ""
    if request.decision_logs:
        logs_summary = (
            f"\n\nDecision logs ({len(request.decision_logs)} entries):\n"
            f"```json\n{json.dumps(request.decision_logs[:20], indent=2)}\n```"
        )

    user_message = (
        f"Analyze and optimize rule ID: {request.rule_id}\n"
        f"{logs_summary}"
    )

    result = await llm_client.call_with_tools(
        system_prompt=system_prompt,
        user_message=user_message,
        tools=[OPTIMIZATION_TOOL],
    )

    suggestions = [
        OptimizeSuggestion(
            type=s.get("type", "unknown"),
            description=s.get("description", ""),
            impact=s.get("impact", ""),
        )
        for s in result.get("suggestions", [])
    ]

    return OptimizeRuleResponse(
        suggestions=suggestions,
        analysis=result.get("analysis", ""),
    )
