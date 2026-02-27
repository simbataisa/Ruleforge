# Test Strategy -- Feature Sprint 01

> **Date:** 2026-02-27
> **Author:** Tester & QE Agent
> **Scope:** P0 stories (US-1.1, US-1.2, US-2.1, US-3.1, US-3.2, US-3.3)
> **PRD:** `docs/prd-feature-sprint-01.md`

---

## Executive Summary

This test strategy covers the six P0 user stories in Feature Sprint 01. These stories fix broken or fake functionality: approval RBAC enforcement, approval comments, real analytics data, solver A/B traffic routing, execution event tagging, and A/B test results. The strategy defines happy path scenarios, edge cases, regression risks, and test data requirements for each story. Tests span unit, integration, API contract, and frontend layers.

---

## US-1.1: RBAC Enforcement for Approvals

### Happy Path Scenarios

**HP-1.1.1: OWNER approves a pending rule version**
- Preconditions: Org has `approval_required=true`; rule version is in PENDING_APPROVAL status; user has OWNER role
- Steps:
  1. Authenticate as OWNER user
  2. Call `POST /v1/rules/{ruleId}/versions/{version}/approve` with `{ "approved": true }`
  3. Verify response 200 OK
  4. Verify rule version status transitions to PUBLISHED
- Expected Result: Approval succeeds, version is published

**HP-1.1.2: APPROVER role approves a pending rule version**
- Preconditions: Org has `approval_required=true`; user has APPROVER role; rule version in PENDING_APPROVAL
- Steps:
  1. Authenticate as APPROVER user
  2. Call `POST /v1/rules/{ruleId}/versions/{version}/approve` with `{ "approved": true }`
  3. Verify response 200 OK
- Expected Result: Approval succeeds; APPROVER role is sufficient

### Edge Cases and Negative Tests

**EC-1.1.1: EDITOR attempts to approve -- 403 returned**
- Preconditions: User has EDITOR role; rule in PENDING_APPROVAL
- Steps: Call approve endpoint
- Expected: 403 Forbidden with message "Insufficient permissions to approve rules"

**EC-1.1.2: VIEWER attempts to approve -- 403 returned**
- Preconditions: User has VIEWER role
- Steps: Call approve endpoint
- Expected: 403 Forbidden

**EC-1.1.3: Rule author tries to self-approve**
- Preconditions: User is both APPROVER and the author of the rule version
- Steps: Call approve endpoint for own rule version
- Expected: 400 or 403 with "Cannot approve your own rule version"

**EC-1.1.4: Approve an already-PUBLISHED version**
- Preconditions: Rule version status is PUBLISHED (not PENDING_APPROVAL)
- Steps: Call approve endpoint
- Expected: 400 or 409 with "Version is not in PENDING_APPROVAL status"

**EC-1.1.5: Approve with expired/invalid JWT**
- Preconditions: Token is expired or malformed
- Steps: Call approve endpoint
- Expected: 401 Unauthorized

### Regression Risks

- Existing OWNER approval flow must still work after adding APPROVER role
- Existing rule publishing flow (without approval_required) must be unaffected
- Role hierarchy: OWNER should retain all permissions APPROVER has
- JWT parsing must not break with new role enum value

### Test Data Requirements

| Data | Value |
|------|-------|
| Org with approval_required=true | Acme Corp (test seed) |
| OWNER user | admin@ruleforge.io / password123 |
| EDITOR user | editor@ruleforge.io / password123 |
| VIEWER user | viewer@ruleforge.io / password123 |
| APPROVER user | New test user with APPROVER role (migration V029+) |
| Rule in PENDING_APPROVAL | Create via management API then submit for approval |

---

## US-1.2: Approval Comments

### Happy Path Scenarios

**HP-1.2.1: Post a comment on approval, retrieved in list**
- Preconditions: Rule version in PENDING_APPROVAL; user has APPROVER role
- Steps:
  1. Call `POST /v1/rules/{ruleId}/versions/{v}/approve` with `{ "approved": true, "comment": "Looks good, verified logic" }`
  2. Call `GET /v1/rules/{ruleId}/versions/{v}/comments`
  3. Verify the comment appears with correct user, action=APPROVE, timestamp
- Expected: Comment is persisted and retrievable

**HP-1.2.2: Reply to a comment (threaded, parentId set)**
- Preconditions: An existing approval comment exists
- Steps:
  1. Call `POST /v1/rules/{ruleId}/versions/{v}/comments` with `{ "comment": "Follow-up question", "parentId": "<existing-comment-id>" }`
  2. Retrieve comments list
  3. Verify reply has parentId field matching original comment
- Expected: Threaded comment structure is maintained

### Edge Cases and Negative Tests

**EC-1.2.1: Empty comment body -- validation error**
- Preconditions: Valid auth, rule in PENDING_APPROVAL
- Steps: Call approve with `{ "approved": true, "comment": "" }`
- Expected: 400 Bad Request with validation error "Comment must not be empty"

**EC-1.2.2: Comment body exceeds 2000 characters**
- Preconditions: Valid auth
- Steps: Call approve with comment body of 2001 characters
- Expected: 400 Bad Request with "Comment must not exceed 2000 characters"

**EC-1.2.3: Comment on non-existent rule or version -- 404**
- Steps: Call `POST /v1/rules/nonexistent-id/versions/99/comments`
- Expected: 404 Not Found

**EC-1.2.4: Rejection comment persists and rule returns to DRAFT**
- Steps:
  1. Call approve with `{ "approved": false, "comment": "Missing edge case for null input" }`
  2. Verify rule version status is DRAFT
  3. Verify rejection comment appears in comment list with action=REJECT
- Expected: Comment stored; status reverted to DRAFT

**EC-1.2.5: Comment from user in different org (tenant isolation)**
- Preconditions: User belongs to Org B; rule belongs to Org A
- Steps: Call comment endpoint
- Expected: 403 or 404 (tenant isolation prevents access)

### Regression Risks

- Existing approve/reject flow (without comments) must still function
- If comment parameter is optional, approving without a comment should still work
- Approval audit events in Kafka must still fire correctly
- Frontend approval history panel must render comments without breaking existing layout

### Test Data Requirements

| Data | Value |
|------|-------|
| Rule with PENDING_APPROVAL version | Created via test setup |
| Approver user with valid JWT | admin@ruleforge.io or APPROVER user |
| Existing comment for threading tests | Created in test setup step |
| Cross-tenant user | viewer@ruleforge.io (Test Org, different from Acme Corp) |

---

## US-2.1: Real Analytics Data from Elasticsearch

### Happy Path Scenarios

**HP-2.1.1: Timeline returns actual data points matching raw decision count**
- Preconditions: Elasticsearch has indexed decision logs from the last 24 hours
- Steps:
  1. Call `GET /v1/analytics/dashboard?range=24h`
  2. Verify `totalExecutions` matches ES `count` aggregation
  3. Call `GET /v1/analytics/charts/executions-over-time?range=24h`
  4. Sum all bucket counts in response
  5. Verify sum equals totalExecutions (within rounding tolerance)
- Expected: Chart data points reflect real ES data, not synthetic jitter

**HP-2.1.2: Type distribution percentages sum to 100%**
- Preconditions: Multiple rule types have been executed
- Steps:
  1. Call dashboard endpoint
  2. Extract type distribution data
  3. Sum all percentages
- Expected: Sum equals 100% (within floating point tolerance of 0.1%)

**HP-2.1.3: Latency percentiles maintain ordering invariant (p50 <= p95 <= p99)**
- Preconditions: ES has latency data
- Steps:
  1. Call latency percentiles endpoint
  2. Verify p50 <= p75 <= p90 <= p95 <= p99
- Expected: Percentile ordering is always maintained

### Edge Cases and Negative Tests

**EC-2.1.1: No data in time range -- empty arrays, not error**
- Preconditions: No decision logs in the last 1 hour
- Steps: Call `GET /v1/analytics/dashboard?range=1h`
- Expected: 200 OK with `totalExecutions: 0`, `avgLatencyMs: 0`, `errorRate: 0`, empty chart arrays

**EC-2.1.2: Single data point -- percentiles all equal**
- Preconditions: Only one decision log exists in range
- Steps: Call latency endpoint
- Expected: All percentiles (p50, p95, p99) return the same value

**EC-2.1.3: Very large time range (30 days) -- performance test**
- Preconditions: ES has data spanning 30 days
- Steps:
  1. Call `GET /v1/analytics/dashboard?range=720h`
  2. Measure response time
- Expected: Response completes in < 2 seconds (NFR-P02)

**EC-2.1.4: Elasticsearch unavailable -- graceful error**
- Preconditions: ES cluster is down or unreachable
- Steps: Call dashboard endpoint
- Expected: 503 Service Unavailable with meaningful error message (not 500 stacktrace)

**EC-2.1.5: Tenant isolation -- only own org's data returned**
- Preconditions: Two orgs with decision logs in ES
- Steps: Call dashboard as Org A user
- Expected: Only Org A's decision data is returned; zero data from Org B

### Regression Risks

- Existing `/v1/analytics/dashboard` endpoint contract must not change (frontend depends on response shape)
- Metric card values must still populate correctly in frontend
- Chart components that previously consumed synthetic data must work with real data shape
- Time range selector must continue to function

### Test Data Requirements

| Data | Value |
|------|-------|
| ES decision logs for Acme Corp | At least 100 decision logs across multiple rule types |
| ES decision logs for Test Org | Separate set to verify tenant isolation |
| Multiple rule types executed | TABLE, TREE, SCRIPT at minimum |
| Varied latency values | Range from 1ms to 500ms for percentile testing |
| Error decisions | At least 5 decisions with error status |

---

## US-3.1: Solver A/B Traffic Routing

### Happy Path Scenarios

**HP-3.1.1: ACTIVE test with 50/50 split -- roughly equal distribution**
- Preconditions: A/B test active with `trafficSplitA=50`, versionA=3, versionB=4
- Steps:
  1. Send 1000 solve requests for the rule (without explicit version)
  2. Count how many used version A vs version B
- Expected: Each version gets 480-520 calls (within +/- 5% statistical tolerance)

**HP-3.1.2: ACTIVE test with 80/20 split -- verify skewed distribution**
- Preconditions: A/B test active with `trafficSplitA=80`
- Steps:
  1. Send 1000 solve requests
  2. Count distribution
- Expected: Version A gets ~780-820 calls; Version B gets ~180-220 calls

**HP-3.1.3: No active A/B test -- latest PUBLISHED version used**
- Preconditions: No active A/B test for the rule; rule has published versions
- Steps: Send solve request without explicit version
- Expected: Latest PUBLISHED version is used (existing behavior preserved)

### Edge Cases and Negative Tests

**EC-3.1.1: A/B test just stopped -- cache TTL behavior**
- Preconditions: A/B test was ACTIVE, then stopped
- Steps:
  1. Stop the A/B test
  2. Send solve requests immediately (within 60s cache TTL)
  3. Send solve requests after 60s
- Expected: Within 60s, may still route via A/B (cached); after 60s, normal routing resumes

**EC-3.1.2: Explicit version request overrides A/B routing**
- Preconditions: Active A/B test on rule X
- Steps: Send solve request with explicit `version=3`
- Expected: Version 3 is always used; A/B test does not override explicit version

**EC-3.1.3: Rule with no PUBLISHED versions**
- Preconditions: Rule has only DRAFT versions; active A/B test references these versions
- Steps: Send solve request
- Expected: Error handling unchanged -- appropriate error returned

**EC-3.1.4: Concurrent requests -- thread safety**
- Preconditions: Active A/B test
- Steps: Send 100 concurrent solve requests using parallel threads
- Expected: No race conditions; all requests resolve correctly; no exceptions

**EC-3.1.5: Redis cache unavailable -- fallback behavior**
- Preconditions: Redis is down; A/B test exists in PostgreSQL
- Steps: Send solve request
- Expected: Solver falls back to direct DB query or uses latest published version; no 500 error

### Regression Risks

- Solve request latency must not increase by more than 5ms (NFR-P01)
- Existing rule execution without A/B tests must be completely unaffected
- Redis cache key patterns for compiled rules must not conflict with A/B test cache keys
- API key-based solve requests must work with A/B routing

### Test Data Requirements

| Data | Value |
|------|-------|
| Rule with 2+ published versions | Created via management API |
| Active A/B test (50/50) | Created via management API |
| Active A/B test (80/20) | Second test on different rule |
| Redis running with 60s TTL config | Infrastructure requirement |
| API key for solve requests | Generated via auth-service |

---

## US-3.2: Execution Event Tagging

### Happy Path Scenarios

**HP-3.2.1: During ACTIVE test, decisions have abTestId and variant in ES**
- Preconditions: Active A/B test on rule X
- Steps:
  1. Send solve request for rule X
  2. Wait for Kafka consumption + ES indexing (~5s)
  3. Query ES for the decision log entry
  4. Verify `abTestId` field matches the active test ID
  5. Verify `variant` field is "A" or "B"
- Expected: Both fields present and correct

**HP-3.2.2: Normal execution (no active test) -- abTestId is null**
- Preconditions: No active A/B test for the rule
- Steps:
  1. Send solve request
  2. Query ES for the decision log entry
  3. Check abTestId field
- Expected: `abTestId` is null; `variant` is null

### Edge Cases and Negative Tests

**EC-3.2.1: Test stopped mid-execution**
- Preconditions: A/B test is active; stop the test while requests are in-flight
- Steps:
  1. Start sending solve requests in background
  2. Stop the A/B test
  3. Check decision logs around the stop timestamp
- Expected: Events tagged with test ID OR not tagged (both acceptable); document observed behavior

**EC-3.2.2: Kafka consumer lag -- events still correctly tagged**
- Preconditions: High volume of solve requests creating Kafka consumer lag
- Steps: Verify events indexed after lag still contain correct A/B fields
- Expected: Tags are set at publish time (solver-side), not at consumer time; all events correctly tagged

**EC-3.2.3: ES field mapping validation**
- Steps:
  1. Check ES index mapping for decision log index
  2. Verify `abTestId` is mapped as `keyword` type
  3. Verify `variant` is mapped as `keyword` type
- Expected: Fields are searchable and aggregatable

### Regression Risks

- Existing Kafka event schema must remain backward compatible (new fields are additive)
- Event-service Kafka consumers must not fail on events without A/B fields (older events)
- Elasticsearch index mapping update must not require reindexing existing data
- Decision log table in frontend must handle null abTestId gracefully

### Test Data Requirements

| Data | Value |
|------|-------|
| Active A/B test | Same as US-3.1 test data |
| Kafka cluster running | Infrastructure requirement |
| ES cluster running | Infrastructure requirement |
| Decision log ES index with updated mapping | Applied via ES API or event-service startup |

---

## US-3.3: A/B Test Results Page

### Happy Path Scenarios

**HP-3.3.1: Results page shows correct per-variant counts**
- Preconditions: A/B test has been running; ES has tagged decisions for both variants
- Steps:
  1. Call `GET /v1/analytics/ab-tests/{testId}/results`
  2. Verify response contains variant A and variant B metrics
  3. Cross-reference execution counts with raw ES query filtered by abTestId + variant
- Expected: Counts match exactly

**HP-3.3.2: Error rates calculated correctly per variant**
- Preconditions: Both variants have some error decisions
- Steps:
  1. Call results endpoint
  2. For each variant: verify `errorRate = errorCount / totalCount`
- Expected: Error rates are accurate to 2 decimal places

### Edge Cases and Negative Tests

**EC-3.3.1: Fewer than 1000 samples per variant -- insufficient data indicator**
- Preconditions: A/B test just started; < 1000 decisions per variant
- Steps: Call results endpoint
- Expected: `significance: "INCONCLUSIVE"`, `sufficientSamples: false`

**EC-3.3.2: No decisions yet (brand new test)**
- Preconditions: A/B test just created, zero executions
- Steps: Call results endpoint
- Expected: Zeroed stats for both variants (`executionCount: 0, avgLatency: 0, errorRate: 0`); not a 404 or error

**EC-3.3.3: Test for non-existent testId -- 404**
- Steps: Call `GET /v1/analytics/ab-tests/nonexistent-id/results`
- Expected: 404 Not Found

**EC-3.3.4: Cross-tenant access to another org's test results**
- Preconditions: Test belongs to Org A; user is in Org B
- Steps: Call results endpoint with Org B JWT
- Expected: 403 or 404 (tenant isolation)

### Regression Risks

- Existing A/B test list page must continue to function
- AbTestCard component "View Results" navigation must work
- Analytics service must handle the new endpoint without affecting existing dashboard endpoints

### Test Data Requirements

| Data | Value |
|------|-------|
| Completed A/B test with 500+ decisions per variant | Generated by running solve requests during active test |
| New A/B test with zero decisions | Freshly created test |
| A/B test with error decisions | Solve requests that trigger rule errors |
| Test in different org for isolation check | Test Org A/B test |

---

## Test Automation Strategy

### Unit Tests (Frontend -- Vitest)
- MetricCard component rendering and interaction
- AbTestCard component states and actions
- AbTestDetailPage results rendering

### Integration Tests (Backend -- Spring Boot Test + TestContainers)
- Approval RBAC endpoint tests with different roles
- Approval comments CRUD operations
- Analytics dashboard endpoint with embedded ES
- Solver A/B routing with in-memory Redis

### Contract Tests
- Kafka event schema: verify `abTestId` and `variant` fields are backward compatible
- REST API: verify response shapes for new endpoints match frontend expectations

### Performance Tests
- Solver latency overhead: < 5ms with A/B routing
- Analytics dashboard: < 2s for 30-day range
- A/B traffic distribution: statistical validation over 10,000 requests

---

## Quality Gates

### Pre-Merge Gate
- [ ] All unit tests pass (frontend + backend)
- [ ] Integration tests pass for modified services
- [ ] No P0/P1 open defects
- [ ] Code coverage >= 80% for new code

### Pre-Release Gate
- [ ] All P0 happy path scenarios verified
- [ ] All edge cases tested (automated or manual)
- [ ] Regression suite passes
- [ ] Performance targets met (NFR-P01 through NFR-P04)
- [ ] Security: RBAC tested for all approval endpoints
- [ ] Tenant isolation verified for analytics and A/B results

---

**Sign-off:** Tester & QE Agent -- 2026-02-27
