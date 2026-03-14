#!/usr/bin/env bash
# ============================================================================
# seed-analytics.sh
#
# Seeds Elasticsearch with realistic DecisionLogDocument records for local dev.
# Generates ~500 decision log entries spread over the last 30 days, covering
# all 8 rule types with realistic latency and error distributions.
#
# Usage:
#   ./scripts/seed-analytics.sh             # Seed 500 documents (default)
#   ./scripts/seed-analytics.sh --count 200 # Custom document count
#   ./scripts/seed-analytics.sh --clear     # Delete index then re-seed
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ES_URL="http://localhost:9200"
ES_INDEX="ruleforge-decisions"
DOC_COUNT=500
CLEAR=false

# --- Args ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    --count) DOC_COUNT="$2"; shift 2 ;;
    --clear) CLEAR=true; shift ;;
    -h|--help)
      echo "Usage: $0 [--count N] [--clear]"
      echo "  --count N  Number of documents to seed (default: 500)"
      echo "  --clear    Delete and recreate the index before seeding"
      exit 0
      ;;
    *) echo "Unknown flag: $1"; exit 1 ;;
  esac
done

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}$1${NC}"; }
success() { echo -e "  ${GREEN}✓${NC}  $1"; }
warn()    { echo -e "  ${YELLOW}!${NC}  $1"; }
error()   { echo -e "  ${RED}✗${NC}  $1"; }

# --- Preflight: check Elasticsearch is reachable ---
info "═══════════════════════════════════════════════"
info "  RuleForge Analytics Seed Data"
info "═══════════════════════════════════════════════"
echo ""

ES_HEALTH=$(curl -sf "${ES_URL}/_cluster/health" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','red'))" 2>/dev/null || echo "unreachable")
if [[ "$ES_HEALTH" == "unreachable" ]]; then
  error "Elasticsearch is not reachable at ${ES_URL}. Start infrastructure first: make infra-up"
  exit 1
fi
success "Elasticsearch reachable (cluster status: ${ES_HEALTH})"

# --- Optionally clear the index ---
if [[ "$CLEAR" == true ]]; then
  info "Clearing existing index ${ES_INDEX}..."
  curl -sf -X DELETE "${ES_URL}/${ES_INDEX}" > /dev/null 2>&1 || true
  success "Index deleted"

  # Re-create with explicit keyword mappings (matches DecisionLogDocument annotations)
  info "Creating index with explicit field mappings..."
  CREATE_RESP=$(curl -s -X PUT "${ES_URL}/${ES_INDEX}" \
    -H 'Content-Type: application/json' \
    -d '{
      "settings": {"number_of_shards": 1, "number_of_replicas": 0},
      "mappings": {
        "properties": {
          "ruleId":         {"type": "keyword"},
          "ruleName":       {"type": "keyword"},
          "ruleType":       {"type": "keyword"},
          "version":        {"type": "integer"},
          "orgId":          {"type": "keyword"},
          "spaceId":        {"type": "keyword"},
          "executionTimeMs":{"type": "long"},
          "rulesEvaluated": {"type": "integer"},
          "rulesFired":     {"type": "integer"},
          "strategy":       {"type": "keyword"},
          "correlationId":  {"type": "keyword"},
          "status":         {"type": "keyword"},
          "errorMessage":   {"type": "text"},
          "timestamp":      {"type": "date"},
          "abTestId":       {"type": "keyword"},
          "abTestVersion":  {"type": "keyword"}
        }
      }
    }')
  if echo "$CREATE_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if d.get('acknowledged') else 1)" 2>/dev/null; then
    success "Index recreated with correct keyword mappings"
  else
    warn "Index creation response: $CREATE_RESP"
  fi
fi

# Ensure index exists (first run without --clear)
INDEX_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "${ES_URL}/${ES_INDEX}" 2>/dev/null)
if [[ "$INDEX_EXISTS" != "200" ]]; then
  info "Index does not exist — creating with explicit mappings..."
  curl -s -X PUT "${ES_URL}/${ES_INDEX}" \
    -H 'Content-Type: application/json' \
    -d '{
      "settings": {"number_of_shards": 1, "number_of_replicas": 0},
      "mappings": {
        "properties": {
          "ruleId":         {"type": "keyword"},
          "ruleName":       {"type": "keyword"},
          "ruleType":       {"type": "keyword"},
          "version":        {"type": "integer"},
          "orgId":          {"type": "keyword"},
          "spaceId":        {"type": "keyword"},
          "executionTimeMs":{"type": "long"},
          "rulesEvaluated": {"type": "integer"},
          "rulesFired":     {"type": "integer"},
          "strategy":       {"type": "keyword"},
          "correlationId":  {"type": "keyword"},
          "status":         {"type": "keyword"},
          "errorMessage":   {"type": "text"},
          "timestamp":      {"type": "date"},
          "abTestId":       {"type": "keyword"},
          "abTestVersion":  {"type": "keyword"}
        }
      }
    }' > /dev/null 2>&1
  success "Index created with explicit keyword mappings"
fi

# --- Generate and index documents via Python ---
info "Generating and indexing ${DOC_COUNT} decision log documents..."

python3 << PYEOF
import json
import math
import random
import time
import uuid
from datetime import datetime, timedelta, timezone
import urllib.request
import urllib.error

ES_URL = "${ES_URL}"
ES_INDEX = "${ES_INDEX}"
DOC_COUNT = int(${DOC_COUNT})

# Org / Space IDs from V021 seed data (Acme Corp)
ORG_ID   = "a0000000-0000-0000-0000-000000000001"
SPACE_ID = "d0000000-0000-0000-0000-000000000001"

# Rule IDs from V022 + V024 seed migrations
RULES = [
    # From V022 (basic showcase)
    {"id": "e0000000-0000-0000-0000-000000000001", "name": "Loan Eligibility Check",         "type": "TABLE",     "weight": 15},
    {"id": "e0000000-0000-0000-0000-000000000002", "name": "Fraud Risk Tree",                "type": "TREE",      "weight": 12},
    {"id": "e0000000-0000-0000-0000-000000000003", "name": "Customer Risk Scorecard",        "type": "SCORECARD", "weight": 8},
    {"id": "e0000000-0000-0000-0000-000000000004", "name": "Dynamic Pricing Script",         "type": "SCRIPT",    "weight": 6},
    {"id": "e0000000-0000-0000-0000-000000000005", "name": "Loan Application Pipeline",      "type": "FLOW",      "weight": 5},
    {"id": "e0000000-0000-0000-0000-000000000006", "name": "Shipping Rate Decision Table",   "type": "DMN",       "weight": 5},
    {"id": "e0000000-0000-0000-0000-000000000007", "name": "Churn Prediction Ensemble",      "type": "ENSEMBLE",  "weight": 3},
    {"id": "e0000000-0000-0000-0000-000000000008", "name": "Login Anomaly Detector",         "type": "CEP",       "weight": 2},
    # From V024 (comprehensive)
    {"id": "e1000000-0000-0000-0000-000000000001", "name": "Employee Benefits Eligibility",  "type": "TABLE",     "weight": 12},
    {"id": "e1000000-0000-0000-0000-000000000002", "name": "Support Ticket Routing",         "type": "TREE",      "weight": 10},
    {"id": "e1000000-0000-0000-0000-000000000003", "name": "Vendor Risk Assessment",         "type": "SCORECARD", "weight": 7},
    {"id": "e1000000-0000-0000-0000-000000000004", "name": "Tax Calculation Engine",         "type": "SCRIPT",    "weight": 6},
    {"id": "e1000000-0000-0000-0000-000000000005", "name": "Loan Application Pipeline v2",   "type": "FLOW",      "weight": 4},
    {"id": "e1000000-0000-0000-0000-000000000006", "name": "Shipping Rate Calculator",       "type": "DMN",       "weight": 4},
    {"id": "e1000000-0000-0000-0000-000000000007", "name": "Customer Churn Prediction",      "type": "ENSEMBLE",  "weight": 3},
    {"id": "e1000000-0000-0000-0000-000000000008", "name": "Login Anomaly Detector v2",      "type": "CEP",       "weight": 1},
    # From V025 (callable actions)
    {"id": "e2000000-0000-0000-0000-000000000001", "name": "Mortgage Qualification Tree",    "type": "TREE",      "weight": 8},
]

STRATEGIES = ["FIRST_MATCH", "ALL_MATCH", "BEST_MATCH", "SEQUENTIAL", "FORWARD_CHAIN"]

# Error messages to randomly assign to ERROR docs
ERROR_MESSAGES = [
    "NullPointerException: fact 'income' is null",
    "Timeout executing rule after 5000ms",
    "Data enrichment failed: connector 'crm-api' unreachable",
    "Invalid input: field 'creditScore' must be between 300 and 850",
    "Execution interrupted: circuit breaker OPEN",
    "Script evaluation error: ReferenceError: taxBracket is not defined",
    "Rule version mismatch: expected v2 but got v1",
    "Working memory overflow: too many facts",
]

def lognormal_ms(mu_ms, sigma=0.8):
    """Generate log-normal execution time in ms, clamped to [1, 2000]."""
    ln_mu = math.log(mu_ms)
    val = math.exp(random.gauss(ln_mu, sigma))
    return max(1, min(2000, int(val)))

def weighted_choice(items):
    """Pick an item from list of dicts with 'weight' key."""
    total = sum(r["weight"] for r in items)
    r = random.uniform(0, total)
    cumulative = 0
    for item in items:
        cumulative += item["weight"]
        if r <= cumulative:
            return item
    return items[-1]

def random_timestamp(days_back=30):
    """Random timestamp in the last N days, biased toward recent dates."""
    now = datetime.now(timezone.utc)
    # Exponential bias: more recent = more likely
    age_days = random.expovariate(0.15)
    age_days = min(age_days, days_back)
    # Add intra-day hour variation (business hours weighted)
    hour_bias = random.choices(range(24), weights=[
        1,1,1,1,1,2,  # 00-05: low
        3,5,8,10,10,10,  # 06-11: rising
        9,10,10,10,9,8,  # 12-17: peak
        6,4,3,2,2,1      # 18-23: declining
    ])[0]
    ts = now - timedelta(days=age_days, hours=random.randint(0,23), minutes=random.randint(0,59), seconds=random.randint(0,59))
    ts = ts.replace(hour=hour_bias)
    return ts.strftime("%Y-%m-%dT%H:%M:%S.000Z")

def make_doc(rule):
    """Build a single DecisionLogDocument dict."""
    is_error = random.random() < 0.08  # 8% error rate

    # Latency varies by rule type
    base_latency = {
        "TABLE": 15, "TREE": 20, "SCORECARD": 25,
        "DMN": 18, "SCRIPT": 45, "FLOW": 80,
        "ENSEMBLE": 60, "CEP": 35
    }.get(rule["type"], 25)

    exec_ms = lognormal_ms(base_latency)
    rules_eval = random.randint(1, 15)
    rules_fired = 0 if is_error else random.randint(1, rules_eval)

    doc = {
        "id": str(uuid.uuid4()),
        "ruleId": rule["id"],
        "ruleName": rule["name"],
        "ruleType": rule["type"],
        "version": random.choice([1, 1, 1, 2]),  # mostly v1
        "orgId": ORG_ID,
        "spaceId": SPACE_ID,
        "executionTimeMs": exec_ms,
        "rulesEvaluated": rules_eval,
        "rulesFired": rules_fired,
        "strategy": random.choice(STRATEGIES),
        "correlationId": str(uuid.uuid4()),
        "status": "ERROR" if is_error else "SUCCESS",
        "timestamp": random_timestamp(30),
        "inputFacts": {"__seed": True},
        "outputResult": {} if is_error else {"result": "ok"},
    }
    if is_error:
        doc["errorMessage"] = random.choice(ERROR_MESSAGES)

    # Randomly attach to A/B test (5% of docs)
    if random.random() < 0.05:
        doc["abTestId"] = "ab-test-loan-v2"
        doc["abTestVersion"] = random.choice(["A", "B"])

    return doc

# --- Build bulk body ---
docs = []
for _ in range(DOC_COUNT):
    rule = weighted_choice(RULES)
    doc = make_doc(rule)
    docs.append(doc)

# Build ES bulk format (newline-delimited JSON)
bulk_lines = []
for doc in docs:
    meta = json.dumps({"index": {"_index": ES_INDEX, "_id": doc["id"]}})
    source = json.dumps(doc)
    bulk_lines.append(meta)
    bulk_lines.append(source)
bulk_lines.append("")  # trailing newline required by ES

bulk_body = "\n".join(bulk_lines).encode("utf-8")

# --- POST to ES _bulk API ---
url = f"{ES_URL}/_bulk"
req = urllib.request.Request(
    url,
    data=bulk_body,
    headers={"Content-Type": "application/x-ndjson"},
    method="POST",
)
try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        result = json.loads(resp.read())
        if result.get("errors"):
            error_items = [i for i in result.get("items", []) if i.get("index", {}).get("error")]
            print(f"WARN: {len(error_items)} documents had errors (first: {error_items[0] if error_items else 'n/a'})")
        else:
            print(f"OK: {DOC_COUNT} documents indexed successfully into '{ES_INDEX}'")
except urllib.error.HTTPError as e:
    body = e.read().decode()
    print(f"ERROR: ES bulk index failed HTTP {e.code}: {body[:300]}")
    exit(1)
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYEOF

PY_EXIT=$?
if [[ $PY_EXIT -ne 0 ]]; then
  error "Seeding failed. Check Elasticsearch connectivity."
  exit 1
fi

# --- Refresh the index so data is immediately queryable ---
curl -sf -X POST "${ES_URL}/${ES_INDEX}/_refresh" > /dev/null 2>&1 && \
  success "Index refreshed — data immediately queryable"

# --- Quick verification ---
DOC_TOTAL=$(curl -sf "${ES_URL}/${ES_INDEX}/_count" 2>/dev/null | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('count',0))" 2>/dev/null || echo "?")

echo ""
success "${DOC_TOTAL} total documents in '${ES_INDEX}'"
info ""
info "Analytics dashboard is ready at: http://localhost:3000/analytics"
info ""
