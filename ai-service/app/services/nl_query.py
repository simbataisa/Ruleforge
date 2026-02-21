import json
import logging

import httpx

from app.config import settings
from app.models.requests import NaturalLanguageQueryRequest
from app.models.responses import NaturalLanguageQueryResponse
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

SEARCH_TOOL = {
    "name": "search_rules",
    "description": "Search for rules in the management service",
    "input_schema": {
        "type": "object",
        "properties": {
            "search_terms": {
                "type": "array",
                "items": {"type": "string"},
                "description": "Keywords to search for",
            },
            "rule_type_filter": {
                "type": "string",
                "enum": ["DECISION_TABLE", "DECISION_TREE", ""],
                "description": "Optional rule type filter",
            },
        },
        "required": ["search_terms"],
    },
}

SYSTEM_PROMPT = """You are a natural language search assistant for the RuleForge BRMS platform.
The user will ask questions about rules in their workspace. Parse their intent and extract search terms.

Examples:
- "Show me all pricing rules" -> search_terms: ["pricing"], rule_type_filter: ""
- "Find decision tables about shipping" -> search_terms: ["shipping"], rule_type_filter: "DECISION_TABLE"
- "What rules handle customer discounts?" -> search_terms: ["customer", "discount"], rule_type_filter: ""
"""


async def _search_management_service(
    space_id: str, search_terms: list[str], rule_type_filter: str
) -> list[dict]:
    """Search rules via management service API."""
    url = f"{settings.management_service_url}/v1/rules"
    params: dict = {"spaceId": space_id, "q": " ".join(search_terms)}
    if rule_type_filter:
        params["ruleType"] = rule_type_filter

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(url, params=params)
            if resp.status_code == 200:
                data = resp.json()
                return data.get("content", data) if isinstance(data, dict) else data
            logger.warning("Management service search failed: HTTP %d", resp.status_code)
    except httpx.HTTPError as e:
        logger.warning("Management service search error: %s", str(e))
    return []


async def nl_query(
    request: NaturalLanguageQueryRequest,
) -> NaturalLanguageQueryResponse:
    # Step 1: Parse query intent
    parse_result = await llm_client.call_with_tools(
        system_prompt=SYSTEM_PROMPT,
        user_message=request.query,
        tools=[SEARCH_TOOL],
    )

    search_terms = parse_result.get("search_terms", [request.query])
    rule_type_filter = parse_result.get("rule_type_filter", "")

    # Step 2: Search management service
    rules = await _search_management_service(
        request.space_id, search_terms, rule_type_filter
    )

    rule_ids = [r.get("id", "") for r in rules if r.get("id")]

    # Step 3: Generate natural language answer
    if rules:
        rules_summary = json.dumps(
            [{"id": r.get("id"), "name": r.get("name"), "type": r.get("ruleType")} for r in rules[:10]],
            indent=2,
        )
        answer_prompt = (
            f"The user asked: '{request.query}'\n\n"
            f"Found {len(rules)} matching rules:\n{rules_summary}\n\n"
            f"Provide a brief natural language answer summarizing the results."
        )
    else:
        answer_prompt = (
            f"The user asked: '{request.query}'\n\n"
            f"No matching rules were found. Provide a helpful response."
        )

    answer = await llm_client.call(
        system_prompt="You are a helpful assistant for the RuleForge platform.",
        user_message=answer_prompt,
        max_tokens=1024,
    )

    return NaturalLanguageQueryResponse(
        answer=answer,
        relevant_rule_ids=rule_ids,
    )
