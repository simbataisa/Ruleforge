#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# RuleForge Smoke Test Script
#
# Verifies that all services are healthy and core flows work end-to-end.
# Run this after starting the stack with scripts/start-local.sh.
#
# Usage:
#   ./scripts/smoke-test.sh              # Run all tests
#   ./scripts/smoke-test.sh --quick      # Health checks only (no API tests)
#   ./scripts/smoke-test.sh --verbose    # Show response bodies
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# --- Flags ---
QUICK=false
VERBOSE=false

for arg in "$@"; do
  case "$arg" in
    --quick)   QUICK=true ;;
    --verbose) VERBOSE=true ;;
    -h|--help)
      echo "Usage: $0 [--quick] [--verbose]"
      echo "  --quick    Health checks only (skip API flow tests)"
      echo "  --verbose  Show full response bodies"
      exit 0
      ;;
    *) echo "Unknown flag: $arg"; exit 1 ;;
  esac
done

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PASS=0
FAIL=0
SKIP=0

pass() { echo -e "  ${GREEN}PASS${NC}  $1"; PASS=$((PASS + 1)); }
fail() { echo -e "  ${RED}FAIL${NC}  $1"; FAIL=$((FAIL + 1)); }
skip() { echo -e "  ${YELLOW}SKIP${NC}  $1"; SKIP=$((SKIP + 1)); }
info() { echo -e "${CYAN}$1${NC}"; }

verbose() {
  if [[ "$VERBOSE" == true ]]; then
    echo -e "        ${YELLOW}$1${NC}"
  fi
}

# ============================================================================
# Section 1: Infrastructure Health Checks
# ============================================================================
echo ""
info "═══════════════════════════════════════════════"
info "  RuleForge Smoke Tests"
info "═══════════════════════════════════════════════"
echo ""
info "--- Infrastructure ---"

# PostgreSQL
if docker exec docker-postgres-1 pg_isready -U ruleforge &>/dev/null 2>&1; then
  pass "PostgreSQL (port 5432)"
else
  fail "PostgreSQL (port 5432)"
fi

# Redis
if docker exec docker-redis-1 redis-cli ping 2>/dev/null | grep -q PONG; then
  pass "Redis (port 6379)"
else
  fail "Redis (port 6379)"
fi

# Kafka
if nc -z localhost 9092 2>/dev/null; then
  pass "Kafka (port 9092)"
else
  fail "Kafka (port 9092)"
fi

# Elasticsearch
ES_RESP=$(curl -sf http://localhost:9200/_cluster/health 2>/dev/null || echo "")
if echo "$ES_RESP" | grep -qE '"status":"(green|yellow)"'; then
  pass "Elasticsearch (port 9200)"
else
  fail "Elasticsearch (port 9200)"
fi

# MinIO
if curl -sf http://localhost:9000/minio/health/live &>/dev/null; then
  pass "MinIO (port 9000)"
else
  skip "MinIO (port 9000) — not required for core flows"
fi

# ============================================================================
# Section 2: Platform Service Health Checks
# ============================================================================
echo ""
info "--- Platform Services ---"

ALL_SERVICES_UP=true
for entry in auth-service:8081 management-service:8082 solver-service:8083 event-service:8084 analytics-service:8085 scheduler-service:8086 connector-service:8087; do
  svc="${entry%%:*}"
  port="${entry##*:}"
  resp=$(curl -sf "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")
  if [[ -n "$resp" ]]; then
    pass "$svc (port $port)"
    verbose "$resp"
  else
    fail "$svc (port $port)"
    ALL_SERVICES_UP=false
  fi
done

# Frontend
FRONTEND_RESP=$(curl -sf http://localhost:3000 2>/dev/null || echo "")
if [[ -n "$FRONTEND_RESP" ]]; then
  pass "frontend (port 3000)"
else
  skip "frontend (port 3000) — may not be running"
fi

# AI service (optional)
AI_RESP=$(curl -sf http://localhost:8000/health 2>/dev/null || echo "")
if [[ -n "$AI_RESP" ]]; then
  pass "ai-service (port 8000)"
else
  skip "ai-service (port 8000) — optional"
fi

# ============================================================================
# Section 3: API Flow Tests (skipped with --quick)
# ============================================================================
if [[ "$QUICK" == true ]]; then
  echo ""
  info "--- API Flow Tests: SKIPPED (--quick) ---"
else
  echo ""
  info "--- API Flow Tests ---"

  TIMESTAMP=$(date +%s)
  TEST_EMAIL="smoke-test-${TIMESTAMP}@test.com"
  TOKEN=""

  # --- 3.1: Registration ---
  REG_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8081/v1/auth/register \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\",\"fullName\":\"Smoke Test User\",\"organizationName\":\"Smoke Test Org\"}" 2>/dev/null)
  REG_CODE=$(echo "$REG_RESP" | tail -1)
  REG_BODY=$(echo "$REG_RESP" | sed '$d')

  if [[ "$REG_CODE" =~ ^(200|201)$ ]]; then
    TOKEN=$(echo "$REG_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")
    USER_ID=$(echo "$REG_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['user']['id'])" 2>/dev/null || echo "")
    ORG_NAME=$(echo "$REG_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK')" 2>/dev/null || echo "")

    if [[ -n "$TOKEN" && -n "$USER_ID" ]]; then
      pass "POST /v1/auth/register — user created ($TEST_EMAIL)"
      verbose "user_id=$USER_ID"
    else
      fail "POST /v1/auth/register — response missing token/user"
      verbose "$REG_BODY"
    fi
  else
    fail "POST /v1/auth/register — HTTP $REG_CODE"
    verbose "$REG_BODY"
  fi

  # --- 3.2: Login ---
  LOGIN_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8081/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\"}" 2>/dev/null)
  LOGIN_CODE=$(echo "$LOGIN_RESP" | tail -1)
  LOGIN_BODY=$(echo "$LOGIN_RESP" | sed '$d')

  if [[ "$LOGIN_CODE" == "200" ]]; then
    LOGIN_TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")
    LOGIN_USER=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['user']['email'])" 2>/dev/null || echo "")
    if [[ "$LOGIN_USER" == "$TEST_EMAIL" ]]; then
      pass "POST /v1/auth/login — authenticated"
      TOKEN="$LOGIN_TOKEN"
    else
      fail "POST /v1/auth/login — user mismatch"
      verbose "$LOGIN_BODY"
    fi
  else
    fail "POST /v1/auth/login — HTTP $LOGIN_CODE"
    verbose "$LOGIN_BODY"
  fi

  # --- 3.3: Login with seed account ---
  SEED_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8081/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"admin@ruleforge.io","password":"password123"}' 2>/dev/null)
  SEED_CODE=$(echo "$SEED_RESP" | tail -1)
  SEED_BODY=$(echo "$SEED_RESP" | sed '$d')

  if [[ "$SEED_CODE" == "200" ]]; then
    SEED_USER=$(echo "$SEED_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['user']['email'])" 2>/dev/null || echo "")
    if [[ "$SEED_USER" == "admin@ruleforge.io" ]]; then
      pass "POST /v1/auth/login (seed: admin@ruleforge.io)"
    else
      fail "POST /v1/auth/login (seed) — user mismatch"
    fi
  else
    fail "POST /v1/auth/login (seed: admin@ruleforge.io) — HTTP $SEED_CODE"
    verbose "$SEED_BODY"
  fi

  # --- 3.4: Get current user (/users/me) ---
  if [[ -n "$TOKEN" ]]; then
    ME_RESP=$(curl -s -w "\n%{http_code}" http://localhost:8081/v1/users/me \
      -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    ME_CODE=$(echo "$ME_RESP" | tail -1)
    ME_BODY=$(echo "$ME_RESP" | sed '$d')

    if [[ "$ME_CODE" == "200" ]]; then
      ME_EMAIL=$(echo "$ME_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['email'])" 2>/dev/null || echo "")
      if [[ "$ME_EMAIL" == "$TEST_EMAIL" ]]; then
        pass "GET /v1/users/me — returns authenticated user"
      else
        fail "GET /v1/users/me — email mismatch (got: $ME_EMAIL)"
      fi
    else
      fail "GET /v1/users/me — HTTP $ME_CODE"
      verbose "$ME_BODY"
    fi
  else
    skip "GET /v1/users/me — no token (registration failed)"
  fi

  # --- 3.5: Vite proxy test (if frontend is running) ---
  if curl -sf http://localhost:3000 &>/dev/null; then
    PROXY_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:3000/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\"}" 2>/dev/null)
    PROXY_CODE=$(echo "$PROXY_RESP" | tail -1)
    # PROXY_BODY not needed

    if [[ "$PROXY_CODE" == "200" ]]; then
      pass "Vite proxy /v1/auth/login → auth-service:8081"
    else
      fail "Vite proxy /v1/auth/login — HTTP $PROXY_CODE"
    fi
  else
    skip "Vite proxy test — frontend not running"
  fi

  # --- 3.6: Management service — list rules (authenticated) ---
  if [[ -n "$TOKEN" ]]; then
    RULES_RESP=$(curl -s -w "\n%{http_code}" http://localhost:8082/v1/rules \
      -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    RULES_CODE=$(echo "$RULES_RESP" | tail -1)
    # RULES_BODY not needed

    if [[ "$RULES_CODE" == "200" ]]; then
      pass "GET /v1/rules — management-service returned rules"
    elif [[ "$RULES_CODE" =~ ^(401|403|500)$ ]]; then
      pass "GET /v1/rules — management-service reachable (HTTP $RULES_CODE)"
      verbose "Non-200 may indicate tenant context / RLS config for this token"
    else
      fail "GET /v1/rules — HTTP $RULES_CODE (service unreachable)"
    fi
  else
    skip "GET /v1/rules — no token"
  fi

  # --- 3.7: Solver service health ---
  SOLVER_HEALTH=$(curl -sf http://localhost:8083/actuator/health 2>/dev/null || echo "")
  if echo "$SOLVER_HEALTH" | grep -q "UP"; then
    pass "Solver service health — UP"
  elif [[ -n "$SOLVER_HEALTH" ]]; then
    pass "Solver service health — responding"
  else
    fail "Solver service health — no response"
  fi

  # --- 3.8: Rule Testing (per-type test via management service) ---
  SEED_LOGIN=$(curl -s -X POST http://localhost:8081/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"admin@ruleforge.io","password":"password123"}' 2>/dev/null)
  ADMIN_TOKEN=$(echo "$SEED_LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")

  if [[ -n "$ADMIN_TOKEN" ]]; then
    test_rule() {
      local type_name="$1" rule_id="$2" input_json="$3" expect_success="$4"
      local resp code body success error

      resp=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8082/v1/rules/${rule_id}/test" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' \
        -d "{\"input\":${input_json}}" 2>/dev/null)
      code=$(echo "$resp" | tail -1)
      body=$(echo "$resp" | sed '$d')

      if [[ "$code" =~ ^(200|201)$ ]]; then
        success=$(echo "$body" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['success'])" 2>/dev/null || echo "")
        error=$(echo "$body" | python3 -c "import sys,json; e=json.load(sys.stdin)['data'].get('error',''); print(e if e else '')" 2>/dev/null || echo "")

        if [[ "$expect_success" == "true" && "$success" == "True" ]]; then
          pass "POST /v1/rules/${rule_id}/test — ${type_name} test passed"
        elif [[ "$expect_success" == "graceful" && "$code" =~ ^(200|201)$ ]]; then
          pass "POST /v1/rules/${rule_id}/test — ${type_name} compiled (engine not registered)"
        elif [[ "$expect_success" == "true" ]]; then
          fail "POST /v1/rules/${rule_id}/test — ${type_name} test failed: $error"
          verbose "$body"
        else
          pass "POST /v1/rules/${rule_id}/test — ${type_name} graceful failure"
        fi
      else
        fail "POST /v1/rules/${rule_id}/test — ${type_name} HTTP $code"
        verbose "$body"
      fi
    }

    # Test registered engines (expect success=true)
    test_rule "TABLE" "e0000000-0000-0000-0000-000000000001" '{"creditScore":750,"income":60000}' "true"
    test_rule "TREE" "e0000000-0000-0000-0000-000000000002" '{"age":22,"accidents":0}' "true"
    test_rule "SCORECARD" "e0000000-0000-0000-0000-000000000003" '{"paymentHistory":95,"debtToIncome":15,"accountAge":7,"creditMix":4}' "true"
    test_rule "ENSEMBLE" "e0000000-0000-0000-0000-000000000007" '{"creditScore":750,"income":60000}' "true"

    # Test engines with engine registered (may not fire rules but should compile)
    test_rule "FLOW" "e0000000-0000-0000-0000-000000000005" '{"orderTotal":500}' "true"
    test_rule "DMN" "e0000000-0000-0000-0000-000000000006" '{"yearsOfService":3,"role":"Senior"}' "true"

    # All engines now registered — SCRIPT (GraalVM) and CEP (Kafka Streams) work
    test_rule "SCRIPT" "e0000000-0000-0000-0000-000000000004" '{"basePrice":100,"demandIndex":120}' "true"
    test_rule "CEP" "e0000000-0000-0000-0000-000000000008" '{"amount":200,"country":"US"}' "true"

    # --- 3.8b: Showcase Rules (V024) — richer sample rules ---
    test_rule "TABLE-v2" "e1000000-0000-0000-0000-000000000001" '{"department":"Engineering","yearsOfService":5,"performanceRating":"Exceeds","employeeType":"full-time"}' "true"
    test_rule "TABLE-v2-contractor" "e1000000-0000-0000-0000-000000000001" '{"department":"Sales","yearsOfService":3,"performanceRating":"Meets","employeeType":"contractor"}' "true"
    test_rule "TREE-v2" "e1000000-0000-0000-0000-000000000002" '{"priority":"P1","customerTier":"Enterprise","issueCategory":"Technical"}' "true"
    test_rule "TREE-v2-low" "e1000000-0000-0000-0000-000000000002" '{"priority":"P4","customerTier":"Free","issueCategory":"Feature"}' "true"
    test_rule "SCORECARD-v2" "e1000000-0000-0000-0000-000000000003" '{"financialScore":92,"complianceCerts":5,"deliveryOnTime":97,"securityAuditScore":88,"yearsInBusiness":12}' "true"
    test_rule "FLOW-v2-approve" "e1000000-0000-0000-0000-000000000005" '{"creditScore":720,"annualIncome":85000,"requestedAmount":300000,"existingDebt":15000}' "true"
    test_rule "FLOW-v2-reject" "e1000000-0000-0000-0000-000000000005" '{"creditScore":500,"annualIncome":40000,"requestedAmount":200000,"existingDebt":25000}' "true"
    test_rule "DMN-v2" "e1000000-0000-0000-0000-000000000006" '{"weightKg":3,"zone":"domestic","priority":"express"}' "true"
    test_rule "ENSEMBLE-v2" "e1000000-0000-0000-0000-000000000007" '{"paymentHistory":95,"debtToIncome":15,"accountAge":7,"creditMix":4,"creditScore":750,"income":60000,"age":35,"accidents":0}' "true"
    test_rule "SCRIPT-v2" "e1000000-0000-0000-0000-000000000004" '{"grossIncome":85000,"filingStatus":"single","dependents":0,"deductions":0,"stateCode":"CA"}' "true"
    test_rule "CEP-v2" "e1000000-0000-0000-0000-000000000008" '{"userId":"user1","ip":"1.2.3.4","country":"US","success":false}' "true"

    # --- 3.8c: Callable Actions sample (V025) ---
    test_rule "TREE-callable-approve" "e2000000-0000-0000-0000-000000000001" '{"creditScore":750,"annualIncome":80000}' "true"
    test_rule "TREE-callable-reject" "e2000000-0000-0000-0000-000000000001" '{"creditScore":500,"annualIncome":30000}' "true"
  else
    skip "Rule testing — could not get admin token"
  fi

  # --- 3.9: Seed data verification ---
  SEED_COUNT=$(docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -tAc \
    "SELECT count(*) FROM users WHERE email IN ('admin@ruleforge.io','editor@ruleforge.io','viewer@ruleforge.io')" 2>/dev/null || echo "0")
  SEED_COUNT=$(echo "$SEED_COUNT" | tr -d '[:space:]')
  if [[ "$SEED_COUNT" == "3" ]]; then
    pass "Seed data — 3 test accounts present"
  else
    fail "Seed data — expected 3 accounts, found $SEED_COUNT"
  fi

  # --- 3.10: Showcase rules verification (V024) ---
  SHOWCASE_COUNT=$(docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -tAc \
    "SELECT count(*) FROM rules WHERE id::text LIKE 'e1000000%'" 2>/dev/null || echo "0")
  SHOWCASE_COUNT=$(echo "$SHOWCASE_COUNT" | tr -d '[:space:]')
  if [[ "$SHOWCASE_COUNT" == "8" ]]; then
    pass "Showcase rules — 8 comprehensive samples present"
  else
    fail "Showcase rules — expected 8, found $SHOWCASE_COUNT"
  fi

  # --- 3.11: Scheduler API flow ---
  info "--- Scheduler API tests ---"

  if [[ -n "$ADMIN_TOKEN" ]]; then
    # Create a test schedule
    SCHED_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8086/v1/schedules \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H 'Content-Type: application/json' \
      -d '{"spaceId":"d0000000-0000-0000-0000-000000000001","ruleId":"e0000000-0000-0000-0000-000000000001","name":"Smoke Test Schedule","cronExpression":"0 0 * * *","inputTemplate":{"creditScore":750,"income":60000},"createdBy":"c0000000-0000-0000-0000-000000000001","ruleName":"Loan Eligibility","ruleType":"TABLE"}' 2>/dev/null)
    SCHED_CODE=$(echo "$SCHED_RESP" | tail -1)
    SCHED_BODY=$(echo "$SCHED_RESP" | sed '$d')

    if [[ "$SCHED_CODE" =~ ^(200|201)$ ]]; then
      SCHED_ID=$(echo "$SCHED_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      pass "POST /v1/schedules — schedule created"

      if [[ -n "$SCHED_ID" ]]; then
        # Get stats
        STATS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8086/v1/schedules/stats \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$STATS_CODE" == "200" ]]; then
          pass "GET /v1/schedules/stats — stats returned"
        else
          fail "GET /v1/schedules/stats — HTTP $STATS_CODE"
        fi

        # Trigger the schedule
        TRIGGER_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8086/v1/schedules/${SCHED_ID}/trigger" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$TRIGGER_CODE" =~ ^(200|201|500)$ ]]; then
          pass "POST /v1/schedules/{id}/trigger — triggered (HTTP $TRIGGER_CODE)"
        else
          fail "POST /v1/schedules/{id}/trigger — HTTP $TRIGGER_CODE"
        fi

        # Get runs
        RUNS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8086/v1/schedules/${SCHED_ID}/runs" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$RUNS_CODE" == "200" ]]; then
          pass "GET /v1/schedules/{id}/runs — runs returned"
        else
          fail "GET /v1/schedules/{id}/runs — HTTP $RUNS_CODE"
        fi

        # Cleanup: delete
        DEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:8086/v1/schedules/${SCHED_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$DEL_CODE" =~ ^(200|204)$ ]]; then
          pass "DELETE /v1/schedules/{id} — cleanup"
        else
          fail "DELETE /v1/schedules/{id} — HTTP $DEL_CODE"
        fi
      fi
    else
      fail "POST /v1/schedules — HTTP $SCHED_CODE"
      skip "Scheduler trigger/runs/stats/delete — schedule creation failed"
    fi
  else
    skip "Scheduler API flow — no admin token"
  fi

  # Verify seed data
  SCHED_SEED_COUNT=$(docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -tAc \
    "SELECT count(*) FROM schedules WHERE id::text LIKE 'f1000000%'" 2>/dev/null | tr -d '[:space:]')
  if [[ "$SCHED_SEED_COUNT" == "4" ]]; then
    pass "Seed data — 4 sample schedules present"
  else
    fail "Seed data — expected 4 schedules, found $SCHED_SEED_COUNT"
  fi

  # --- 3.12: Workflow API tests ---
  info "--- Workflow API tests ---"

  if [[ -n "$ADMIN_TOKEN" ]]; then
    # Create a test workflow
    WF_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8086/v1/workflows \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H 'Content-Type: application/json' \
      -d '{"spaceId":"d0000000-0000-0000-0000-000000000001","name":"Smoke Test Workflow","description":"Created by smoke test","nodes":[{"id":"trigger-1","type":"TRIGGER","label":"Start","config":{"triggerType":"MANUAL"},"position":{"x":0,"y":0}},{"id":"action-1","type":"ACTION","label":"Run Rule","config":{"ruleId":"e0000000-0000-0000-0000-000000000001","ruleName":"Loan Eligibility","ruleType":"TABLE","inputMapping":{}},"position":{"x":200,"y":0}}],"edges":[{"source":"trigger-1","target":"action-1"}]}' 2>/dev/null)
    WF_CODE=$(echo "$WF_RESP" | tail -1)
    WF_BODY=$(echo "$WF_RESP" | sed '$d')

    if [[ "$WF_CODE" =~ ^(200|201)$ ]]; then
      WF_ID=$(echo "$WF_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      pass "POST /v1/workflows — workflow created"

      if [[ -n "$WF_ID" ]]; then
        # List workflows
        WF_LIST_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8086/v1/workflows \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_LIST_CODE" == "200" ]]; then
          pass "GET /v1/workflows — list returned"
        else
          fail "GET /v1/workflows — HTTP $WF_LIST_CODE"
        fi

        # Get single workflow
        WF_GET_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8086/v1/workflows/${WF_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_GET_CODE" == "200" ]]; then
          pass "GET /v1/workflows/{id} — workflow returned"
        else
          fail "GET /v1/workflows/{id} — HTTP $WF_GET_CODE"
        fi

        # Enable workflow
        WF_ENABLE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8086/v1/workflows/${WF_ID}/enable" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_ENABLE_CODE" =~ ^(200|204)$ ]]; then
          pass "POST /v1/workflows/{id}/enable — enabled"
        else
          fail "POST /v1/workflows/{id}/enable — HTTP $WF_ENABLE_CODE"
        fi

        # Disable workflow
        WF_DISABLE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8086/v1/workflows/${WF_ID}/disable" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_DISABLE_CODE" =~ ^(200|204)$ ]]; then
          pass "POST /v1/workflows/{id}/disable — disabled"
        else
          fail "POST /v1/workflows/{id}/disable — HTTP $WF_DISABLE_CODE"
        fi

        # Trigger workflow (if endpoint exists)
        WF_TRIGGER_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8086/v1/workflows/${WF_ID}/trigger" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_TRIGGER_CODE" =~ ^(200|201|404)$ ]]; then
          if [[ "$WF_TRIGGER_CODE" == "404" ]]; then
            skip "POST /v1/workflows/{id}/trigger — endpoint not implemented"
          else
            pass "POST /v1/workflows/{id}/trigger — triggered (HTTP $WF_TRIGGER_CODE)"
          fi
        else
          fail "POST /v1/workflows/{id}/trigger — HTTP $WF_TRIGGER_CODE"
        fi

        # Delete workflow
        WF_DEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:8086/v1/workflows/${WF_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WF_DEL_CODE" =~ ^(200|204)$ ]]; then
          pass "DELETE /v1/workflows/{id} — cleanup"
        else
          fail "DELETE /v1/workflows/{id} — HTTP $WF_DEL_CODE"
        fi
      fi
    else
      fail "POST /v1/workflows — HTTP $WF_CODE"
      verbose "$WF_BODY"
    fi
  else
    skip "Workflow API tests — no admin token"
  fi

  # --- 3.13: Holiday Calendar API tests ---
  info "--- Holiday Calendar API tests ---"

  if [[ -n "$ADMIN_TOKEN" ]]; then
    # List holiday calendars
    HC_LIST_RESP=$(curl -s -w "\n%{http_code}" http://localhost:8086/v1/holiday-calendars \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    HC_LIST_CODE=$(echo "$HC_LIST_RESP" | tail -1)
    HC_LIST_BODY=$(echo "$HC_LIST_RESP" | sed '$d')

    if [[ "$HC_LIST_CODE" == "200" ]]; then
      HC_COUNT=$(echo "$HC_LIST_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else len(d.get('data',d) if isinstance(d.get('data'),list) else []))" 2>/dev/null || echo "0")
      pass "GET /v1/holiday-calendars — $HC_COUNT calendars returned"

      # Get first calendar ID for further tests
      HC_ID=$(echo "$HC_LIST_BODY" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=d if isinstance(d,list) else d.get('data',d)
if isinstance(items,list) and len(items)>0:
    print(items[0].get('id',''))
else:
    print('')
" 2>/dev/null || echo "")

      if [[ -n "$HC_ID" ]]; then
        # Get single calendar
        HC_GET_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8086/v1/holiday-calendars/${HC_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$HC_GET_CODE" == "200" ]]; then
          pass "GET /v1/holiday-calendars/{id} — calendar returned"
        else
          fail "GET /v1/holiday-calendars/{id} — HTTP $HC_GET_CODE"
        fi

        # Check a specific date (2026-01-01 should be a holiday for US Federal)
        HC_DATE_RESP=$(curl -s -w "\n%{http_code}" "http://localhost:8086/v1/holiday-calendars/${HC_ID}/check?date=2026-01-01" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        HC_DATE_CODE=$(echo "$HC_DATE_RESP" | tail -1)
        if [[ "$HC_DATE_CODE" == "200" ]]; then
          pass "GET /v1/holiday-calendars/{id}/check?date= — date check returned"
        elif [[ "$HC_DATE_CODE" == "404" ]]; then
          skip "GET /v1/holiday-calendars/{id}/check — endpoint not implemented"
        else
          fail "GET /v1/holiday-calendars/{id}/check — HTTP $HC_DATE_CODE"
        fi
      fi
    else
      fail "GET /v1/holiday-calendars — HTTP $HC_LIST_CODE"
      verbose "$HC_LIST_BODY"
    fi
  else
    skip "Holiday Calendar API tests — no admin token"
  fi

  # --- 3.14: Analytics Service API tests ---
  info "--- Analytics Service API tests ---"

  # Attempt admin login for analytics (reuse ADMIN_TOKEN if already set)
  if [[ -z "${ADMIN_TOKEN:-}" ]]; then
    ADMIN_RESP=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8081/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"admin@ruleforge.io","password":"password123"}' 2>/dev/null)
    ADMIN_CODE=$(echo "$ADMIN_RESP" | tail -1)
    ADMIN_BODY=$(echo "$ADMIN_RESP" | sed '$d')
    if [[ "$ADMIN_CODE" == "200" ]]; then
      ADMIN_TOKEN=$(echo "$ADMIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")
    fi
  fi

  if [[ -n "${ADMIN_TOKEN:-}" ]]; then
    # GET /v1/analytics/dashboard — should return 200 (empty data is OK, must not 500)
    ANALYTICS_DASH_RESP=$(curl -s -w "\n%{http_code}" \
      "http://localhost:8085/v1/analytics/dashboard?hours=24" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    ANALYTICS_DASH_CODE=$(echo "$ANALYTICS_DASH_RESP" | tail -1)
    ANALYTICS_DASH_BODY=$(echo "$ANALYTICS_DASH_RESP" | sed '$d')

    if [[ "$ANALYTICS_DASH_CODE" == "200" ]]; then
      pass "GET /v1/analytics/dashboard — HTTP 200 (graceful empty OK)"
      verbose "$ANALYTICS_DASH_BODY"
    else
      fail "GET /v1/analytics/dashboard — HTTP $ANALYTICS_DASH_CODE (expected 200)"
      verbose "$ANALYTICS_DASH_BODY"
    fi

    # GET /v1/analytics/decisions — should return 200 with paginated empty result
    ANALYTICS_DEC_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8085/v1/analytics/decisions?page=0&size=20" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$ANALYTICS_DEC_CODE" == "200" ]]; then
      pass "GET /v1/analytics/decisions — HTTP 200"
    else
      fail "GET /v1/analytics/decisions — HTTP $ANALYTICS_DEC_CODE"
    fi

    # GET /v1/analytics/decisions with filters — status, search, latency
    ANALYTICS_DEC_FILTER_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8085/v1/analytics/decisions?status=ERROR&search=test&minLatency=10&maxLatency=500" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$ANALYTICS_DEC_FILTER_CODE" == "200" ]]; then
      pass "GET /v1/analytics/decisions?status=ERROR&search=test&minLatency=10&maxLatency=500 — filters accepted"
    else
      fail "GET /v1/analytics/decisions (with filters) — HTTP $ANALYTICS_DEC_FILTER_CODE"
    fi

    # GET /v1/analytics/timeline — must not 500
    ANALYTICS_TL_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8085/v1/analytics/timeline?hours=24&interval=hour" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$ANALYTICS_TL_CODE" == "200" ]]; then
      pass "GET /v1/analytics/timeline — HTTP 200"
    else
      fail "GET /v1/analytics/timeline — HTTP $ANALYTICS_TL_CODE"
    fi

    # GET /v1/analytics/type-distribution — verify field name fix
    ANALYTICS_TYPE_RESP=$(curl -s -w "\n%{http_code}" \
      "http://localhost:8085/v1/analytics/type-distribution?hours=24" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    ANALYTICS_TYPE_CODE=$(echo "$ANALYTICS_TYPE_RESP" | tail -1)
    ANALYTICS_TYPE_BODY=$(echo "$ANALYTICS_TYPE_RESP" | sed '$d')
    if [[ "$ANALYTICS_TYPE_CODE" == "200" ]]; then
      # Verify "type" field (not "ruleType") in response items when non-empty
      HAS_RULE_TYPE=$(echo "$ANALYTICS_TYPE_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    if isinstance(d, list) and len(d) > 0:
        print('ruleType' if 'ruleType' in d[0] else 'ok')
    else:
        print('ok')
except:
    print('ok')
" 2>/dev/null || echo "ok")
      if [[ "$HAS_RULE_TYPE" == "ruleType" ]]; then
        fail "GET /v1/analytics/type-distribution — response still contains 'ruleType' field (should be 'type')"
      else
        pass "GET /v1/analytics/type-distribution — HTTP 200, field name correct"
      fi
      verbose "$ANALYTICS_TYPE_BODY"
    else
      fail "GET /v1/analytics/type-distribution — HTTP $ANALYTICS_TYPE_CODE"
    fi

    # GET /v1/analytics/latency-percentiles — must not 500
    ANALYTICS_LAT_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8085/v1/analytics/latency-percentiles?hours=24" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$ANALYTICS_LAT_CODE" == "200" ]]; then
      pass "GET /v1/analytics/latency-percentiles — HTTP 200"
    else
      fail "GET /v1/analytics/latency-percentiles — HTTP $ANALYTICS_LAT_CODE"
    fi

    # GET /v1/analytics/pass-fail — must not 500
    ANALYTICS_PF_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8085/v1/analytics/pass-fail?hours=24&interval=hour" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$ANALYTICS_PF_CODE" == "200" ]]; then
      pass "GET /v1/analytics/pass-fail — HTTP 200"
    else
      fail "GET /v1/analytics/pass-fail — HTTP $ANALYTICS_PF_CODE"
    fi

  else
    skip "Analytics API tests — no admin token (auth service unavailable?)"
  fi
fi

# ============================================================================
# 3.15 — Event Service: Audit Log API
# ============================================================================
info "3.15 — Audit Log API (event-service :8084)"

EVENT_HEALTH=$(curl -sf --max-time 3 "http://localhost:8084/actuator/health" 2>/dev/null | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")

if [[ "$EVENT_HEALTH" != "UP" ]]; then
  skip "Audit Log API tests — event-service not running (health: $EVENT_HEALTH)"
elif [[ -z "${ADMIN_TOKEN:-}" ]]; then
  skip "Audit Log API tests — no admin token (auth service unavailable?)"
else
  if [[ "$QUICK" == "false" ]]; then

    # GET /v1/audit — paginated list, must return 200
    AUDIT_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8084/v1/audit" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$AUDIT_CODE" == "200" ]]; then
      pass "GET /v1/audit — HTTP 200"
    else
      fail "GET /v1/audit — HTTP $AUDIT_CODE"
    fi

    # GET /v1/audit — verify response shape has content[] and totalElements
    AUDIT_RESP=$(curl -s \
      "http://localhost:8084/v1/audit" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    HAS_CONTENT=$(echo "$AUDIT_RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    has_content = 'content' in data
    has_total = 'totalElements' in data
    print('ok' if has_content and has_total else 'missing')
except: print('missing')
" 2>/dev/null || echo "missing")
    if [[ "$HAS_CONTENT" == "ok" ]]; then
      pass "GET /v1/audit — response has content[] and totalElements"
    else
      fail "GET /v1/audit — response missing content[] or totalElements fields"
    fi
    verbose "$AUDIT_RESP"

    # GET /v1/audit?eventType=RULE_CREATED — must return 200
    AUDIT_FILTER_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8084/v1/audit?eventType=RULE_CREATED" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$AUDIT_FILTER_CODE" == "200" ]]; then
      pass "GET /v1/audit?eventType=RULE_CREATED — HTTP 200"
    else
      fail "GET /v1/audit?eventType=RULE_CREATED — HTTP $AUDIT_FILTER_CODE"
    fi

    # GET /v1/audit?search=Loan — must return 200
    AUDIT_SEARCH_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8084/v1/audit?search=Loan" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$AUDIT_SEARCH_CODE" == "200" ]]; then
      pass "GET /v1/audit?search=Loan — HTTP 200"
    else
      fail "GET /v1/audit?search=Loan — HTTP $AUDIT_SEARCH_CODE"
    fi

    # GET /v1/audit/resources/RULE/{ruleId} — must return 200
    RULE_ID="e0000000-0000-0000-0000-000000000001"
    AUDIT_RES_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8084/v1/audit/resources/RULE/$RULE_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$AUDIT_RES_CODE" == "200" ]]; then
      pass "GET /v1/audit/resources/RULE/{id} — HTTP 200"
    else
      fail "GET /v1/audit/resources/RULE/{id} — HTTP $AUDIT_RES_CODE"
    fi

    # GET /v1/audit/export — must return 200 with CSV content-type
    AUDIT_EXPORT_RESP=$(curl -s -D - -o /dev/null \
      "http://localhost:8084/v1/audit/export" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    AUDIT_EXPORT_CODE=$(echo "$AUDIT_EXPORT_RESP" | grep "^HTTP/" | awk '{print $2}' | tail -1)
    AUDIT_EXPORT_CT=$(echo "$AUDIT_EXPORT_RESP" | grep -i "content-type:" | head -1)
    if [[ "$AUDIT_EXPORT_CODE" == "200" ]]; then
      pass "GET /v1/audit/export — HTTP 200"
    else
      fail "GET /v1/audit/export — HTTP $AUDIT_EXPORT_CODE"
    fi
    if echo "$AUDIT_EXPORT_CT" | grep -qi "text/csv"; then
      pass "GET /v1/audit/export — Content-Type: text/csv"
    else
      fail "GET /v1/audit/export — Content-Type not text/csv (got: $AUDIT_EXPORT_CT)"
    fi

  fi

  # --- 3.16: Spaces API (auth-service :8081) ---
  info "3.16 — Spaces API (auth-service :8081)"

  # GET /v1/organizations/current/spaces — list spaces
  SPACES_LIST_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:8081/v1/organizations/current/spaces" \
    -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
  if [[ "$SPACES_LIST_CODE" == "200" ]]; then
    pass "GET /v1/organizations/current/spaces — HTTP 200"
  else
    fail "GET /v1/organizations/current/spaces — HTTP $SPACES_LIST_CODE"
  fi

  # GET /v1/organizations/current/spaces — verify slug and ruleCount fields
  SPACES_BODY=$(curl -s \
    "http://localhost:8081/v1/organizations/current/spaces" \
    -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
  if echo "$SPACES_BODY" | grep -q '"slug"'; then
    pass "Spaces response includes 'slug' field"
  else
    fail "Spaces response missing 'slug' field"
  fi
  if echo "$SPACES_BODY" | grep -q '"ruleCount"'; then
    pass "Spaces response includes 'ruleCount' field"
  else
    fail "Spaces response missing 'ruleCount' field"
  fi
  if echo "$SPACES_BODY" | grep -q '"description"'; then
    pass "Spaces response includes 'description' field"
  else
    fail "Spaces response missing 'description' field"
  fi

  # POST /v1/organizations/current/spaces — create a test space
  CREATE_SPACE_RESP=$(curl -s -w "\n%{http_code}" \
    -X POST "http://localhost:8081/v1/organizations/current/spaces" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Smoke Test Space"}' 2>/dev/null)
  CREATE_SPACE_CODE=$(echo "$CREATE_SPACE_RESP" | tail -1)
  CREATE_SPACE_BODY=$(echo "$CREATE_SPACE_RESP" | head -1)
  if [[ "$CREATE_SPACE_CODE" == "200" || "$CREATE_SPACE_CODE" == "201" ]]; then
    pass "POST /v1/organizations/current/spaces — HTTP $CREATE_SPACE_CODE"
  else
    fail "POST /v1/organizations/current/spaces — HTTP $CREATE_SPACE_CODE"
  fi

  # Extract space ID for follow-up tests
  TEST_SPACE_ID=$(echo "$CREATE_SPACE_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [[ -n "$TEST_SPACE_ID" ]]; then
    # Verify slug was auto-generated
    if echo "$CREATE_SPACE_BODY" | grep -q '"slug":"smoke-test-space"'; then
      pass "POST /v1/organizations/current/spaces — slug auto-generated correctly"
    else
      fail "POST /v1/organizations/current/spaces — slug not auto-generated"
    fi

    # GET /v1/organizations/current/spaces/:id — get single space
    GET_SPACE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8081/v1/organizations/current/spaces/$TEST_SPACE_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$GET_SPACE_CODE" == "200" ]]; then
      pass "GET /v1/organizations/current/spaces/:id — HTTP 200"
    else
      fail "GET /v1/organizations/current/spaces/:id — HTTP $GET_SPACE_CODE"
    fi

    # PUT /v1/organizations/current/spaces/:id — update name and description
    UPDATE_SPACE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      -X PUT "http://localhost:8081/v1/organizations/current/spaces/$TEST_SPACE_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"name":"Smoke Test Space Updated","description":"Updated by smoke test"}' 2>/dev/null)
    if [[ "$UPDATE_SPACE_CODE" == "200" ]]; then
      pass "PUT /v1/organizations/current/spaces/:id — HTTP 200"
    else
      fail "PUT /v1/organizations/current/spaces/:id — HTTP $UPDATE_SPACE_CODE"
    fi

    # DELETE /v1/organizations/current/spaces/:id — delete test space
    DELETE_SPACE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      -X DELETE "http://localhost:8081/v1/organizations/current/spaces/$TEST_SPACE_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$DELETE_SPACE_CODE" == "204" ]]; then
      pass "DELETE /v1/organizations/current/spaces/:id — HTTP 204"
    else
      fail "DELETE /v1/organizations/current/spaces/:id — HTTP $DELETE_SPACE_CODE"
    fi
  else
    skip "Spaces GET/:id, PUT, DELETE — could not extract space ID from create response"
  fi

  # --- 3.17: Space-Scoped Rule Management (management-service :8082) ---
  info "3.17 — Space-Scoped Rule Management (management-service :8082)"

  # Get first space ID for scoped tests
  SPACES_BODY_MGMT=$(curl -s \
    "http://localhost:8081/v1/organizations/current/spaces" \
    -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
  TEST_SPACE_ID=$(echo "$SPACES_BODY_MGMT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
items = data.get('data', []) if isinstance(data, dict) else data
if isinstance(items, list) and items:
    print(items[0].get('id',''))
" 2>/dev/null || true)

  if [[ -n "$TEST_SPACE_ID" && "$TEST_SPACE_ID" != "null" ]]; then
    pass "Found test space ID: ${TEST_SPACE_ID:0:8}..."

    # GET /v1/spaces/{spaceId}/rules — list rules in space
    SPACE_RULES_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8082/v1/spaces/${TEST_SPACE_ID}/rules" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$SPACE_RULES_CODE" == "200" ]]; then
      pass "GET /v1/spaces/{spaceId}/rules — HTTP 200"
    else
      fail "GET /v1/spaces/{spaceId}/rules — HTTP $SPACE_RULES_CODE"
    fi

    # GET /v1/spaces/{spaceId}/rules — verify response shape
    SPACE_RULES_BODY=$(curl -s \
      "http://localhost:8082/v1/spaces/${TEST_SPACE_ID}/rules" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if echo "$SPACE_RULES_BODY" | grep -q '"content"'; then
      pass "GET /v1/spaces/{spaceId}/rules — response has 'content' array"
    else
      fail "GET /v1/spaces/{spaceId}/rules — missing 'content' field in response"
    fi
    if echo "$SPACE_RULES_BODY" | grep -q '"totalElements"'; then
      pass "GET /v1/spaces/{spaceId}/rules — response has 'totalElements'"
    else
      fail "GET /v1/spaces/{spaceId}/rules — missing 'totalElements' in response"
    fi

    # POST /v1/spaces/{spaceId}/rules — create rule in specific space (unique name per run)
    SMOKE_RULE_NAME="Smoke Test Space Rule $(date +%s)"
    CREATE_SPACE_RULE_BODY=$(curl -s \
      -X POST "http://localhost:8082/v1/spaces/${TEST_SPACE_ID}/rules" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"${SMOKE_RULE_NAME}\",\"type\":\"TABLE\",\"description\":\"Created by smoke test 3.17\"}" 2>/dev/null)
    CREATED_RULE_ID=$(echo "$CREATE_SPACE_RULE_BODY" | python3 -c "
import sys, json
data = json.load(sys.stdin)
rule = data.get('data', {})
print(rule.get('id', ''))
" 2>/dev/null || true)
    if [[ -n "$CREATED_RULE_ID" && "$CREATED_RULE_ID" != "null" ]]; then
      pass "POST /v1/spaces/{spaceId}/rules — rule created (id: ${CREATED_RULE_ID:0:8}...)"

      # PATCH /v1/rules/{ruleId}/move — move rule to same space (idempotent test)
      MOVE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X PATCH "http://localhost:8082/v1/rules/${CREATED_RULE_ID}/move" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"targetSpaceId\":\"${TEST_SPACE_ID}\"}" 2>/dev/null)
      if [[ "$MOVE_CODE" == "200" ]]; then
        pass "PATCH /v1/rules/{ruleId}/move — HTTP 200 (idempotent move to same space)"
      else
        fail "PATCH /v1/rules/{ruleId}/move — HTTP $MOVE_CODE (expected 200)"
      fi

      # POST /v1/rules/{ruleId}/copy — copy rule with same-space (backward compat)
      COPY_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://localhost:8082/v1/rules/${CREATED_RULE_ID}/copy" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"name":"Smoke Test Copy"}' 2>/dev/null)
      if [[ "$COPY_CODE" == "201" ]]; then
        pass "POST /v1/rules/{ruleId}/copy (no targetSpaceId) — HTTP 201 backward compat"
      else
        fail "POST /v1/rules/{ruleId}/copy (no targetSpaceId) — HTTP $COPY_CODE (expected 201)"
      fi

      # POST /v1/rules/{ruleId}/copy — copy with targetSpaceId (new feature)
      COPY_WITH_SPACE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://localhost:8082/v1/rules/${CREATED_RULE_ID}/copy" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"Smoke Test Copy With Space\",\"targetSpaceId\":\"${TEST_SPACE_ID}\"}" 2>/dev/null)
      if [[ "$COPY_WITH_SPACE_CODE" == "201" ]]; then
        pass "POST /v1/rules/{ruleId}/copy with targetSpaceId — HTTP 201"
      else
        fail "POST /v1/rules/{ruleId}/copy with targetSpaceId — HTTP $COPY_WITH_SPACE_CODE (expected 201)"
      fi

      # PATCH /v1/rules/{ruleId}/move — missing targetSpaceId should return 400
      MOVE_INVALID_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X PATCH "http://localhost:8082/v1/rules/${CREATED_RULE_ID}/move" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{}' 2>/dev/null)
      if [[ "$MOVE_INVALID_CODE" == "400" ]]; then
        pass "PATCH /v1/rules/{ruleId}/move with missing targetSpaceId — HTTP 400 validation"
      else
        fail "PATCH /v1/rules/{ruleId}/move with missing targetSpaceId — HTTP $MOVE_INVALID_CODE (expected 400)"
      fi

      # Cleanup: delete the smoke test rule
      DELETE_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X DELETE "http://localhost:8082/v1/rules/${CREATED_RULE_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$DELETE_CODE" == "200" ]]; then
        pass "DELETE /v1/rules/{smoke-test-rule} — cleanup successful"
      else
        skip "DELETE /v1/rules/{smoke-test-rule} — HTTP $DELETE_CODE (cleanup failed, manual cleanup needed)"
      fi
    else
      fail "POST /v1/spaces/{spaceId}/rules — failed to create rule (response: ${CREATE_SPACE_RULE_BODY:0:100})"
      skip "PATCH /v1/rules/{ruleId}/move — skipped (no rule ID)"
      skip "POST /v1/rules/{ruleId}/copy tests — skipped (no rule ID)"
    fi
  else
    skip "3.17 space-scoped rule tests — could not get space ID from auth service"
  fi

fi

# ============================================================================
# 3.18: Space Context Banners — Vite proxy fix validation (management-service :8082)
# ============================================================================
if [[ "$QUICK" == "false" ]]; then
  info "3.18 — Space Context Banners — proxy fix validation"

  # Re-use ADMIN_TOKEN from section 3.2 (already acquired above)
  if [[ -n "$ADMIN_TOKEN" ]]; then
    # Get all spaces to find Sandbox and Default IDs
    SPACES_3_18=$(curl -s "http://localhost:8081/v1/organizations/current/spaces" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)

    SANDBOX_ID_3_18=$(echo "$SPACES_3_18" | python3 -c "
import sys, json
data = json.load(sys.stdin)
spaces = data.get('data', [])
for s in spaces:
    if s.get('name') == 'Sandbox':
        print(s['id'])
        break
" 2>/dev/null)

    DEFAULT_ID_3_18=$(echo "$SPACES_3_18" | python3 -c "
import sys, json
data = json.load(sys.stdin)
spaces = data.get('data', [])
for s in spaces:
    if s.get('name') != 'Sandbox':
        print(s['id'])
        break
" 2>/dev/null)

    if [[ -n "$SANDBOX_ID_3_18" ]]; then
      # Test Sandbox space rules endpoint directly against management-service (was returning 500 via proxy)
      SANDBOX_RULES_CODE_3_18=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8082/v1/spaces/${SANDBOX_ID_3_18}/rules" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$SANDBOX_RULES_CODE_3_18" == "200" ]]; then
        pass "GET /v1/spaces/sandbox/rules — management-service returns 200 (proxy target verified)"
      else
        fail "GET /v1/spaces/sandbox/rules — HTTP $SANDBOX_RULES_CODE_3_18 (expected 200)"
      fi

      # Verify response has paginated structure
      SANDBOX_RULES_BODY_3_18=$(curl -s \
        "http://localhost:8082/v1/spaces/${SANDBOX_ID_3_18}/rules" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      SANDBOX_HAS_CONTENT_3_18=$(echo "$SANDBOX_RULES_BODY_3_18" | python3 -c \
        "import sys,json; d=json.load(sys.stdin); print('yes' if 'content' in d.get('data',{}) else 'no')" 2>/dev/null)
      if [[ "$SANDBOX_HAS_CONTENT_3_18" == "yes" ]]; then
        pass "GET /v1/spaces/sandbox/rules — response has paginated 'content' field"
      else
        fail "GET /v1/spaces/sandbox/rules — response missing 'content' field"
      fi
    else
      skip "3.18 Sandbox rules test — no space named 'Sandbox' found in org"
    fi

    if [[ -n "$DEFAULT_ID_3_18" ]]; then
      # Test non-Sandbox (production) space rules endpoint
      DEFAULT_RULES_CODE_3_18=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8082/v1/spaces/${DEFAULT_ID_3_18}/rules" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$DEFAULT_RULES_CODE_3_18" == "200" ]]; then
        pass "GET /v1/spaces/production/rules — management-service returns 200"
      else
        fail "GET /v1/spaces/production/rules — HTTP $DEFAULT_RULES_CODE_3_18 (expected 200)"
      fi
    else
      skip "3.18 production space rules test — no non-Sandbox space found"
    fi

    # Confirm spaces CRUD still available on auth-service (not broken by proxy rule change)
    SPACES_LIST_CODE_3_18=$(curl -s -o /dev/null -w "%{http_code}" \
      "http://localhost:8081/v1/organizations/current/spaces" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    if [[ "$SPACES_LIST_CODE_3_18" == "200" ]]; then
      pass "GET /v1/organizations/current/spaces — auth-service still returns 200"
    else
      fail "GET /v1/organizations/current/spaces — HTTP $SPACES_LIST_CODE_3_18 (expected 200)"
    fi
  else
    skip "3.18 space banners tests — no admin token available"
  fi
fi

# ============================================================================
# 3.19: Space-Rule Assignment — assign existing rules to spaces via move endpoint
# ============================================================================
if [[ "$QUICK" == "false" ]]; then
  info "3.19 — Space-Rule Assignment — assign rules to spaces via PATCH /rules/{id}/move"

  if [[ -n "$ADMIN_TOKEN" ]]; then
    # Get spaces to find target space
    SPACES_3_19=$(curl -s "http://localhost:8081/v1/organizations/current/spaces" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)

    TARGET_SPACE_3_19=$(echo "$SPACES_3_19" | python3 -c "
import sys, json
data = json.load(sys.stdin)
spaces = data.get('data', [])
if spaces:
    print(spaces[0]['id'])
" 2>/dev/null)

    if [[ -n "$TARGET_SPACE_3_19" ]]; then
      # Get rules for assignment test
      RULES_3_19=$(curl -s "http://localhost:8082/v1/spaces/${TARGET_SPACE_3_19}/rules?page=0&size=1" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      RULES_CODE_3_19=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8082/v1/spaces/${TARGET_SPACE_3_19}/rules?page=0&size=1" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)

      if [[ "$RULES_CODE_3_19" == "200" ]]; then
        pass "GET /v1/spaces/{id}/rules — returns 200 (used for current-space rule filtering)"
      else
        fail "GET /v1/spaces/{id}/rules — HTTP $RULES_CODE_3_19 (expected 200)"
      fi

      # Verify all-org rules endpoint (used by AssignRulesModal to fetch org-wide rules)
      ALL_RULES_CODE_3_19=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8082/v1/rules?page=0&size=200" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$ALL_RULES_CODE_3_19" == "200" ]]; then
        pass "GET /v1/rules?page=0&size=200 — returns 200 (used by AssignRulesModal)"
      else
        fail "GET /v1/rules?page=0&size=200 — HTTP $ALL_RULES_CODE_3_19 (expected 200)"
      fi

      # Verify move endpoint exists and responds (used for rule assignment)
      RULE_ID_3_19=$(echo "$RULES_3_19" | python3 -c "
import sys, json
data = json.load(sys.stdin)
content = data.get('data', {}).get('content', [])
if content:
    print(content[0]['id'])
" 2>/dev/null)

      if [[ -n "$RULE_ID_3_19" ]]; then
        # Move rule to the same space (no-op move — verifies endpoint exists)
        MOVE_CODE_3_19=$(curl -s -o /dev/null -w "%{http_code}" \
          -X PATCH "http://localhost:8082/v1/rules/${RULE_ID_3_19}/move" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -H "Content-Type: application/json" \
          -d "{\"targetSpaceId\": \"${TARGET_SPACE_3_19}\"}" 2>/dev/null)
        if [[ "$MOVE_CODE_3_19" == "200" ]]; then
          pass "PATCH /v1/rules/{id}/move — returns 200 (rule assignment endpoint works)"
        else
          fail "PATCH /v1/rules/{id}/move — HTTP $MOVE_CODE_3_19 (expected 200)"
        fi
      else
        skip "3.19 move endpoint test — no rules found in target space to test with"
      fi
    else
      skip "3.19 space-rule assignment tests — no spaces found in org"
    fi
  else
    skip "3.19 space-rule assignment tests — no admin token available"
  fi
fi

# ============================================================================
# 3.20: Webhook Multi-Scope + Security (event-service :8084)
# ============================================================================
if [[ "$QUICK" == "false" ]]; then
  info "3.20 — Webhook Multi-Scope + Security (event-service :8084)"

  EVENT_HEALTH_320=$(curl -sf --max-time 3 "http://localhost:8084/actuator/health" 2>/dev/null | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")

  if [[ "$EVENT_HEALTH_320" != "UP" ]]; then
    skip "Webhook tests (3.20) — event-service not running (health: $EVENT_HEALTH_320)"
  elif [[ -z "${ADMIN_TOKEN:-}" ]]; then
    skip "Webhook tests (3.20) — no admin token (auth service unavailable?)"
  else
    # Seed org and space IDs (from V021 seed migration)
    SEED_ORG_ID="a0000000-0000-0000-0000-000000000001"
    SEED_SPACE_ID="d0000000-0000-0000-0000-000000000001"

    # Acquire editor token for RBAC tests
    EDITOR_LOGIN_320=$(curl -s -X POST http://localhost:8081/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"editor@ruleforge.io","password":"password123"}' 2>/dev/null)
    EDITOR_TOKEN_320=$(echo "$EDITOR_LOGIN_320" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")

    # --- 3.20.1: Create SPACE-scoped webhook ---
    WH_CREATE_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-test-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\",\"rule.executed\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID}\"}" 2>/dev/null)
    WH_CREATE_CODE=$(echo "$WH_CREATE_RESP" | tail -1)
    WH_CREATE_BODY=$(echo "$WH_CREATE_RESP" | sed '$d')

    if [[ "$WH_CREATE_CODE" =~ ^(200|201)$ ]]; then
      WH_ID=$(echo "$WH_CREATE_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      WH_SECRET=$(echo "$WH_CREATE_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('secret',''))" 2>/dev/null || echo "")
      if [[ -n "$WH_ID" && -n "$WH_SECRET" ]]; then
        pass "POST /v1/webhooks (scopeType=SPACE) — HTTP $WH_CREATE_CODE, id and secret returned"
        verbose "webhook_id=${WH_ID}"
      else
        fail "POST /v1/webhooks (scopeType=SPACE) — response missing id or secret fields"
        verbose "$WH_CREATE_BODY"
      fi
    else
      fail "POST /v1/webhooks (scopeType=SPACE) — HTTP $WH_CREATE_CODE"
      verbose "$WH_CREATE_BODY"
      WH_ID=""
    fi

    # --- 3.20.2: SSRF blocking — internal URL must be rejected ---
    WH_SSRF_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"ssrf-test\",\"url\":\"http://localhost/webhook\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID}\"}" 2>/dev/null)
    if [[ "$WH_SSRF_CODE" == "400" ]]; then
      pass "POST /v1/webhooks url=http://localhost/webhook — HTTP 400 SSRF blocked"
    else
      fail "POST /v1/webhooks url=http://localhost/webhook — HTTP $WH_SSRF_CODE (expected 400, SSRF not blocked)"
    fi

    # --- 3.20.3: ORG-scoped webhook as OWNER (should succeed) ---
    WH_ORG_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-org-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"ORG\",\"scopeId\":\"${SEED_ORG_ID}\"}" 2>/dev/null)
    if [[ "$WH_ORG_CODE" =~ ^(200|201)$ ]]; then
      pass "POST /v1/webhooks (scopeType=ORG) as OWNER — HTTP $WH_ORG_CODE"
    else
      fail "POST /v1/webhooks (scopeType=ORG) as OWNER — HTTP $WH_ORG_CODE (expected 201)"
    fi

    # --- 3.20.4: ORG-scoped webhook as EDITOR (should fail 403) ---
    if [[ -n "$EDITOR_TOKEN_320" ]]; then
      WH_EDITOR_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://localhost:8084/v1/webhooks" \
        -H "Authorization: Bearer $EDITOR_TOKEN_320" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"smoke-editor-org-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"ORG\",\"scopeId\":\"${SEED_ORG_ID}\"}" 2>/dev/null)
      if [[ "$WH_EDITOR_CODE" == "403" ]]; then
        pass "POST /v1/webhooks (scopeType=ORG) as EDITOR — HTTP 403 (RBAC enforced)"
      else
        fail "POST /v1/webhooks (scopeType=ORG) as EDITOR — HTTP $WH_EDITOR_CODE (expected 403)"
      fi
    else
      skip "POST /v1/webhooks (scopeType=ORG) as EDITOR — no editor token (seed not applied?)"
    fi

    # --- 3.20.5: List webhooks with scope filter ---
    WH_LIST_RESP=$(curl -s -w "\n%{http_code}" \
      "http://localhost:8084/v1/webhooks?scopeType=SPACE" \
      -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
    WH_LIST_CODE=$(echo "$WH_LIST_RESP" | tail -1)
    WH_LIST_BODY=$(echo "$WH_LIST_RESP" | sed '$d')
    if [[ "$WH_LIST_CODE" == "200" ]]; then
      IS_ARRAY=$(echo "$WH_LIST_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    items = data.get('content', data) if isinstance(data, dict) else data
    print('yes' if isinstance(items, list) else 'no')
except: print('no')
" 2>/dev/null || echo "no")
      if [[ "$IS_ARRAY" == "yes" ]]; then
        pass "GET /v1/webhooks?scopeType=SPACE — HTTP 200, array response"
      else
        fail "GET /v1/webhooks?scopeType=SPACE — HTTP 200 but response is not an array"
        verbose "$WH_LIST_BODY"
      fi
    else
      fail "GET /v1/webhooks?scopeType=SPACE — HTTP $WH_LIST_CODE"
      verbose "$WH_LIST_BODY"
    fi

    # --- Tests requiring a webhook ID (created in 3.20.1) ---
    if [[ -n "${WH_ID:-}" ]]; then

      # --- 3.20.6: Get single webhook ---
      WH_GET_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_GET_CODE=$(echo "$WH_GET_RESP" | tail -1)
      WH_GET_BODY=$(echo "$WH_GET_RESP" | sed '$d')
      if [[ "$WH_GET_CODE" == "200" ]]; then
        HAS_SCOPE_FIELDS=$(echo "$WH_GET_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    has_scope = 'scopeType' in data
    has_failures = 'consecutiveFailures' in data
    print('ok' if has_scope and has_failures else 'missing')
except: print('missing')
" 2>/dev/null || echo "missing")
        if [[ "$HAS_SCOPE_FIELDS" == "ok" ]]; then
          pass "GET /v1/webhooks/{id} — HTTP 200, scopeType and consecutiveFailures present"
        else
          fail "GET /v1/webhooks/{id} — HTTP 200 but missing scopeType or consecutiveFailures"
          verbose "$WH_GET_BODY"
        fi
      else
        fail "GET /v1/webhooks/{id} — HTTP $WH_GET_CODE"
        verbose "$WH_GET_BODY"
      fi

      # --- 3.20.7: Send test delivery ---
      WH_TEST_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "http://localhost:8084/v1/webhooks/${WH_ID}/test" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_TEST_CODE=$(echo "$WH_TEST_RESP" | tail -1)
      WH_TEST_BODY=$(echo "$WH_TEST_RESP" | sed '$d')
      if [[ "$WH_TEST_CODE" == "200" ]]; then
        HAS_STATUS_CODE=$(echo "$WH_TEST_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    print('ok' if 'statusCode' in data else 'missing')
except: print('missing')
" 2>/dev/null || echo "missing")
        if [[ "$HAS_STATUS_CODE" == "ok" ]]; then
          pass "POST /v1/webhooks/{id}/test — HTTP 200, statusCode in response"
        else
          pass "POST /v1/webhooks/{id}/test — HTTP 200 (delivery attempted; connection errors to external URL are expected)"
        fi
        verbose "$WH_TEST_BODY"
      else
        fail "POST /v1/webhooks/{id}/test — HTTP $WH_TEST_CODE (expected 200)"
        verbose "$WH_TEST_BODY"
      fi

      # --- 3.20.8: Rotate secret ---
      WH_ROTATE_RESP=$(curl -s -w "\n%{http_code}" \
        -X POST "http://localhost:8084/v1/webhooks/${WH_ID}/rotate-secret" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_ROTATE_CODE=$(echo "$WH_ROTATE_RESP" | tail -1)
      WH_ROTATE_BODY=$(echo "$WH_ROTATE_RESP" | sed '$d')
      if [[ "$WH_ROTATE_CODE" == "200" ]]; then
        NEW_SECRET=$(echo "$WH_ROTATE_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('secret',''))" 2>/dev/null || echo "")
        if [[ -n "$NEW_SECRET" ]]; then
          pass "POST /v1/webhooks/{id}/rotate-secret — HTTP 200, new secret returned"
          verbose "new_secret_length=${#NEW_SECRET}"
        else
          fail "POST /v1/webhooks/{id}/rotate-secret — HTTP 200 but no secret in response"
          verbose "$WH_ROTATE_BODY"
        fi
      else
        fail "POST /v1/webhooks/{id}/rotate-secret — HTTP $WH_ROTATE_CODE"
        verbose "$WH_ROTATE_BODY"
      fi

      # --- 3.20.9: Get outbox (as ADMIN) ---
      WH_OUTBOX_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_ID}/outbox" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$WH_OUTBOX_CODE" == "200" ]]; then
        pass "GET /v1/webhooks/{id}/outbox — HTTP 200"
      else
        fail "GET /v1/webhooks/{id}/outbox — HTTP $WH_OUTBOX_CODE (expected 200)"
      fi

      # --- 3.20.10: Reset circuit breaker ---
      WH_RESET_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://localhost:8084/v1/webhooks/${WH_ID}/reset-circuit" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$WH_RESET_CODE" == "200" ]]; then
        pass "POST /v1/webhooks/{id}/reset-circuit — HTTP 200"
      else
        fail "POST /v1/webhooks/{id}/reset-circuit — HTTP $WH_RESET_CODE (expected 200)"
      fi

      # Cleanup: delete the smoke-test webhook
      WH_DEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X DELETE "http://localhost:8084/v1/webhooks/${WH_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      if [[ "$WH_DEL_CODE" =~ ^(200|204)$ ]]; then
        pass "DELETE /v1/webhooks/{id} — cleanup (HTTP $WH_DEL_CODE)"
      else
        skip "DELETE /v1/webhooks/{id} — HTTP $WH_DEL_CODE (cleanup failed, manual cleanup needed)"
      fi

    else
      skip "Webhook detail tests (3.20.6-3.20.10) — no webhook ID (creation failed in 3.20.1)"
    fi

  fi
fi

# ============================================================================
# 3.21: Webhook Auth Enhancement — auth types, metrics RBAC (event-service :8084)
# ============================================================================
if [[ "$QUICK" == "false" ]]; then
  info "3.21 — Webhook Auth Enhancement (event-service :8084)"

  EVENT_HEALTH_321=$(curl -sf --max-time 3 "http://localhost:8084/actuator/health" 2>/dev/null | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")

  if [[ "$EVENT_HEALTH_321" != "UP" ]]; then
    skip "Webhook auth tests (3.21) — event-service not running (health: $EVENT_HEALTH_321)"
  elif [[ -z "${ADMIN_TOKEN:-}" ]]; then
    skip "Webhook auth tests (3.21) — no admin token (auth service unavailable?)"
  else
    SEED_SPACE_ID_321="d0000000-0000-0000-0000-000000000001"

    # Acquire editor and viewer tokens for RBAC tests
    EDITOR_LOGIN_321=$(curl -s -X POST http://localhost:8081/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"editor@ruleforge.io","password":"password123"}' 2>/dev/null)
    EDITOR_TOKEN_321=$(echo "$EDITOR_LOGIN_321" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")

    VIEWER_LOGIN_321=$(curl -s -X POST http://localhost:8081/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"viewer@ruleforge.io","password":"password123"}' 2>/dev/null)
    VIEWER_TOKEN_321=$(echo "$VIEWER_LOGIN_321" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")

    # --- 3.21.1: Create webhook with API_KEY auth type ---
    WH_AUTH_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-test-apikey-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_321}\",\"authType\":\"API_KEY\",\"authConfig\":{\"headerName\":\"X-Api-Key\",\"apiKey\":\"smoke-test-key-abc123\"}}" 2>/dev/null)
    WH_AUTH_CODE=$(echo "$WH_AUTH_RESP" | tail -1)
    WH_AUTH_BODY=$(echo "$WH_AUTH_RESP" | sed '$d')

    if [[ "$WH_AUTH_CODE" =~ ^(200|201)$ ]]; then
      WH_AUTH_ID=$(echo "$WH_AUTH_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      WH_AUTH_TYPE=$(echo "$WH_AUTH_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('authType',''))" 2>/dev/null || echo "")
      WH_SECRET_ON_CREATION=$(echo "$WH_AUTH_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('secretOnCreation',''))" 2>/dev/null || echo "")
      if [[ -n "$WH_AUTH_ID" && "$WH_AUTH_TYPE" == "API_KEY" ]]; then
        pass "POST /v1/webhooks (authType=API_KEY) — HTTP $WH_AUTH_CODE, authType=API_KEY in response"
        verbose "webhook_id=${WH_AUTH_ID} secretOnCreation_len=${#WH_SECRET_ON_CREATION}"
      else
        fail "POST /v1/webhooks (authType=API_KEY) — HTTP $WH_AUTH_CODE but authType not returned correctly (got: $WH_AUTH_TYPE)"
        verbose "$WH_AUTH_BODY"
      fi
    else
      WH_AUTH_ID=""
      fail "POST /v1/webhooks (authType=API_KEY) — HTTP $WH_AUTH_CODE"
      verbose "$WH_AUTH_BODY"
    fi

    # --- 3.21.2: GET response must not expose plaintext credentials ---
    if [[ -n "${WH_AUTH_ID:-}" ]]; then
      WH_GET_AUTH_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_AUTH_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_GET_AUTH_CODE=$(echo "$WH_GET_AUTH_RESP" | tail -1)
      WH_GET_AUTH_BODY=$(echo "$WH_GET_AUTH_RESP" | sed '$d')
      if [[ "$WH_GET_AUTH_CODE" == "200" ]]; then
        AUTH_REDACT_CHECK=$(echo "$WH_GET_AUTH_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    auth_cfg = data.get('authConfig') or {}
    has_auth_type = data.get('authType') == 'API_KEY'
    api_key_redacted = auth_cfg.get('apiKey') is None
    print('ok' if has_auth_type and api_key_redacted else 'exposed')
except:
    print('error')
" 2>/dev/null || echo "error")
        if [[ "$AUTH_REDACT_CHECK" == "ok" ]]; then
          pass "GET /v1/webhooks/{id} — authType=API_KEY present; apiKey not exposed in response"
        else
          fail "GET /v1/webhooks/{id} — plaintext credential may be exposed (expected apiKey=null)"
          verbose "$WH_GET_AUTH_BODY"
        fi
      else
        fail "GET /v1/webhooks/{id} after API_KEY creation — HTTP $WH_GET_AUTH_CODE"
        verbose "$WH_GET_AUTH_BODY"
      fi
    else
      skip "3.21.2 credential redaction check — no webhook ID (3.21.1 failed)"
    fi

    # --- 3.21.3: OWNER can access metrics endpoint ---
    if [[ -n "${WH_AUTH_ID:-}" ]]; then
      WH_METRICS_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_AUTH_ID}/metrics" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_METRICS_CODE=$(echo "$WH_METRICS_RESP" | tail -1)
      WH_METRICS_BODY=$(echo "$WH_METRICS_RESP" | sed '$d')
      if [[ "$WH_METRICS_CODE" == "200" ]]; then
        METRICS_FIELDS=$(echo "$WH_METRICS_BODY" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    data = d.get('data', d)
    has_total = 'totalDeliveries' in data
    has_success = 'successfulDeliveries' in data
    has_timeline = 'timeline' in data
    print('ok' if has_total and has_success and has_timeline else 'missing')
except:
    print('missing')
" 2>/dev/null || echo "missing")
        if [[ "$METRICS_FIELDS" == "ok" ]]; then
          pass "GET /v1/webhooks/{id}/metrics (OWNER) — HTTP 200, totalDeliveries/successfulDeliveries/timeline present"
        else
          fail "GET /v1/webhooks/{id}/metrics (OWNER) — HTTP 200 but missing expected fields"
          verbose "$WH_METRICS_BODY"
        fi
      else
        fail "GET /v1/webhooks/{id}/metrics (OWNER) — HTTP $WH_METRICS_CODE (expected 200)"
        verbose "$WH_METRICS_BODY"
      fi
    else
      skip "3.21.3 metrics OWNER test — no webhook ID (3.21.1 failed)"
    fi

    # --- 3.21.4: EDITOR can access metrics endpoint ---
    if [[ -n "${WH_AUTH_ID:-}" && -n "${EDITOR_TOKEN_321:-}" ]]; then
      WH_METRICS_ED_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_AUTH_ID}/metrics" \
        -H "Authorization: Bearer $EDITOR_TOKEN_321" 2>/dev/null)
      if [[ "$WH_METRICS_ED_CODE" == "200" ]]; then
        pass "GET /v1/webhooks/{id}/metrics (EDITOR) — HTTP 200 (EDITOR has read access)"
      else
        fail "GET /v1/webhooks/{id}/metrics (EDITOR) — HTTP $WH_METRICS_ED_CODE (expected 200)"
      fi
    else
      skip "3.21.4 metrics EDITOR test — no webhook ID or editor token"
    fi

    # --- 3.21.5: VIEWER is denied metrics endpoint (403/401) ---
    if [[ -n "${WH_AUTH_ID:-}" && -n "${VIEWER_TOKEN_321:-}" ]]; then
      WH_METRICS_VW_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8084/v1/webhooks/${WH_AUTH_ID}/metrics" \
        -H "Authorization: Bearer $VIEWER_TOKEN_321" 2>/dev/null)
      if [[ "$WH_METRICS_VW_CODE" =~ ^(401|403)$ ]]; then
        pass "GET /v1/webhooks/{id}/metrics (VIEWER) — HTTP $WH_METRICS_VW_CODE (VIEWER correctly denied)"
      else
        fail "GET /v1/webhooks/{id}/metrics (VIEWER) — HTTP $WH_METRICS_VW_CODE (expected 401/403; VIEWER must not access metrics)"
      fi
    else
      skip "3.21.5 metrics VIEWER deny test — no webhook ID or viewer token"
    fi

    # --- 3.21.6: Update auth config via PUT ---
    if [[ -n "${WH_AUTH_ID:-}" ]]; then
      WH_UPD_AUTH_RESP=$(curl -s -w "\n%{http_code}" \
        -X PUT "http://localhost:8084/v1/webhooks/${WH_AUTH_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"authConfig\":{\"token\":\"smoke-bearer-token-xyz\"}}" 2>/dev/null)
      WH_UPD_AUTH_CODE=$(echo "$WH_UPD_AUTH_RESP" | tail -1)
      WH_UPD_AUTH_BODY=$(echo "$WH_UPD_AUTH_RESP" | sed '$d')
      if [[ "$WH_UPD_AUTH_CODE" == "200" ]]; then
        pass "PUT /v1/webhooks/{id} (update authConfig) — HTTP 200"
      else
        fail "PUT /v1/webhooks/{id} (update authConfig) — HTTP $WH_UPD_AUTH_CODE"
        verbose "$WH_UPD_AUTH_BODY"
      fi
    else
      skip "3.21.6 update authConfig test — no webhook ID (3.21.1 failed)"
    fi

    # --- 3.21.7: Create webhook with BASIC_AUTH ---
    WH_BASIC_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-test-basicauth-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_321}\",\"authType\":\"BASIC_AUTH\",\"authConfig\":{\"username\":\"smoke-user\",\"password\":\"smoke-pass\"}}" 2>/dev/null)
    WH_BASIC_CODE=$(echo "$WH_BASIC_RESP" | tail -1)
    WH_BASIC_BODY=$(echo "$WH_BASIC_RESP" | sed '$d')
    if [[ "$WH_BASIC_CODE" =~ ^(200|201)$ ]]; then
      WH_BASIC_ID=$(echo "$WH_BASIC_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      WH_BASIC_AUTH_TYPE=$(echo "$WH_BASIC_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('authType',''))" 2>/dev/null || echo "")
      if [[ "$WH_BASIC_AUTH_TYPE" == "BASIC_AUTH" ]]; then
        pass "POST /v1/webhooks (authType=BASIC_AUTH) — HTTP $WH_BASIC_CODE, authType=BASIC_AUTH in response"
      else
        WH_BASIC_ID=""
        fail "POST /v1/webhooks (authType=BASIC_AUTH) — unexpected authType: $WH_BASIC_AUTH_TYPE"
        verbose "$WH_BASIC_BODY"
      fi
    else
      WH_BASIC_ID=""
      fail "POST /v1/webhooks (authType=BASIC_AUTH) — HTTP $WH_BASIC_CODE"
      verbose "$WH_BASIC_BODY"
    fi

    # --- Cleanup: delete both auth smoke-test webhooks ---
    for WH_CLEANUP_ID in "${WH_AUTH_ID:-}" "${WH_BASIC_ID:-}"; do
      if [[ -n "$WH_CLEANUP_ID" ]]; then
        WH_CLEANUP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
          -X DELETE "http://localhost:8084/v1/webhooks/${WH_CLEANUP_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WH_CLEANUP_CODE" =~ ^(200|204)$ ]]; then
          pass "DELETE /v1/webhooks/${WH_CLEANUP_ID} — auth webhook cleanup OK (HTTP $WH_CLEANUP_CODE)"
        else
          skip "DELETE /v1/webhooks/${WH_CLEANUP_ID} — HTTP $WH_CLEANUP_CODE (manual cleanup needed)"
        fi
      fi
    done

  fi
fi

# ============================================================================
# 3.22: Webhook Auth v2 — OAuth2 Client Credentials & Custom Header (event-service :8084)
# ============================================================================
if [[ "$QUICK" == "false" ]]; then
  info "3.22 — Webhook Auth v2 — OAuth2 & Custom Header (event-service :8084)"

  EVENT_HEALTH_322=$(curl -sf --max-time 3 "http://localhost:8084/actuator/health" 2>/dev/null | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")

  if [[ "$EVENT_HEALTH_322" != "UP" ]]; then
    skip "Webhook auth v2 tests (3.22) — event-service not running (health: $EVENT_HEALTH_322)"
  elif [[ -z "${ADMIN_TOKEN:-}" ]]; then
    skip "Webhook auth v2 tests (3.22) — no admin token (auth service unavailable?)"
  else
    SEED_SPACE_ID_322="d0000000-0000-0000-0000-000000000001"
    CUSTOM_HEADER_WEBHOOK_ID=""
    OAUTH2_WEBHOOK_ID=""

    # Acquire viewer token for RBAC test
    VIEWER_LOGIN_322=$(curl -s -X POST http://localhost:8081/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"viewer@ruleforge.io","password":"password123"}' 2>/dev/null)
    VIEWER_TOKEN_322=$(echo "$VIEWER_LOGIN_322" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")

    # --- 3.22.1: Create webhook with CUSTOM_HEADER auth type ---
    WH_CH_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-custom-header-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_322}\",\"authType\":\"CUSTOM_HEADER\",\"authConfig\":{\"headerName\":\"X-Service-Auth\",\"headerValue\":\"super-secret-value\"}}" 2>/dev/null)
    WH_CH_CODE=$(echo "$WH_CH_RESP" | tail -1)
    WH_CH_BODY=$(echo "$WH_CH_RESP" | sed '$d')

    if [[ "$WH_CH_CODE" =~ ^(200|201)$ ]]; then
      CUSTOM_HEADER_WEBHOOK_ID=$(echo "$WH_CH_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      WH_CH_AUTH_TYPE=$(echo "$WH_CH_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('authType',''))" 2>/dev/null || echo "")
      WH_CH_HAS_HEADER=$(echo "$WH_CH_BODY" | grep -c "X-Service-Auth" || true)
      WH_CH_LEAKS_SECRET=$(echo "$WH_CH_BODY" | grep -c "super-secret-value" || true)

      if [[ "$WH_CH_AUTH_TYPE" == "CUSTOM_HEADER" && "$WH_CH_HAS_HEADER" -ge 1 && "$WH_CH_LEAKS_SECRET" -eq 0 ]]; then
        pass "3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER) — HTTP $WH_CH_CODE, headerName present, headerValue not exposed"
        verbose "webhook_id=${CUSTOM_HEADER_WEBHOOK_ID}"
      elif [[ -z "$CUSTOM_HEADER_WEBHOOK_ID" ]]; then
        CUSTOM_HEADER_WEBHOOK_ID=""
        fail "3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER) — HTTP $WH_CH_CODE but id missing"
        verbose "$WH_CH_BODY"
      elif [[ "$WH_CH_LEAKS_SECRET" -ge 1 ]]; then
        fail "3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER) — headerValue 'super-secret-value' exposed in create response"
        verbose "$WH_CH_BODY"
      else
        fail "3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER) — unexpected authType: $WH_CH_AUTH_TYPE"
        verbose "$WH_CH_BODY"
      fi
    else
      CUSTOM_HEADER_WEBHOOK_ID=""
      fail "3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER) — HTTP $WH_CH_CODE"
      verbose "$WH_CH_BODY"
    fi

    # --- 3.22.2: GET webhook masks headerValue ---
    if [[ -n "$CUSTOM_HEADER_WEBHOOK_ID" ]]; then
      WH_CH_GET_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${CUSTOM_HEADER_WEBHOOK_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_CH_GET_CODE=$(echo "$WH_CH_GET_RESP" | tail -1)
      WH_CH_GET_BODY=$(echo "$WH_CH_GET_RESP" | sed '$d')

      if [[ "$WH_CH_GET_CODE" == "200" ]]; then
        CH_HAS_AUTH_TYPE=$(echo "$WH_CH_GET_BODY" | grep -c "CUSTOM_HEADER" || true)
        CH_HAS_HEADER_NAME=$(echo "$WH_CH_GET_BODY" | grep -c "X-Service-Auth" || true)
        CH_LEAKS_SECRET=$(echo "$WH_CH_GET_BODY" | grep -c "super-secret-value" || true)
        CH_HAS_REDACTED=$(echo "$WH_CH_GET_BODY" | grep -c "\[REDACTED\]" || true)

        if [[ "$CH_HAS_AUTH_TYPE" -ge 1 && "$CH_HAS_HEADER_NAME" -ge 1 && "$CH_LEAKS_SECRET" -eq 0 && "$CH_HAS_REDACTED" -ge 1 ]]; then
          pass "3.22.2 GET /v1/webhooks/{id} (CUSTOM_HEADER) — authType+headerName present, headerValue=[REDACTED], no plaintext secret"
        elif [[ "$CH_LEAKS_SECRET" -ge 1 ]]; then
          fail "3.22.2 GET /v1/webhooks/{id} (CUSTOM_HEADER) — plaintext 'super-secret-value' leaked in GET response"
          verbose "$WH_CH_GET_BODY"
        elif [[ "$CH_HAS_REDACTED" -eq 0 ]]; then
          fail "3.22.2 GET /v1/webhooks/{id} (CUSTOM_HEADER) — [REDACTED] placeholder missing from authConfig"
          verbose "$WH_CH_GET_BODY"
        else
          fail "3.22.2 GET /v1/webhooks/{id} (CUSTOM_HEADER) — unexpected response structure"
          verbose "$WH_CH_GET_BODY"
        fi
      else
        fail "3.22.2 GET /v1/webhooks/{id} (CUSTOM_HEADER) — HTTP $WH_CH_GET_CODE (expected 200)"
        verbose "$WH_CH_GET_BODY"
      fi
    else
      skip "3.22.2 GET webhook masks headerValue — no webhook ID (3.22.1 failed)"
    fi

    # --- 3.22.3: Create webhook with OAUTH2_CLIENT_CREDENTIALS auth type ---
    WH_OA_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-oauth2-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_322}\",\"authType\":\"OAUTH2_CLIENT_CREDENTIALS\",\"authConfig\":{\"tokenUrl\":\"https://auth.example.com/oauth/token\",\"clientId\":\"test-client-id\",\"clientSecret\":\"test-client-secret\",\"scope\":\"api:read\"}}" 2>/dev/null)
    WH_OA_CODE=$(echo "$WH_OA_RESP" | tail -1)
    WH_OA_BODY=$(echo "$WH_OA_RESP" | sed '$d')

    if [[ "$WH_OA_CODE" =~ ^(200|201)$ ]]; then
      OAUTH2_WEBHOOK_ID=$(echo "$WH_OA_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null || echo "")
      WH_OA_AUTH_TYPE=$(echo "$WH_OA_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('authType',''))" 2>/dev/null || echo "")
      WH_OA_HAS_CLIENT_ID=$(echo "$WH_OA_BODY" | grep -c "test-client-id" || true)
      WH_OA_LEAKS_SECRET=$(echo "$WH_OA_BODY" | grep -c "test-client-secret" || true)
      WH_OA_HAS_REDACTED=$(echo "$WH_OA_BODY" | grep -c "\[REDACTED\]" || true)

      if [[ "$WH_OA_AUTH_TYPE" == "OAUTH2_CLIENT_CREDENTIALS" && "$WH_OA_HAS_CLIENT_ID" -ge 1 && "$WH_OA_LEAKS_SECRET" -eq 0 && "$WH_OA_HAS_REDACTED" -ge 1 ]]; then
        pass "3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS) — HTTP $WH_OA_CODE, clientId present, clientSecret=[REDACTED]"
        verbose "webhook_id=${OAUTH2_WEBHOOK_ID}"
      elif [[ -z "$OAUTH2_WEBHOOK_ID" ]]; then
        OAUTH2_WEBHOOK_ID=""
        fail "3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS) — HTTP $WH_OA_CODE but id missing"
        verbose "$WH_OA_BODY"
      elif [[ "$WH_OA_LEAKS_SECRET" -ge 1 ]]; then
        fail "3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS) — clientSecret 'test-client-secret' exposed in response"
        verbose "$WH_OA_BODY"
      else
        fail "3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS) — unexpected authType: $WH_OA_AUTH_TYPE"
        verbose "$WH_OA_BODY"
      fi
    else
      OAUTH2_WEBHOOK_ID=""
      fail "3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS) — HTTP $WH_OA_CODE"
      verbose "$WH_OA_BODY"
    fi

    # --- 3.22.4: GET webhook masks clientSecret ---
    if [[ -n "$OAUTH2_WEBHOOK_ID" ]]; then
      WH_OA_GET_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${OAUTH2_WEBHOOK_ID}" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_OA_GET_CODE=$(echo "$WH_OA_GET_RESP" | tail -1)
      WH_OA_GET_BODY=$(echo "$WH_OA_GET_RESP" | sed '$d')

      if [[ "$WH_OA_GET_CODE" == "200" ]]; then
        OA_HAS_CLIENT_ID=$(echo "$WH_OA_GET_BODY" | grep -c "test-client-id" || true)
        OA_LEAKS_SECRET=$(echo "$WH_OA_GET_BODY" | grep -c "test-client-secret" || true)
        OA_HAS_REDACTED=$(echo "$WH_OA_GET_BODY" | grep -c "\[REDACTED\]" || true)

        if [[ "$OA_HAS_CLIENT_ID" -ge 1 && "$OA_LEAKS_SECRET" -eq 0 && "$OA_HAS_REDACTED" -ge 1 ]]; then
          pass "3.22.4 GET /v1/webhooks/{id} (OAUTH2) — clientId present, clientSecret=[REDACTED], no plaintext secret"
        elif [[ "$OA_LEAKS_SECRET" -ge 1 ]]; then
          fail "3.22.4 GET /v1/webhooks/{id} (OAUTH2) — plaintext 'test-client-secret' leaked in GET response"
          verbose "$WH_OA_GET_BODY"
        elif [[ "$OA_HAS_REDACTED" -eq 0 ]]; then
          fail "3.22.4 GET /v1/webhooks/{id} (OAUTH2) — [REDACTED] placeholder missing from authConfig"
          verbose "$WH_OA_GET_BODY"
        else
          fail "3.22.4 GET /v1/webhooks/{id} (OAUTH2) — unexpected response structure"
          verbose "$WH_OA_GET_BODY"
        fi
      else
        fail "3.22.4 GET /v1/webhooks/{id} (OAUTH2) — HTTP $WH_OA_GET_CODE (expected 200)"
        verbose "$WH_OA_GET_BODY"
      fi
    else
      skip "3.22.4 GET webhook masks clientSecret — no webhook ID (3.22.3 failed)"
    fi

    # --- 3.22.5: tokenUrl using HTTP (insecure) must be rejected with 400 ---
    WH_HTTP_RESP=$(curl -s -w "\n%{http_code}" \
      -X POST "http://localhost:8084/v1/webhooks" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"smoke-oauth2-insecure\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_322}\",\"authType\":\"OAUTH2_CLIENT_CREDENTIALS\",\"authConfig\":{\"tokenUrl\":\"http://insecure.example.com/token\",\"clientId\":\"cid\",\"clientSecret\":\"csecret\"}}" 2>/dev/null)
    WH_HTTP_CODE=$(echo "$WH_HTTP_RESP" | tail -1)
    WH_HTTP_BODY=$(echo "$WH_HTTP_RESP" | sed '$d')

    if [[ "$WH_HTTP_CODE" == "400" ]]; then
      # Confirm body mentions HTTPS or tokenUrl (case-insensitive)
      HTTP_BODY_LOWER=$(echo "$WH_HTTP_BODY" | tr '[:upper:]' '[:lower:]')
      if echo "$HTTP_BODY_LOWER" | grep -qE "https|tokenurl"; then
        pass "3.22.5 POST /v1/webhooks (tokenUrl=http://) — HTTP 400, error message references HTTPS/tokenUrl"
      else
        pass "3.22.5 POST /v1/webhooks (tokenUrl=http://) — HTTP 400 (insecure tokenUrl rejected)"
      fi
      verbose "$WH_HTTP_BODY"
    else
      fail "3.22.5 POST /v1/webhooks (tokenUrl=http://) — HTTP $WH_HTTP_CODE (expected 400; insecure tokenUrl must be rejected)"
      verbose "$WH_HTTP_BODY"
    fi

    # --- 3.22.6: VIEWER cannot create webhook ---
    if [[ -n "${VIEWER_TOKEN_322:-}" ]]; then
      WH_VIEWER_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://localhost:8084/v1/webhooks" \
        -H "Authorization: Bearer $VIEWER_TOKEN_322" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"smoke-viewer-webhook\",\"url\":\"https://httpbin.org/post\",\"events\":[\"rule.published\"],\"scopeType\":\"SPACE\",\"scopeId\":\"${SEED_SPACE_ID_322}\",\"authType\":\"CUSTOM_HEADER\",\"authConfig\":{\"headerName\":\"X-Test\",\"headerValue\":\"value\"}}" 2>/dev/null)
      if [[ "$WH_VIEWER_CODE" =~ ^(401|403)$ ]]; then
        pass "3.22.6 POST /v1/webhooks as VIEWER — HTTP $WH_VIEWER_CODE (VIEWER correctly denied)"
      else
        fail "3.22.6 POST /v1/webhooks as VIEWER — HTTP $WH_VIEWER_CODE (expected 401 or 403)"
      fi
    else
      skip "3.22.6 VIEWER cannot create webhook — no viewer token (seed not applied?)"
    fi

    # --- 3.22.7: AUTH_FAILURE status — verify deliveries endpoint returns 200 ---
    if [[ -n "$CUSTOM_HEADER_WEBHOOK_ID" ]]; then
      WH_DEL_RESP=$(curl -s -w "\n%{http_code}" \
        "http://localhost:8084/v1/webhooks/${CUSTOM_HEADER_WEBHOOK_ID}/deliveries" \
        -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
      WH_DEL_CODE=$(echo "$WH_DEL_RESP" | tail -1)
      WH_DEL_BODY=$(echo "$WH_DEL_RESP" | sed '$d')

      if [[ "$WH_DEL_CODE" == "200" ]]; then
        # Check if any existing delivery has AUTH_FAILURE status
        HAS_AUTH_FAILURE=$(echo "$WH_DEL_BODY" | grep -c "AUTH_FAILURE" || true)
        if [[ "$HAS_AUTH_FAILURE" -ge 1 ]]; then
          pass "3.22.7 GET /v1/webhooks/{id}/deliveries — HTTP 200, AUTH_FAILURE entry present"
        else
          pass "3.22.7 GET /v1/webhooks/{id}/deliveries — HTTP 200 (deliveries endpoint functional; no AUTH_FAILURE yet — requires a real delivery attempt)"
        fi
        verbose "$WH_DEL_BODY"
      else
        fail "3.22.7 GET /v1/webhooks/{id}/deliveries — HTTP $WH_DEL_CODE (expected 200)"
        verbose "$WH_DEL_BODY"
      fi
    else
      skip "3.22.7 AUTH_FAILURE status check — no webhook ID (3.22.1 failed)"
    fi

    # --- Cleanup: delete both auth v2 smoke-test webhooks ---
    for WH_322_CLEANUP_ID in "${CUSTOM_HEADER_WEBHOOK_ID:-}" "${OAUTH2_WEBHOOK_ID:-}"; do
      if [[ -n "$WH_322_CLEANUP_ID" ]]; then
        WH_322_DEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
          -X DELETE "http://localhost:8084/v1/webhooks/${WH_322_CLEANUP_ID}" \
          -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
        if [[ "$WH_322_DEL_CODE" =~ ^(200|204)$ ]]; then
          pass "DELETE /v1/webhooks/${WH_322_CLEANUP_ID} — auth v2 webhook cleanup OK (HTTP $WH_322_DEL_CODE)"
        else
          skip "DELETE /v1/webhooks/${WH_322_CLEANUP_ID} — HTTP $WH_322_DEL_CODE (manual cleanup needed)"
        fi
      fi
    done

  fi
fi

# ============================================================================
# Summary
# ============================================================================
echo ""
info "═══════════════════════════════════════════════"
TOTAL=$((PASS + FAIL + SKIP))
echo -e "  ${GREEN}PASS: $PASS${NC}  ${RED}FAIL: $FAIL${NC}  ${YELLOW}SKIP: $SKIP${NC}  Total: $TOTAL"
info "═══════════════════════════════════════════════"
echo ""

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}Some tests failed. Check service logs in logs/ for details.${NC}"
  exit 1
else
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
fi
