import logging

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.models.requests import (
    DetectConflictsRequest,
    ExplainRuleRequest,
    GenerateRuleRequest,
    GenerateTestDataRequest,
    NaturalLanguageQueryRequest,
    OptimizeRuleRequest,
)
from app.models.responses import (
    DetectConflictsResponse,
    ExplainRuleResponse,
    GenerateRuleResponse,
    GenerateTestDataResponse,
    NaturalLanguageQueryResponse,
    OptimizeRuleResponse,
)
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

app = FastAPI(
    title="RuleForge AI Assistant",
    description="LLM-powered rule generation and optimization",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _check_api_key() -> None:
    """Raise 503 if the Anthropic API key is not configured."""
    if not llm_client.is_configured:
        raise HTTPException(
            status_code=503,
            detail=(
                "Anthropic API key not configured. "
                "Set the ANTHROPIC_API_KEY environment variable."
            ),
        )


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "llm_configured": llm_client.is_configured,
        "model": settings.anthropic_model,
    }


@app.post("/v1/ai/generate-rule", response_model=GenerateRuleResponse)
async def generate_rule_endpoint(request: GenerateRuleRequest):
    _check_api_key()
    from app.services.rule_generator import generate_rule

    return await generate_rule(request)


@app.post("/v1/ai/explain-rule", response_model=ExplainRuleResponse)
async def explain_rule_endpoint(request: ExplainRuleRequest):
    _check_api_key()
    from app.services.rule_explainer import explain_rule

    return await explain_rule(request)


@app.post("/v1/ai/optimize-rule", response_model=OptimizeRuleResponse)
async def optimize_rule_endpoint(request: OptimizeRuleRequest):
    _check_api_key()
    from app.services.rule_optimizer import optimize_rule

    return await optimize_rule(request)


@app.post("/v1/ai/generate-test-data", response_model=GenerateTestDataResponse)
async def generate_test_data_endpoint(request: GenerateTestDataRequest):
    _check_api_key()
    from app.services.test_data_generator import generate_test_data

    return await generate_test_data(request)


@app.post("/v1/ai/detect-conflicts", response_model=DetectConflictsResponse)
async def detect_conflicts_endpoint(request: DetectConflictsRequest):
    _check_api_key()
    from app.services.conflict_detector import detect_conflicts

    return await detect_conflicts(request)


@app.post("/v1/ai/query", response_model=NaturalLanguageQueryResponse)
async def nl_query_endpoint(request: NaturalLanguageQueryRequest):
    _check_api_key()
    from app.services.nl_query import nl_query

    return await nl_query(request)
