# RuleForge Python SDK

A Python 3.10+ client SDK for the RuleForge BRMS REST API, built on `httpx`.

## Installation

```bash
pip install ruleforge-sdk
```

Or directly from source:

```bash
pip install ./sdks/sdk-python
```

## Usage

### Initialize the client

```python
from ruleforge_sdk import RuleForgeClient

# API key authentication
client = RuleForgeClient(base_url="https://api.ruleforge.io", api_key="your-api-key")

# JWT authentication
client = RuleForgeClient(base_url="https://api.ruleforge.io", jwt_token="your-jwt-token")
```

### Solve a rule

```python
result = client.solve("loan-eligibility", {
    "age": 30,
    "income": 75000,
    "credit_score": 720,
})
print(result["output"])
print(result["matchedRules"])
print(result["executionTimeMs"])
```

### Solve in bulk

```python
results = client.solve_bulk("loan-eligibility", [
    {"age": 25, "income": 50000},
    {"age": 45, "income": 120000},
])
```

### List rules in a space

```python
rules = client.list_rules("your-space-id")
```

### Get a specific rule

```python
rule = client.get_rule("your-rule-id")
print(rule["name"])
```

## Error Handling

```python
import httpx
from ruleforge_sdk import RuleForgeClient

client = RuleForgeClient(base_url="https://api.ruleforge.io", api_key="key")

try:
    result = client.solve("my-rule", {"field": "value"})
except httpx.HTTPStatusError as e:
    print(f"API error {e.response.status_code}: {e.response.text}")
except httpx.RequestError as e:
    print(f"Network error: {e}")
```
