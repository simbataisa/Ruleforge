# Product Requirements Document: RuleForge Feature Sprint 01

> **Status:** Draft
> **Owner:** Product Owner Agent
> **Date:** 2026-02-27
> **Version:** 1.0
> **Source:** Internal gap analysis of Approvals, Analytics, and A/B Testing

## Changelog

| Date       | Version | Author        | Changes            |
|------------|---------|---------------|--------------------|
| 2026-02-27 | 1.0     | Product Owner | Initial PRD draft  |

---

## 1. Executive Summary

RuleForge has shipped core approval workflows, an analytics dashboard, and A/B testing scaffolding across Phases 1-7. However, each feature area has critical gaps that prevent production readiness:

- **Approvals** lack role-based permissions, discussion threads, and notifications -- meaning any authenticated user can approve any rule with no audit trail of reasoning.
- **Analytics** dashboards render synthetic/jittered data instead of real Elasticsearch aggregations, providing misleading metrics to decision-makers.
- **A/B Testing** has UI and CRUD operations but the solver service never checks the `ab_tests` table -- traffic is never actually split, making the feature entirely non-functional.

This sprint addresses these gaps to bring all three features to production quality. The highest priority items (P0) are those that fix broken or fake functionality; P1 items add enterprise-grade capabilities.

---

## 2. Goals & Success Metrics

| Goal | KPI | Target |
|------|-----|--------|
| Approvals enforce organizational controls | % of rule publishes going through proper approval | 100% for orgs with approvals enabled |
| Analytics show real data | Dashboard metrics match Elasticsearch query results | 100% accuracy (zero synthetic data) |
| A/B tests actually route traffic | % of solve requests correctly routed per split config | Within 2% of configured traffic split |
| Approval cycle time is visible | Average time from PENDING_APPROVAL to resolution | Measurable and displayed in UI |
| Analytics support operational decisions | Users can drill from metric to filtered logs | Click-through rate on drill-down > 30% |
| A/B tests produce actionable results | Tests with statistical significance reached | > 80% of tests reaching significance within configured duration |

---

## 3. Target Users

| Persona | Description | Key Needs | Pain Points |
|---------|-------------|-----------|-------------|
| **Rule Author** | Creates and edits business rules | Submit rules for approval, see approval status, understand rejection reasons | No visibility into why rules were rejected; no discussion thread |
| **Rule Approver** | Reviews and approves/rejects rule changes (team lead, compliance officer) | Review rule diffs, approve/reject with comments, bulk operations | Cannot restrict who can approve; no notifications when approval is needed |
| **Data Analyst** | Monitors rule performance and decision patterns | Real metrics, custom date ranges, CSV export, drill-down | Dashboard shows fake data; cannot export or filter meaningfully |
| **Platform Admin** | Manages org settings, runs A/B tests | Configure A/B tests, see real results, promote winners | A/B tests do nothing; no results collected |
| **Operations Engineer** | Monitors system health and performance | Alert thresholds, error rate monitoring | No alerting; no way to detect degradation |

---

## 4. Functional Requirements

### Epic 1: Approval Workflow Enhancements

| ID | User Story | Acceptance Criteria | Priority | Complexity |
|----|-----------|---------------------|----------|------------|
| US-1.1 | As an org admin, I want to configure which roles can approve rules so that only authorized personnel can publish changes | Given an org with APPROVER role configured, When a user without APPROVER role attempts to approve, Then the request is rejected with 403 | P0 | M |
| US-1.2 | As a rule approver, I want to add comments when approving or rejecting a rule so that authors understand the reasoning | Given a pending approval, When the approver submits a decision, Then a comment is persisted and visible to the author | P0 | M |
| US-1.3 | As a rule author, I want to receive a notification when my rule is approved or rejected so that I can take action promptly | Given an approval decision is made, When the event is published, Then the author receives an in-app notification (and optionally email) | P1 | M |
| US-1.4 | As an approver, I want to receive a notification when a rule is submitted for approval so that I can review it promptly | Given a rule transitions to PENDING_APPROVAL, When the event is published, Then all users with APPROVER role in that space receive a notification | P1 | M |
| US-1.5 | As an approver, I want to approve or reject multiple rules at once so that I can process a backlog efficiently | Given multiple pending approvals selected, When I click bulk approve/reject, Then all selected rules are processed in a single operation | P1 | S |
| US-1.6 | As an org admin, I want to set approval SLA deadlines so that rules do not languish in review | Given an org-level SLA configuration (e.g., 48 hours), When a pending approval exceeds the SLA, Then it is flagged as overdue in the UI and a notification is sent | P2 | L |
| US-1.7 | As an approver, I want to delegate my approval authority to another user while I am unavailable | Given delegation is configured, When a rule needs approval, Then the delegate can approve on behalf of the original approver | P2 | L |

#### P0 Acceptance Criteria Detail

**US-1.1: Role-Based Approval Permissions**
- Given: Org has `approval_required=true` and roles OWNER, EDITOR, APPROVER, VIEWER
- When: User with EDITOR role calls `POST /v1/rules/{id}/versions/{v}/approve`
- Then: Returns 403 Forbidden with message "Insufficient permissions to approve rules"
- When: User with APPROVER or OWNER role calls the same endpoint
- Then: Approval proceeds normally
- Backend: New `APPROVER` role in auth-service; permission check in VersionController
- Migration: Add `APPROVER` to role enum; add org setting `approval_roles` (default: OWNER, APPROVER)

**US-1.2: Approval Comments**
- Given: A rule in PENDING_APPROVAL status
- When: Approver submits `POST /v1/rules/{id}/versions/{v}/approve` with body `{ "approved": true, "comment": "Looks good, verified logic" }`
- Then: Comment is stored in `approval_comments` table with (rule_id, version, user_id, comment, action, timestamp)
- When: Author views the approval history
- Then: All comments are displayed in chronological order with user name and action
- When: Approver rejects with `{ "approved": false, "comment": "Missing edge case for null input" }`
- Then: Rule returns to DRAFT, rejection reason and comment are displayed to author

### Epic 2: Analytics Data Accuracy & Features

| ID | User Story | Acceptance Criteria | Priority | Complexity |
|----|-----------|---------------------|----------|------------|
| US-2.1 | As a data analyst, I want the analytics dashboard to show real metrics from Elasticsearch so that I can trust the data | Given decision logs exist in ES, When I open the dashboard, Then all 4 metric cards and 4 charts reflect actual aggregation queries (zero synthetic data) | P0 | L |
| US-2.2 | As a data analyst, I want the dashboard to auto-refresh so that I see near-real-time data | Given the dashboard is open, When new decisions are logged, Then metrics update within 30 seconds via polling | P1 | S |
| US-2.3 | As a data analyst, I want to select a custom date range so that I can analyze specific time periods | Given the time selector, When I click "Custom", Then a date/time picker allows me to select arbitrary start and end dates | P1 | S |
| US-2.4 | As a data analyst, I want to click a metric card and drill down to the filtered decision logs that compose it | Given the dashboard shows "Error Rate: 2.3%", When I click the error rate card, Then I navigate to the decision log table pre-filtered to show only error decisions in the selected time range | P1 | M |
| US-2.5 | As a data analyst, I want to export decision logs to CSV so that I can perform analysis in external tools | Given a filtered decision log view, When I click "Export CSV", Then a CSV file is downloaded containing the visible columns and all matching rows (up to 10,000) | P1 | M |
| US-2.6 | As an operations engineer, I want to configure alert thresholds so that I am notified when error rates exceed acceptable levels | Given an alert threshold (e.g., error rate > 5%), When the threshold is exceeded, Then a notification is sent to configured recipients | P2 | L |
| US-2.7 | As a data analyst, I want to compare performance between two rule versions so that I can validate changes | Given two versions of a rule, When I select "Compare", Then side-by-side metrics (latency, error rate, throughput) are displayed | P2 | L |

#### P0 Acceptance Criteria Detail

**US-2.1: Real Elasticsearch Aggregations**
- Given: Analytics service has an Elasticsearch connection with indexed decision logs
- When: Dashboard requests `/v1/analytics/dashboard?range=24h`
- Then: Response contains:
  - `totalExecutions`: ES `count` aggregation on decision log index
  - `avgLatencyMs`: ES `avg` aggregation on `latencyMs` field
  - `errorRate`: ES `terms` aggregation on `status` field, calculating error/total ratio
  - `activeRules`: ES `cardinality` aggregation on `ruleId` field
- When: Charts request `/v1/analytics/charts/executions-over-time?range=24h`
- Then: Response contains ES `date_histogram` aggregation with real bucketed counts
- When: No decision logs exist for the time range
- Then: All metrics show 0 (not synthetic data)
- Frontend: Remove all `Math.random()`, `jitter`, and synthetic data generation from chart components
- Backend: Replace mock/stub aggregation responses in analytics-service with real ES query client calls

### Epic 3: A/B Testing End-to-End

| ID | User Story | Acceptance Criteria | Priority | Complexity |
|----|-----------|---------------------|----------|------------|
| US-3.1 | As the solver service, I need to check active A/B tests and route traffic according to the configured split so that A/B testing actually works | Given an active A/B test with 70/30 split on rule X, When 1000 solve requests arrive for rule X, Then approximately 700 use version A and 300 use version B (within statistical tolerance) | P0 | L |
| US-3.2 | As the solver service, I need to record which version was used for each solve request so that results can be analyzed | Given an A/B test is active, When a solve request is processed, Then the decision log includes `abTestId` and `variant` (A or B) fields | P0 | M |
| US-3.3 | As a platform admin, I want to view A/B test results with metrics per variant so that I can determine a winner | Given an active or completed A/B test, When I click "View Results", Then I see per-variant metrics: execution count, avg latency, error rate, and outcome distribution | P0 | L |
| US-3.4 | As a platform admin, I want to see statistical significance of A/B test results so that I can make data-driven decisions | Given test results are displayed, When sufficient data is collected, Then a chi-squared or equivalent test shows confidence level (e.g., 95% significant) and declares a winner if applicable | P1 | L |
| US-3.5 | As a platform admin, I want to promote the winning variant to 100% traffic so that the better rule version is published | Given a completed A/B test with a declared winner, When I click "Promote Winner", Then the winning version becomes the published version and the A/B test is archived | P1 | M |
| US-3.6 | As a platform admin, I want to monitor active A/B tests in real-time so that I can detect issues early | Given an active A/B test, When I view the test detail page, Then I see live-updating charts of per-variant metrics refreshed every 30 seconds | P2 | M |

#### P0 Acceptance Criteria Detail

**US-3.1: Solver Traffic Routing**
- Given: An A/B test exists with `status=ACTIVE`, `ruleId=X`, `versionA=3`, `versionB=4`, `trafficSplitA=70`
- When: A solve request arrives for rule X (without explicit version)
- Then: Solver generates a random value 0-99; if < 70 uses version 3, otherwise version 4
- When: A solve request arrives for rule X with explicit `version=3`
- Then: The explicit version is used (A/B test does not override explicit version requests)
- When: No active A/B test exists for the rule
- Then: Normal version resolution (latest published) is used
- Implementation: Add `AbTestResolver` component to solver-service that queries active tests (cached in Redis with 60s TTL)
- Data: Solver reads from `ab_tests` table via a read-only view or cross-service API call to management-service

**US-3.2: Decision Log Variant Tracking**
- Given: A solve request was routed by an A/B test
- When: The decision log event is published to Kafka
- Then: The event payload includes `abTestId` (UUID) and `variant` ("A" or "B")
- When: The event is indexed in Elasticsearch
- Then: Fields `abTestId` and `variant` are searchable and aggregatable

**US-3.3: A/B Test Results Page**
- Given: An A/B test has been running for 24 hours
- When: Admin clicks "View Results" on the test
- Then: Page shows:
  - Variant A: execution count, avg latency, error rate, top 5 outcomes
  - Variant B: execution count, avg latency, error rate, top 5 outcomes
  - Comparison delta for each metric
- Backend: New endpoint `GET /v1/analytics/ab-tests/{testId}/results` that queries ES with `abTestId` filter and groups by `variant`

---

## 5. Non-Functional Requirements

### Performance

| ID | Requirement | Target | Measurement |
|----|-------------|--------|-------------|
| NFR-P01 | A/B test routing overhead | < 5ms added to solve latency | p99 latency comparison with/without A/B |
| NFR-P02 | Analytics dashboard load time | < 2s for default 24h view | Frontend performance monitoring |
| NFR-P03 | Approval operations | < 200ms p95 response time | APM monitoring |
| NFR-P04 | CSV export (10,000 rows) | < 10s generation time | Load testing |

### Security

| ID | Requirement | Standard | Implementation |
|----|-------------|----------|----------------|
| NFR-S01 | Approval permissions | RBAC | Only APPROVER/OWNER roles can approve; enforced server-side |
| NFR-S02 | A/B test management | RBAC | Only EDITOR+ roles can create/modify A/B tests |
| NFR-S03 | Analytics data access | Tenant isolation | All ES queries scoped by orgId; RLS equivalent |
| NFR-S04 | CSV export | Rate limited | Max 5 exports per user per hour to prevent data exfiltration |

### Scalability

| ID | Requirement | Target | Strategy |
|----|-------------|--------|----------|
| NFR-SC01 | A/B test resolution | 10,000 concurrent tests | Redis cache with 60s TTL |
| NFR-SC02 | ES aggregation queries | < 1s for 30-day window with 10M docs | Pre-computed rollups for common ranges |

### Observability

| ID | Requirement | Target | Tooling |
|----|-------------|--------|---------|
| NFR-O01 | A/B test routing correctness | Actual split within 2% of configured | Monitoring dashboard metric |
| NFR-O02 | Analytics data freshness | Decision logs indexed within 30s of execution | ES index lag metric |

---

## 6. Integration Requirements

| System | Direction | Protocol | Data | Frequency |
|--------|-----------|----------|------|-----------|
| Elasticsearch | Outbound (analytics-service) | REST (ES client) | Decision logs, aggregations | Real-time indexing + on-demand queries |
| Kafka | Inbound/Outbound | Event streaming | `rule.execution` topic with A/B variant fields | Real-time |
| Redis | Outbound (solver-service) | Redis client | Cached A/B test configs | Read with 60s TTL |
| PostgreSQL | Outbound (management-service) | JDBC | `ab_tests`, `approval_comments`, role configs | On-demand |
| Notification System | Outbound (event-service) | Internal API / Email | Approval notifications, SLA alerts | Event-driven |

---

## 7. Data Requirements

### New Tables / Schema Changes

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `approval_comments` | Discussion thread on approvals | id, rule_id, version, user_id, comment, action (APPROVE/REJECT/COMMENT), created_at |
| `approval_settings` | Per-org approval configuration | id, org_id, approval_roles (JSONB), sla_hours, delegation_enabled |
| `ab_test_results` | Materialized A/B test results (optional cache) | id, ab_test_id, variant, execution_count, avg_latency, error_rate, last_computed |
| `notifications` | In-app notification store | id, user_id, type, title, body, read, entity_type, entity_id, created_at |
| `alert_thresholds` | Analytics alert configuration | id, org_id, metric, operator, value, recipients (JSONB), enabled |

### Elasticsearch Index Changes

- Add fields to decision log mapping: `abTestId` (keyword), `variant` (keyword)
- Ensure `ruleId`, `status`, `latencyMs`, `timestamp` are properly mapped for aggregations

### Data Migration

- New Flyway migrations V029-V033 for schema changes
- Backfill: existing decision logs will not have `abTestId`/`variant` (acceptable -- only new logs during active tests)

---

## 8. UX Requirements

### Approvals
- Approval queue page: add comment input field alongside approve/reject buttons
- Approval history panel on rule detail: show threaded comments with user avatars and timestamps
- Notification bell icon in top navigation with unread count badge
- Bulk select checkboxes on approval queue with bulk action toolbar

### Analytics
- Remove all synthetic data generators from frontend chart components
- Add "Custom" option to time range selector with date picker popover
- Add click handlers on metric cards that navigate to filtered decision log view
- Add "Export CSV" button on decision log table toolbar
- Loading skeleton states while ES queries execute

### A/B Testing
- "View Results" navigates to a results page with side-by-side variant comparison cards
- Each card shows: execution count, avg latency, error rate, bar chart of outcome distribution
- Statistical significance badge (e.g., "95% confidence -- Variant A wins")
- "Promote Winner" button (disabled until significance threshold reached)
- Real-time polling indicator on active test pages

---

## 9. Release Strategy

| Release | Scope | Success Criteria |
|---------|-------|-----------------|
| Sprint 01a (P0) | US-1.1, US-1.2, US-2.1, US-3.1, US-3.2, US-3.3 | Approvals enforce roles; dashboard shows real data; A/B tests route traffic and show results |
| Sprint 01b (P1) | US-1.3, US-1.4, US-1.5, US-2.2, US-2.3, US-2.4, US-2.5, US-3.4, US-3.5 | Notifications working; custom date ranges; CSV export; statistical significance; winner promotion |
| Sprint 01c (P2) | US-1.6, US-1.7, US-2.6, US-2.7, US-3.6 | SLA tracking; delegation; alerts; version comparison; real-time monitoring |

---

## 10. Out of Scope

The following items are explicitly **not** included in this sprint:

- **Email delivery infrastructure** -- Notifications will be in-app only for P1. Email integration requires SMTP/SES setup and is deferred.
- **Mobile push notifications** -- No mobile app exists.
- **Multi-level approval chains** -- Only single-level approve/reject. Sequential or parallel multi-approver workflows are deferred.
- **Machine learning-based analytics** -- No anomaly detection or predictive analytics. Only descriptive metrics.
- **A/B testing for non-rule entities** -- Only rule version A/B testing. No UI A/B testing.
- **GraphQL API for analytics** -- REST only. GraphQL is a future consideration.
- **Approval workflow customization per space/folder** -- Approval settings are org-level only.
- **Historic data backfill for A/B fields** -- Existing decision logs will not retroactively gain `abTestId`/`variant` fields.

---

## 11. Dependencies

| Dependency | Feature Area | Description | Risk |
|------------|-------------|-------------|------|
| Elasticsearch cluster health | Analytics | Real aggregations require ES to be properly indexed and performant | Medium -- ES must be populated with real decision logs |
| Kafka `rule.execution` topic schema | A/B Testing | Must add `abTestId` and `variant` fields to event schema | Low -- backward compatible addition |
| Auth-service role model | Approvals | Must support APPROVER role without breaking existing OWNER/EDITOR/VIEWER | Low -- additive change |
| Solver-service DB access | A/B Testing | Solver needs to read `ab_tests` table (currently only reads rules/cache) | Medium -- new cross-concern data access |
| Redis availability | A/B Testing | A/B test config caching requires Redis | Low -- Redis already in use for rule cache |
| Management-service A/B test API | A/B Testing | Solver needs to query active tests; may call management API or read DB directly | Medium -- architectural decision needed |

---

## 12. Prioritized Backlog (RICE Scoring)

| Rank | ID | Feature | Reach | Impact | Confidence | Effort (days) | RICE Score | Priority |
|------|-----|---------|-------|--------|------------|---------------|------------|----------|
| 1 | US-2.1 | Real ES aggregations for analytics | 500 | 3x | 95% | 5 | 285 | P0 |
| 2 | US-3.1 | Solver traffic routing for A/B tests | 300 | 3x | 90% | 5 | 162 | P0 |
| 3 | US-1.1 | Role-based approval permissions | 400 | 3x | 95% | 3 | 380 | P0 |
| 4 | US-3.2 | Decision log variant tracking | 300 | 3x | 95% | 2 | 427 | P0 |
| 5 | US-1.2 | Approval comments | 400 | 2x | 90% | 3 | 240 | P0 |
| 6 | US-3.3 | A/B test results page | 200 | 3x | 85% | 5 | 102 | P0 |
| 7 | US-2.4 | Drill-down from metrics to logs | 400 | 2x | 85% | 3 | 227 | P1 |
| 8 | US-2.5 | CSV export | 300 | 2x | 90% | 3 | 180 | P1 |
| 9 | US-1.3 | Author notification on decision | 400 | 2x | 80% | 3 | 213 | P1 |
| 10 | US-1.4 | Approver notification on submission | 300 | 2x | 80% | 3 | 160 | P1 |
| 11 | US-2.2 | Real-time polling refresh | 400 | 1x | 90% | 2 | 180 | P1 |
| 12 | US-2.3 | Custom date range picker | 400 | 1x | 95% | 2 | 190 | P1 |
| 13 | US-3.4 | Statistical significance calculation | 200 | 2x | 75% | 4 | 75 | P1 |
| 14 | US-3.5 | Winner promotion workflow | 150 | 2x | 80% | 3 | 80 | P1 |
| 15 | US-1.5 | Bulk approve/reject | 200 | 1x | 90% | 2 | 90 | P1 |
| 16 | US-2.6 | Alert thresholds | 100 | 2x | 70% | 5 | 28 | P2 |
| 17 | US-2.7 | Version comparison view | 100 | 2x | 70% | 5 | 28 | P2 |
| 18 | US-3.6 | Real-time A/B test monitoring | 100 | 1x | 75% | 3 | 25 | P2 |
| 19 | US-1.6 | Approval SLA/deadline tracking | 100 | 1x | 70% | 4 | 17 | P2 |
| 20 | US-1.7 | Approval delegation | 50 | 1x | 60% | 5 | 6 | P2 |

**Scoring Scale:**
- Reach: estimated users affected per month
- Impact: 3x = massive (feature broken without it), 2x = high, 1x = medium, 0.5x = low
- Confidence: belief in estimates
- Effort: development days
- RICE: (Reach x Impact x Confidence) / Effort

---

## 13. Traceability Matrix

| PRD Requirement | Feature Area | Architecture Component | Service(s) | DB Migration |
|----------------|-------------|----------------------|------------|-------------|
| US-1.1 | Approvals | Auth RBAC | auth-service, management-service | V029 (APPROVER role, approval_settings) |
| US-1.2 | Approvals | Approval Comments | management-service | V029 (approval_comments table) |
| US-1.3, US-1.4 | Approvals | Notification System | event-service, frontend | V030 (notifications table) |
| US-1.5 | Approvals | Bulk Operations | management-service | None |
| US-2.1 | Analytics | ES Aggregation Layer | analytics-service, frontend | None (ES mapping update) |
| US-2.2 | Analytics | Polling | frontend | None |
| US-2.3 | Analytics | Date Range | frontend, analytics-service | None |
| US-2.4 | Analytics | Drill-down Navigation | frontend | None |
| US-2.5 | Analytics | CSV Export | analytics-service | None |
| US-3.1 | A/B Testing | Solver A/B Resolver | solver-service | V031 (solver read access) |
| US-3.2 | A/B Testing | Kafka Event Schema | solver-service, event-service | None (schema change) |
| US-3.3 | A/B Testing | Results API | analytics-service, frontend | V032 (optional results cache) |
| US-3.4 | A/B Testing | Statistical Engine | analytics-service | None |
| US-3.5 | A/B Testing | Winner Promotion | management-service | None |

---

## 14. Open Questions

| # | Question | Owner | Due Date | Resolution |
|---|----------|-------|----------|-----------|
| 1 | Should solver read `ab_tests` directly from PG or call management-service API? | Solution Architect | Sprint planning | Direct DB read with Redis cache recommended for latency |
| 2 | What statistical test to use for A/B significance? Chi-squared vs Bayesian? | Data Analyst / Architect | Sprint 01b | TBD |
| 3 | Should notification system be in-process or a separate microservice? | Solution Architect | Sprint planning | Recommend extending event-service |
| 4 | What is the maximum number of concurrent A/B tests per rule? | Product Owner | Sprint planning | Recommend: 1 active test per rule at a time |
| 5 | Should approval comments support markdown/rich text? | UX Designer | Sprint planning | Recommend: plain text for v1, markdown for v2 |

---

## 15. Handoff Notes

**Artifacts Consumed:** Existing codebase analysis (platform/, frontend/, db/migrations/)
**Artifacts Produced:** This PRD (docs/prd-feature-sprint-01.md)
**Next Agents:** Solution Architect (architecture design), UX/UI Designer (page designs), Tech Lead (story breakdown)

**Key Decisions Made:**
1. P0 focuses on fixing broken/fake functionality (real ES data, actual A/B routing, approval permissions)
2. Only single-level approvals -- no multi-approver chains this sprint
3. In-app notifications only -- email deferred
4. A/B test routing uses Redis-cached config for minimal solver latency impact
5. One active A/B test per rule at a time (simplifies routing logic)
6. Statistical significance deferred to P1 (P0 gets raw metrics first)
