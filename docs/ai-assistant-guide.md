# AI Assistant Guide

The RuleForge AI Assistant is a Python/FastAPI service that provides LLM-powered capabilities for rule generation, optimization, conflict detection, and natural-language querying. It runs on port 8000 internally (mapped to 8088 on the host) and is accessible through Kong at `/v1/ai`.

## Configuration

The AI service is configured via environment variables with the prefix `RULEFORGE_AI_`.

| Variable | Description | Default |
|----------|-------------|---------|
| `RULEFORGE_AI_LLM_API_KEY` | Anthropic API key for Claude | (required) |
| `RULEFORGE_AI_LLM_MODEL` | Model identifier | `claude-sonnet-4-5-20250929` |
| `RULEFORGE_AI_MANAGEMENT_SERVICE_URL` | Management service URL for rule lookups | `http://localhost:8082` |
| `RULEFORGE_AI_DEBUG` | Enable debug logging | `false` |

### Docker Compose

When running via `make stack-up`, set the `ANTHROPIC_API_KEY` environment variable on the host. The docker-compose configuration maps it to `RULEFORGE_AI_LLM_API_KEY` automatically:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
make stack-up
```

### Local Development

```bash
cd ai-service
export RULEFORGE_AI_LLM_API_KEY=sk-ant-...
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## API Endpoints

All endpoints are served under the `/v1/ai` prefix. When using Kong, the base URL is `http://localhost:8000/v1/ai`.

### 1. Generate Decision Table

Generate a decision table from a natural-language description of business rules.

```bash
curl -X POST http://localhost:8000/v1/ai/generate-table \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Approve loans under $50k for applicants with credit score above 700 and debt-to-income ratio below 0.4. Deny all others.",
    "spaceId": "space-uuid",
    "folderId": "folder-uuid"
  }'
```

### 2. Suggest Optimization

Analyze an existing rule and suggest performance or logic improvements.

```bash
curl -X POST http://localhost:8000/v1/ai/suggest-optimization \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "rule-uuid",
    "version": 1
  }'
```

### 3. Explain Rule

Return a human-readable explanation of what a rule does, its conditions, and its outcomes.

```bash
curl -X POST http://localhost:8000/v1/ai/explain-rule \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "rule-uuid",
    "version": 1
  }'
```

### 4. Generate Test Data

Create sample input datasets that exercise all branches and conditions of a rule.

```bash
curl -X POST http://localhost:8000/v1/ai/generate-test-data \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "rule-uuid",
    "version": 1,
    "count": 20
  }'
```

### 5. Detect Conflicts

Analyze a set of rules within a folder for overlapping conditions, contradictions, or redundancies.

```bash
curl -X POST http://localhost:8000/v1/ai/detect-conflicts \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "folderId": "folder-uuid"
  }'
```

### 6. Natural Language Query

Query rules using plain English. The AI translates the question into rule lookups and returns matching results.

```bash
curl -X POST http://localhost:8000/v1/ai/natural-language-query \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Which rules affect loan approval for applicants in California?",
    "spaceId": "space-uuid"
  }'
```

## Integration with Rule Management Workflow

The AI assistant integrates with the rule management lifecycle at several points:

1. **Rule Creation** -- Use `/generate-table` to bootstrap a new decision table from requirements written in plain English. The generated rule can be saved directly through the management service.

2. **Rule Review** -- Use `/explain-rule` to produce documentation-quality explanations for peer review, and `/detect-conflicts` to verify that new rules do not contradict existing ones in the same folder.

3. **Testing** -- Use `/generate-test-data` to create comprehensive test inputs that cover edge cases and boundary conditions, ensuring all rule paths are exercised before promotion.

4. **Optimization** -- Use `/suggest-optimization` after rules are in production to identify conditions that can be reordered for performance, redundant checks, or opportunities to consolidate rules.

5. **Discovery** -- Use `/natural-language-query` to find relevant rules across spaces and folders without needing to know exact names or IDs.

## Health Check

```bash
curl http://localhost:8000/health
# {"status": "ok"}
```
