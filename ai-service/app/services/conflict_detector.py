import json
import logging
from pathlib import Path

import httpx

from app.config import settings
from app.models.requests import DetectConflictsRequest
from app.models.responses import Conflict, DetectConflictsResponse
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "detect_conflicts.txt"

CONFLICT_TOOL = {
    "name": "report_conflicts",
    "description": "Report detected conflicts between rules",
    "input_schema": {
        "type": "object",
        "properties": {
            "summary": {
                "type": "string",
                "description": "Overall conflict analysis summary",
            },
            "conflicts": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "rule_id_a": {"type": "string"},
                        "rule_id_b": {"type": "string"},
                        "conflict_type": {
                            "type": "string",
                            "enum": [
                                "identical",
                                "overlapping",
                                "contradictory",
                                "subsumption",
                            ],
                        },
                        "description": {"type": "string"},
                    },
                    "required": [
                        "rule_id_a",
                        "rule_id_b",
                        "conflict_type",
                        "description",
                    ],
                },
            },
        },
        "required": ["summary", "conflicts"],
    },
}


async def _fetch_rule(rule_id: str) -> dict | None:
    """Fetch a rule definition from the management service."""
    url = f"{settings.management_service_url}/v1/rules/{rule_id}"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(url)
            if resp.status_code == 200:
                return resp.json()
            logger.warning("Failed to fetch rule %s: HTTP %d", rule_id, resp.status_code)
    except httpx.HTTPError as e:
        logger.warning("Failed to fetch rule %s: %s", rule_id, str(e))
    return None


async def detect_conflicts(
    request: DetectConflictsRequest,
) -> DetectConflictsResponse:
    system_prompt = PROMPT_PATH.read_text()

    rules: list[dict] = []
    for rule_id in request.rule_ids:
        rule = await _fetch_rule(rule_id)
        if rule:
            rules.append(rule)

    if len(rules) < 2:
        return DetectConflictsResponse(
            conflicts=[],
            summary="Need at least 2 valid rules to detect conflicts.",
        )

    rules_text = "\n\n".join(
        f"Rule {i + 1} (ID: {r.get('id', 'unknown')}):\n```json\n{json.dumps(r, indent=2)}\n```"
        for i, r in enumerate(rules)
    )

    user_message = (
        f"Detect conflicts among the following {len(rules)} rules "
        f"in space {request.space_id}:\n\n{rules_text}"
    )

    result = await llm_client.call_with_tools(
        system_prompt=system_prompt,
        user_message=user_message,
        tools=[CONFLICT_TOOL],
    )

    conflicts = [
        Conflict(
            rule_id_a=c.get("rule_id_a", ""),
            rule_id_b=c.get("rule_id_b", ""),
            conflict_type=c.get("conflict_type", "unknown"),
            description=c.get("description", ""),
        )
        for c in result.get("conflicts", [])
    ]

    return DetectConflictsResponse(
        conflicts=conflicts,
        summary=result.get("summary", ""),
    )
