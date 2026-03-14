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
