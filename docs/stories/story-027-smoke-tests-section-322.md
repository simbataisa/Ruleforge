# Story 027 — Smoke Tests: Section 3.22 (Webhook Auth v2)

**Story ID:** story-027
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Backend Engineer / Tech Lead
**Priority:** P1
**Estimate:** 1 story point
**Depends on:** Stories 022–026 (all implementation complete)

---

## User Story

**As a** tech lead,
**I want** smoke test cases for Custom Header and OAuth 2.0 auth types,
**So that** post-deploy verification confirms both new auth types are functional end-to-end, credentials are masked in responses, and RBAC is enforced.

---

## Context

`scripts/smoke-test.sh` follows a numbered section convention. Epic 5 added section 3.21. This story adds section 3.22 for Epic 6. The smoke test uses `curl` against `localhost` service ports (no Kong). It follows the existing assertion style: `assert_field` + `assert_not_contains`.

**Note on OAuth 2.0 smoke test:** A live OAuth2 token endpoint is not available in the local smoke test environment. The OAuth2 create test validates only that the API contract is correct (201 response, fields masked, HTTPS validation) — not that token acquisition works end-to-end. Token acquisition is covered by unit/integration tests (Story 023).

---

## Acceptance Criteria

1. Section 3.22 is added to `scripts/smoke-test.sh` with 7 test cases.
2. All 7 test cases pass against a running local stack (`./scripts/start-local.sh`).
3. Section follows the existing smoke test format (numbered `TEST_NAME`, `pass`/`fail` output, `TOTAL_PASSED`/`TOTAL_FAILED` counters).
4. `./scripts/smoke-test.sh --quick` skips section 3.22 (health checks only — no regression).

---

## Test Cases to Implement

### 3.22.1 — Create webhook with CUSTOM_HEADER auth type

```bash
# POST /v1/webhooks with authType=CUSTOM_HEADER
# Assert: 201 response
# Assert: response contains "authType":"CUSTOM_HEADER"
# Assert: response contains "headerName":"X-Service-Auth"
# Assert: response does NOT contain the actual headerValue (masked or absent)
```

### 3.22.2 — GET webhook masks headerValue for CUSTOM_HEADER

```bash
# GET /v1/webhooks/{id} for webhook created in 3.22.1
# Assert: response contains "authType":"CUSTOM_HEADER"
# Assert: response contains "headerName":"X-Service-Auth"
# Assert: response does NOT contain "super-secret-value" (the literal value submitted)
# Assert: response contains "[REDACTED]" where headerValue would be
```

### 3.22.3 — Create webhook with OAUTH2_CLIENT_CREDENTIALS auth type

```bash
# POST /v1/webhooks with authType=OAUTH2_CLIENT_CREDENTIALS
# tokenUrl=https://auth.example.com/oauth/token (unreachable — just testing API contract)
# clientId=test-client-id, clientSecret=test-client-secret, scope=api:read
# Assert: 201 response
# Assert: response contains "authType":"OAUTH2_CLIENT_CREDENTIALS"
# Assert: response contains "tokenUrl":"https://auth.example.com/oauth/token"
# Assert: response contains "clientId":"test-client-id"
# Assert: response does NOT contain "test-client-secret"
# Assert: response contains "[REDACTED]" for clientSecret
```

### 3.22.4 — GET webhook masks clientSecret for OAUTH2_CLIENT_CREDENTIALS

```bash
# GET /v1/webhooks/{id} for webhook created in 3.22.3
# Assert: clientId and tokenUrl are present
# Assert: "test-client-secret" NOT in response body
# Assert: "[REDACTED]" present for clientSecret
```

### 3.22.5 — tokenUrl HTTP (not HTTPS) rejected with 400

```bash
# POST /v1/webhooks with authType=OAUTH2_CLIENT_CREDENTIALS
# tokenUrl="http://insecure.example.com/token" (HTTP, not HTTPS)
# Assert: 400 response
# Assert: response body contains "HTTPS" or "tokenUrl"
```

### 3.22.6 — VIEWER cannot create webhook with new auth types

```bash
# POST /v1/webhooks as viewer@ruleforge.io
# authType=CUSTOM_HEADER or OAUTH2_CLIENT_CREDENTIALS
# Assert: 401 or 403 response
```

### 3.22.7 — AUTH_FAILURE status appears in delivery log for invalid auth config

```bash
# Create a webhook with authType=CUSTOM_HEADER and blank headerValue (or a header value
# that will fail — this requires triggering a delivery, so this test may be SKIP'd
# in --quick mode and only run in full mode with a test event trigger)
# Alternatively: verify AUTH_FAILURE is a valid status string by querying delivery history
# of an already-failed-auth webhook if one exists in test data.
# Assert: delivery log entry with status "AUTH_FAILURE" is retrievable
# (if no AUTH_FAILURE entry exists yet, this test is informational — skip with NOTE)
```

---

## Implementation Reference

Follow the style from section 3.21 of `scripts/smoke-test.sh`:

```bash
echo ""
echo "=== Section 3.22: Webhook Auth v2 — OAuth 2.0 & Custom Header ==="

# --- 3.22.1: Create CUSTOM_HEADER webhook ---
TEST_NAME="3.22.1 POST /v1/webhooks authType=CUSTOM_HEADER"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$EVENT_SVC/v1/webhooks" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Custom Header Test",
    "url": "https://httpbin.org/post",
    "events": ["rule.published"],
    "authType": "CUSTOM_HEADER",
    "authConfig": { "headerName": "X-Service-Auth", "headerValue": "super-secret-value" }
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -1)
CUSTOM_HEADER_WEBHOOK_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
if [ "$HTTP_CODE" = "201" ] && echo "$BODY" | grep -q '"CUSTOM_HEADER"' && ! echo "$BODY" | grep -q "super-secret-value"; then
  echo "PASS: $TEST_NAME"
  ((TOTAL_PASSED++))
else
  echo "FAIL: $TEST_NAME (HTTP $HTTP_CODE)"
  [ "$VERBOSE" = "true" ] && echo "  Body: $BODY"
  ((TOTAL_FAILED++))
fi
```

*(Repeat pattern for 3.22.2 through 3.22.7)*

---

## Definition of Done

- [ ] Section 3.22 added to `scripts/smoke-test.sh` with 7 test cases
- [ ] All 7 test cases pass against local running stack
- [ ] `./scripts/smoke-test.sh --quick` does not run section 3.22 (or skips gracefully)
- [ ] Test output follows `PASS: 3.22.X` / `FAIL: 3.22.X` format consistent with other sections
- [ ] `CUSTOM_HEADER_WEBHOOK_ID` and `OAUTH2_WEBHOOK_ID` captured for use in subsequent GET tests within the section
- [ ] Section header `=== Section 3.22: Webhook Auth v2 — OAuth 2.0 & Custom Header ===` printed
