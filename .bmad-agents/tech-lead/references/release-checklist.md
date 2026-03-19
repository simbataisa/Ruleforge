# Release Checklist - Comprehensive Pre and Post Release Guide

## Overview

This checklist ensures every release meets quality standards and is ready for production. Use this as a gate before deployment. Nothing ships without checking all items.

---

## PRE-RELEASE CHECKLIST

### 1. Code Quality Gates

Code must meet quality standards before release consideration.

- [ ] **All PRs merged to main have been reviewed**
  - No PRs in "Awaiting Review" status
  - No PRs with "Review Requested" that are >1 hour old
  - All reviewers have approved

- [ ] **No critical SonarQube issues**
  - SonarQube rating is A or B (not C, D, E)
  - No Security Hotspots marked "To Review"
  - No Code Smells with "Blocker" severity
  - Run: `sonarqube scan --project=payment-service`

- [ ] **Code coverage meets threshold**
  - Line coverage: ≥80% (for new code)
  - Branch coverage: ≥70%
  - Run: `npm test -- --coverage` or `pytest --cov=. --cov-report=term-missing`
  - Report committed to PR: `/coverage/index.html`

- [ ] **No compiler/lint warnings**
  - TypeScript: 0 errors, 0 warnings
  - ESLint: 0 errors, 0 warnings
  - Run: `npm run lint` or `cargo clippy`
  - Any pre-existing warnings should be marked as such

- [ ] **All tests passing in CI**
  - GitHub Actions: All green checkmarks
  - No tests skipped (`@skip`, `@pytest.mark.skip`, `xit`)
  - No flaky tests (tests that pass sometimes, fail sometimes)
  - If flakiness suspected, run tests 5 times: `npm run test:unit -- --repeat 5`

### 2. Testing Gates

Testing must prove features work and regressions don't exist.

- [ ] **All P0 and P1 test cases passed**
  - Test case IDs listed: TC-001, TC-002, ... TC-010
  - Manual sign-off from QA: `QA verified all critical tests pass`
  - Evidence: Screenshot or export of test results

- [ ] **Regression test suite passed**
  - Full regression suite executed: 150 test cases
  - All tests passed (0 failures, 0 skipped)
  - Report: `/reports/regression-2024-03-14.html`
  - Timestamp: Less than 24 hours old

- [ ] **Performance test passed SLO**
  - Load test run: 1000 RPS for 5 minutes
  - P99 latency: <500ms (SLO requirement)
  - Error rate: <0.1% (SLO requirement)
  - Throughput: >1000 RPS maintained
  - Report: `/reports/perf-2024-03-14.json`
  - Baseline comparison: P99 latency same or better than previous release

- [ ] **Security scan clean**
  - SAST (static analysis): 0 high/critical issues
  - DAST (dynamic analysis): 0 high/critical vulnerabilities
  - Run: `snyk test` and `./owasp-zap-scan.sh`
  - Dependency vulnerabilities: 0 high/critical
  - Run: `npm audit` (max 8 moderate)

- [ ] **Accessibility audit passed**
  - WCAG 2.1 AA compliance: 0 critical violations
  - Axe scan: 0 violations
  - Manual screen reader test: Navigation works
  - Keyboard-only navigation: Tab order logical, all buttons accessible
  - Run: `axe chrome` or `lighthouse --view`

- [ ] **Mobile responsiveness verified (if applicable)**
  - Mobile viewport 375x667 (iPhone 8 size): All elements visible, no horizontal scroll
  - Tablet viewport 768x1024 (iPad): Layout correct
  - Desktop 1920x1080: No overflow or squishing
  - Tested in Chrome DevTools device emulation

### 3. Database Changes

Database changes are high-risk. Must be thoroughly validated.

- [ ] **Database migrations reviewed**
  - Migration file exists: `/db/migrations/20240314_add_notification_preferences.sql`
  - DDL is correct: No syntax errors
  - Rollback script exists and tested: `/db/migrations/20240314_rollback.sql`
  - Migration tested on staging DB: Success, no timeout

- [ ] **No breaking schema changes without version bump**
  - If removing column: Must be 2 releases after deprecation
  - If renaming table: Deprecated old table first
  - If changing column type: Ensured no data loss (e.g., VARCHAR to INT)
  - Migration is forward and backward compatible

- [ ] **Data backups confirmed**
  - Staging database backed up before migration: Backup ID: `backup-2024-03-14-14-00`
  - Production backup scheduled for release window: Confirmed in backup calendar
  - Backup can be restored within 1 hour if needed

- [ ] **New indexes are not blocking**
  - If adding index to large table (>1M rows): Used CONCURRENT
  - Estimated index creation time: <10 minutes
  - Index tested on staging: Improves query by ≥10%
  - Command: `CREATE INDEX CONCURRENTLY idx_user_id ON payments(user_id);`

- [ ] **No large data migrations in release window**
  - If migrating data: Done before release (e.g., end of day-1)
  - No data migration runs during release
  - If must migrate: Impact <5 minutes of downtime
  - Plan: Migration + verification + rollback plan documented

### 4. Configuration and Secrets

Misconfigured environment causes production outages.

- [ ] **All environment variables documented**
  - List: DATABASE_URL, STRIPE_API_KEY, SENDGRID_API_KEY, etc.
  - Each documented: Purpose, required, default value (if non-sensitive)
  - File: `/docs/environment-variables.md`
  - New variables for this release: NOTIFICATION_EMAIL_FROM (required), NOTIFICATION_BATCH_SIZE (default: 100)

- [ ] **No secrets in code repository**
  - `git log --all -S STRIPE_API_KEY`: No API keys in history
  - `grep -r "sk_live_" .`: No Stripe live keys in code
  - `grep -r "password" src/`: No hardcoded passwords
  - If found: Rotate secrets immediately

- [ ] **Secrets stored in secure vault**
  - All secrets in HashiCorp Vault or AWS Secrets Manager
  - Never committed to git
  - Accessed only via secure APIs
  - Audit log shows who accessed which secrets and when

- [ ] **Feature flags configured for release**
  - New feature behind feature flag: `notificationEmailEnabled`
  - Default: disabled (safe for gradual rollout)
  - Flag checked in LaunchDarkly or similar
  - Plan: Enable for 10% of users day 1, 50% day 2, 100% day 3

- [ ] **Config validated in staging**
  - Deployed to staging with release config
  - All required env vars present and correct
  - Application starts successfully
  - Core workflows tested with staging config

### 5. Infrastructure Readiness

Infrastructure must handle the load and scale appropriately.

- [ ] **Resource limits set correctly**
  - CPU request: 500m, limit: 1000m (pod limits)
  - Memory request: 512Mi, limit: 1024Mi
  - Pod autoscaling: Min 2 replicas, Max 10 replicas, target CPU 70%
  - Check Kubernetes YAML: `kubectl describe node | grep Allocated`

- [ ] **Autoscaling tested**
  - Autoscaler responds to load: Pods scale from 2 to 5 when load increases
  - Load test result: When load increases to 1000 RPS, system automatically scaled to 5 pods
  - Downscaling works: When load drops, pods scale back to 2
  - Time to scale: <2 minutes from spike to new pods running

- [ ] **Health check endpoints working**
  - Liveness probe: GET /health → 200 OK (service is alive)
  - Readiness probe: GET /ready → 200 OK (service ready for traffic)
  - Startup probe: GET /startup → 200 OK (service finished initializing)
  - Each responds in <100ms
  - Failed health checks trigger pod restart (tested)

- [ ] **SSL/TLS certificates valid**
  - Certificate expiry: >30 days remaining
  - Run: `openssl s_client -connect api.example.com:443 | grep "notAfter"`
  - Certificate matches domain: CN=api.example.com
  - Certificate chain is complete (no missing intermediate)

- [ ] **CDN/Cache configuration correct**
  - CloudFront cache invalidation ready: Pattern `/api/*` (no-cache), `/static/*` (1 year)
  - Cache headers set correctly: Static assets with long TTL, API responses no-cache
  - Tested: Stale content not served after change

- [ ] **Rate limiting and DDoS protection configured**
  - WAF rules active: Blocks malicious patterns
  - Rate limits set: 100 requests per IP per second
  - Tested: Exceeding rate limit returns 429 Too Many Requests

- [ ] **Database capacity sufficient**
  - Disk space: >50% free (not >90% full)
  - Connection pool: Max 100 connections, current 30, capacity comfortable
  - Query performance: P99 latency <500ms at peak load
  - Estimated growth: This release doesn't impact disk significantly

- [ ] **Load balancer configured**
  - Health checks pointing to correct endpoint
  - Backend servers weighted correctly (if any servers are slower)
  - Sticky sessions disabled (unless needed)
  - Connection draining timeout: 30 seconds (enough for in-flight requests)

- [ ] **Backup and disaster recovery tested**
  - Database backup created: Timestamp 2024-03-14 14:00 UTC
  - Backup can be restored: Tested restoration time <30 min
  - Database replication healthy: Replication lag <100ms
  - Failover procedure ready and documented

### 6. Observability Readiness

You cannot operate what you cannot see.

- [ ] **Dashboards updated for new features**
  - Created Datadog dashboard: "Payment Notifications"
  - Metrics tracked: Email send latency, success rate, queue depth
  - Alerts configured: See alerts section
  - Dashboard added to team's dashboard list

- [ ] **Alerts configured**
  - High error rate: >0.5% for 5 min → PagerDuty
  - High latency: P99 >1s for 10 min → PagerDuty
  - Database errors: Any database connection error → Slack #payments-oncall
  - Alerts tested: Triggered alert, confirmed PagerDuty notification received

- [ ] **Runbook updated**
  - Production runbook: `/docs/runbooks/payment-service-runbook.md`
  - New incident types documented: "Email queue backlog"
  - Diagnostic commands added: How to check email queue depth
  - Last reviewed: 2024-03-14 by alice@company.com

- [ ] **On-call team briefed**
  - Release notes shared: https://wiki.company.com/releases/2024-03-14
  - What changed: "New email notification feature for user alerts"
  - What to watch: "Email queue depth, SendGrid API latency"
  - Incident response: "See runbook section on email queue issues"
  - Briefing done: 2024-03-14 16:00 UTC in Slack #payments-oncall

- [ ] **Logging and centralization working**
  - Application logs sent to ELK: Check Kibana dashboard
  - Log level: Info (not Debug, not ERROR only)
  - No sensitive data in logs: Confirmed with grep for passwords, API keys
  - Query logs easily: `log_source:payment-service AND level:error`

- [ ] **Distributed tracing set up (if applicable)**
  - OpenTelemetry or Jaeger installed
  - Traces show request flow through services
  - Slow requests traceable to bottleneck
  - 1% of requests sampled (balance sampling cost vs visibility)

### 7. Stakeholder Readiness

Ensure all non-technical stakeholders are prepared.

- [ ] **Release notes written**
  - File: `/docs/release-notes-2024-03-14.md`
  - User-facing changes: Explained in plain language
  - Technical changes: Summarized for developers
  - Known issues: Listed any known limitations
  - Rollback procedure: Included if needed

- [ ] **Product Owner sign-off received**
  - PO approval: Email with "Approved for release" from po@company.com
  - Features tested match requirements: PO verified functionality
  - No last-minute changes: PO confirms ready to ship

- [ ] **Support team briefed**
  - Email sent to support@company.com
  - What's new: "Users can opt in/out of email notifications"
  - Common issues: "If user doesn't get email, check notification preferences"
  - Escalation: "If email service is down, escalate to #payments-oncall"
  - Briefing completed: 2024-03-13

- [ ] **Documentation updated**
  - User documentation: Updated if user-facing change
  - API documentation: Updated if API change
  - Architecture documentation: Updated if architecture change
  - Links updated in help center

- [ ] **Customer communication plan ready (if needed)**
  - Maintenance window: Scheduled 02:00-04:00 UTC (low traffic)
  - Customer notice: Posted 48 hours before if any downtime expected
  - Status page: https://status.company.com updated with maintenance window
  - No customer communications needed if zero-downtime deployment

- [ ] **Compliance and legal review (if applicable)**
  - Data handling: No PII handling changes, no review needed
  - OR security/compliance: Reviewed and approved
  - OR GDPR/privacy: Any user data use reviewed
  - Sign-off: Received from legal@company.com

---

## DEPLOYMENT CHECKLIST

Execute this checklist during the actual deployment. Have one person execute, one person verify.

### Pre-Deployment (30 minutes before)

- [ ] **Deployment window confirmed**
  - Start time: 2024-03-14 02:00 UTC
  - Expected duration: 30 minutes (can extend to 60 min)
  - Who's involved: alice (executor), bob (verifier), charlie (DBA on standby)
  - Communication channel: #payment-deployment Slack channel (live updates)

- [ ] **Deployment tested in staging**
  - Staging deployment successful: No errors
  - Staging health checks pass: Liveness, readiness, startup all 200 OK
  - Staging smoke tests pass: Core workflows work
  - Performed: 2024-03-14 01:30 UTC

- [ ] **Rollback plan reviewed**
  - Rollback procedure: `kubectl rollout undo deployment/payment-service`
  - Expected time: 5 minutes
  - Rollback tested: Procedure was tested on staging
  - If needed: alice knows exact commands to execute

- [ ] **Database backup created**
  - Backup taken: 2024-03-14 01:45 UTC
  - Backup size: 12.5 GB (compressed)
  - Backup location: S3://company-backups/payment-db-2024-03-14-0145.tar.gz
  - Restoration test: Successfully restored to staging in 15 min

- [ ] **Team ready**
  - Executor (alice): Online and focused
  - Verifier (bob): Online and monitoring
  - DBA (charlie): On standby, reachable via Slack
  - Comms (david): Monitoring Slack and status page

### Deployment Execution (During)

- [ ] **Pre-deployment systems check**
  - Current error rate: 0.05% (normal)
  - Current latency P99: 250ms (normal)
  - Current traffic: 500 RPS (normal)
  - Staging deployment status: All pods running and healthy
  - Database replication lag: 50ms (healthy)

- [ ] **Initiate deployment**
  - Deploy new image: `kubectl set image deployment/payment-service payment-service=payment-service:3.2.1 -n payment`
  - Monitor rollout: `kubectl rollout status deployment/payment-service -n payment --timeout=5m`
  - Expected: Pods rolling out, old pods terminating, new pods starting
  - Time: Should take 2-3 minutes for complete rollout

- [ ] **Health checks pass after deployment**
  - Liveness checks: All return 200 OK
  - Readiness checks: All return 200 OK
  - Pod status: Running (not CrashLoopBackOff or Pending)
  - Confirmed: `kubectl get pods -n payment -l app=payment-service`

- [ ] **Application started successfully**
  - Application logs show no errors: `kubectl logs deployment/payment-service -n payment | tail -20` (check for exceptions)
  - Startup completed: Message "Application ready" in logs
  - Configuration loaded: Message "Config loaded from environment" in logs
  - Database connected: Message "Database connection pool initialized" in logs
  - Time to startup: <30 seconds per pod

- [ ] **Smoke tests pass**
  - Test 1: Create test payment: `curl -X POST http://localhost:8080/api/payments -d '{"amount":10, ...}'`
  - Test 2: Query payment status: `curl http://localhost:8080/api/payments/pay-123`
  - Test 3: Send test notification: Check that test notification is queued
  - All tests pass with 200 status codes

- [ ] **Metrics look normal**
  - Error rate: Still <0.1% (not increasing)
  - Latency P99: <500ms (not degrading)
  - Throughput: 500+ RPS maintained
  - Traffic slowly increasing as requests naturally increase
  - No spikes in exceptions or timeouts

- [ ] **Database replication still healthy**
  - Replication lag: <100ms (still healthy)
  - Confirmed: `SELECT max(pg_last_wal_receive_lsn() - '0/0') FROM pg_replication_slots;` in psql

- [ ] **No customer-facing errors**
  - Monitor #support-incidents Slack (or whatever channel support posts issues)
  - No spike in "payment failed" complaints
  - No emails about system outages
  - Dashboard: Status page shows all green

- [ ] **Canary metrics (if canary deployment)**
  - New version running on 10% of pods
  - Error rate of new version: Same as old version (within 1%)
  - Latency of new version: Same as old version (within 50ms)
  - If metrics match: Proceed to 50% → 100%
  - If metrics diverge: Rollback new version immediately

- [ ] **Feature flag enabled (if applicable)**
  - Feature flag: notificationEmailEnabled changed from false to true
  - 10% rollout: Changed flag to serve feature to 10% of users
  - Time: Wait 10 minutes, check if anything breaks
  - Monitoring: Error rate of new feature below 0.1%

### Post-Deployment (30 minutes after)

- [ ] **Verify deployment is complete**
  - All pods running: `kubectl get pods -n payment` shows all pods Running
  - Desired replicas: 3, Current replicas: 3 (not 2 or 4)
  - Image deployed: `kubectl get deployment payment-service -n payment -o jsonpath='{.spec.template.spec.containers[0].image}'`

- [ ] **Verify service stability (30 minute soak)**
  - Wait 30 minutes, monitor metrics continuously
  - Error rate: Maintained <0.1%
  - Latency P99: Maintained <500ms
  - Throughput: Normal (500-600 RPS)
  - Memory usage: Stable (not growing)
  - Database connections: Stable (not growing)

- [ ] **Verify critical workflows**
  - User can log in: Test account works
  - User can make payment: Test payment processes
  - Payment confirmation sent: Test notification received
  - User can view order history: Dashboard works

- [ ] **Monitor for anomalies**
  - Exception rate: No spike
  - Specific error messages: None repeating or new
  - Performance degradation: None detected
  - Resource exhaustion: CPU <70%, memory <70%

- [ ] **Communicate deployment success**
  - Post in Slack #payment-deployment: "Deployment successful, all health checks passed, monitoring for 24h"
  - Update status page: Remove maintenance notice if applicable
  - Notify team: Deployment complete, no issues found

---

## POST-RELEASE MONITORING (24-Hour Window)

After release, monitor intensively for first 24 hours. New issues often emerge slowly.

### 0-30 Minutes: Immediate Post-Deployment

- [ ] **Dashboard actively monitored**
  - Datadog open in separate window, auto-refresh every 30s
  - Metrics being watched: Error rate, latency, throughput
  - Slack notifications enabled for all P0 alerts
  - On-call ready to respond immediately to any alert

- [ ] **Error rate baseline established**
  - Record current error rate: 0.062% (this becomes baseline)
  - Alert triggers if error rate increases by 50% (to 0.093%)
  - Any spike above 0.2% triggers immediate investigation

- [ ] **Performance metrics within SLO**
  - P99 latency: 280ms (target <500ms) ✓
  - P95 latency: 180ms (for reference)
  - Mean latency: 120ms
  - All metrics within SLO, no concerns

### 30 Minutes - 4 Hours: Continued Monitoring

Monitor as traffic patterns return to normal. Watch for:

- [ ] **User-visible issues not appearing**
  - No surge in support tickets
  - No complaints on Twitter/social media
  - Status page quiet, no escalations
  - If issue detected, immediately escalate to dev team

- [ ] **Business metrics look normal**
  - Payment success rate: 99.6% (normal, target 99.5%)
  - Payment processing volume: Normal for time of day
  - Transaction values: Normal distribution (not spike or drop)
  - Revenue tracking normally (if system tracks real-time)

- [ ] **Performance metrics stable**
  - Error rate: Stable at 0.06-0.08% (consistent)
  - Latency P99: 270-310ms (consistent, not trending up)
  - Throughput: 500-700 RPS (varies naturally with traffic)
  - No memory leaks: Memory usage stable, not growing over time

- [ ] **No new errors in logs**
  - Check logs for new error messages: `kubectl logs deployment/payment-service -n payment | grep ERROR | tail -20`
  - If new error appears, immediately investigate root cause
  - If clear bug, decide: Fix now with hotfix or monitor for next release

- [ ] **Feature working correctly (if new feature)**
  - Email notifications: Check that emails are being sent
  - Email queue depth: Monitor, should be near 0 (not backlog)
  - Email delivery rate: >99% of notifications should send within 1 hour
  - If issues: Might be processor (SendGrid) issue, not deployment issue

### 4-24 Hours: Soak Period Monitoring

Longer-term monitoring for slow-to-appear issues (memory leaks, connection leaks, etc.).

- [ ] **No memory leaks detected**
  - Memory usage trending: Should be flat over 24h
  - Check: `kubectl top pods -n payment -l app=payment-service` over time
  - If memory increases by >10% over 4 hours: Likely memory leak
  - Action: Investigate code, might need hotfix or rollback

- [ ] **No connection pool leaks**
  - Database connection count: Stable over time
  - Check: Query `SELECT count(*) FROM pg_stat_activity;` periodically
  - If connections accumulate over time: Connection leak
  - Action: Restart pods (clears connections), or rollback if bug is in new code

- [ ] **Cache hit ratio healthy**
  - Redis hit ratio: >80% (good cache performance)
  - If hit ratio <50%: Cache might not be working properly
  - Check: Either cache invalidation is too aggressive, or cache keys changed

- [ ] **Database query performance stable**
  - Slow query log: No new slow queries from new code
  - Check: `SELECT mean_exec_time, query FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;`
  - If new query is in top 10 slowest: Needs optimization (index, query rewrite)

- [ ] **No cumulative degradation**
  - Errors not slowly increasing
  - Latency not slowly increasing
  - Throughput not slowly decreasing
  - If any trending upward: Likely indicates slow resource exhaustion

- [ ] **Configuration changes stable**
  - No unexpected errors from config changes
  - Feature flags behaving as expected
  - If feature flag flipping on/off causes issue: Config problem

---

## Release Rollback Decision Criteria

**When to rollback immediately:**

| Condition | Action | Timeline |
|---|---|---|
| Error rate >5% (any errors at all during ramp) | Rollback | <5 min decision |
| Payment success rate <98% | Rollback | <5 min decision |
| Service completely down (0% uptime) | Rollback | <2 min decision |
| Data corruption detected | Rollback + investigate | <5 min decision |
| Security vulnerability disclosed | Rollback + patch | <10 min decision |

**When to monitor longer (don't rollback immediately):**

| Condition | Threshold | Decision |
|---|---|---|
| Error rate 0.1-1% | Monitor 5 min, if growing trend rollback | 5 min decision window |
| Latency P99 500-1000ms | Monitor 10 min, performance might be normal | 10 min decision window |
| New errors appearing | If <10 errors total in first 30 min, likely edge case | 30 min decision window |

**Manual rollback decision framework:**

1. Measure impact: What percentage of users affected? (1% = wait, 50% = rollback)
2. Measure severity: Can users work around the issue? (No = rollback, Yes = monitor)
3. Measure reversibility: How quickly can we rollback? (<5 min = safe to monitor longer)
4. Measure fix availability: Do we have a fix ready? (Yes = can delay rollback decision)

---

## Post-Release Sign-Off

Once 24-hour monitoring period complete:

- [ ] **Release is stable**
  - 24 hours have passed: 2024-03-15 02:00 UTC
  - No critical issues found
  - Error rate consistent with baseline
  - Metrics all within SLO

- [ ] **Documentation updated**
  - Release notes published: https://company.com/releases/2024-03-14
  - Production runbook updated with any new procedures
  - Architecture documentation updated if needed

- [ ] **Incident register cleared**
  - No open incidents related to this release
  - If incidents occurred: Post-incident reviews completed

- [ ] **Feature flag status**
  - If gradual rollout: Remove feature flag (100% enabled)
  - If issues: Flag remains disabled, fix planned for next release
  - If deprecation: Old code removed in future release

- [ ] **Sign-off recorded**
  - Tech lead sign-off: alice@company.com - "Release 2024-03-14 approved, stable in production"
  - Product owner sign-off: po@company.com - "Feature working as designed"
  - QA sign-off: qa@company.com - "All test cases passed"
  - Recorded in release tracker: Release marked as "SHIPPED"

---

## Troubleshooting Release Issues

### During Deployment: Pods Won't Start

**Symptom:** Pods stuck in CrashLoopBackOff after deploying new image

**Check 1: Invalid image tag**
```bash
# Verify image exists in registry
docker pull payment-service:3.2.1
# If fails: Tag is wrong or image not pushed
```

**Check 2: Resource limits too low**
```bash
# Check memory usage
kubectl logs deployment/payment-service -n payment
# Look for "Killed" messages = OOMKilled

# Solution: Increase memory limit in deployment YAML
# memory: 512Mi → 1024Mi
```

**Check 3: Config/secrets missing**
```bash
# Check pod environment
kubectl exec -it payment-service-abc123 -n payment -- env | grep DATABASE_URL
# If undefined: Add to ConfigMap or Secret
```

### After Deployment: High Error Rate

**Check 1: Database connectivity**
```bash
kubectl exec -it payment-service-abc123 -n payment -- \
  psql -U payment_user -h postgres-primary.default -d payment_db -c "SELECT 1"
```

**Check 2: External service down (Stripe)**
```bash
curl https://status.stripe.com/api/v2/status.json | jq '.page.status'
```

**Check 3: New code bug**
```bash
kubectl logs deployment/payment-service -n payment | grep -i exception
# Look for stack trace, identify bug
```

### Latency Higher Than Baseline

**Check 1: Database query performance**
```bash
psql -d payment_db
SELECT mean_exec_time, query FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 5;
```

**Check 2: Stripe API slow**
```bash
# Check Stripe status
curl https://status.stripe.com/api/v2/status.json

# Check response times in logs
kubectl logs deployment/payment-service -n payment | grep "stripe" | grep -oP 'duration=\K[0-9]+'
```

---

## Release Success Metrics

After release is stable, measure success:

| Metric | Target | Actual |
|--------|--------|--------|
| **Deployment success (first try)** | 100% | 100% ✓ |
| **Time to stable (post-deploy)** | <30 min | 22 min ✓ |
| **Rollback needed** | 0 (no rollbacks) | 0 ✓ |
| **Critical bugs in first 24h** | 0 | 0 ✓ |
| **SLA breaches** | 0 | 0 ✓ |
| **Customer support escalations** | <10 | 3 ✓ |
| **Feature adoption (if tracked)** | N/A | Email opt-ins: 15% of users day 1 ✓ |

All metrics met = **Release successful**
