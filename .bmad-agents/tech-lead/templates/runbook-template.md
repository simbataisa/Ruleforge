# Production Runbook Template - Payment Processing Service

## Runbook Metadata

| Field | Value |
|-------|-------|
| **Service Name** | Payment Processing Service (payment-service) |
| **Current Version** | 3.2.1 (Kubernetes image tag) |
| **Owner Team** | Payments Platform Engineering |
| **On-Call Contact** | Slack channel #payments-oncall / PagerDuty @payments-team |
| **Last Reviewed** | 2024-03-10 by alice@company.com |
| **Criticality** | P0 (Revenue-critical, must not go down) |
| **Runbook Version** | 2.1 (updated after incident 2024-02) |

---

## Service Overview

### What It Does
The Payment Processing Service handles all financial transactions:
- Payment authorization (credit card, debit card, ACH transfer)
- Payment settlement and batch processing
- Refund processing (full and partial)
- Payment verification and fraud detection
- Webhook notifications to other services (Order Service, Billing Service)
- PCI DSS compliance and encryption

### Why It's Critical
- **Revenue dependency:** 100% of company revenue flows through this service. If down, customers cannot purchase. Revenue impact: ~$50K per hour of downtime.
- **Data integrity:** Contains financial records. Data loss would require complete audit trail recovery.
- **Regulatory compliance:** PCI DSS compliance required. Data breaches trigger legal consequences and customer liability.
- **Customer impact:** Customers waiting to complete purchases. Hundreds of failed transactions per minute if service down.

### Downstream Impact If Down

| Service | Impact | SLA Violation |
|---------|--------|---------------|
| Order Service | Cannot complete purchases | Yes (2 minute threshold) |
| Billing Service | Cannot invoice customers | No (batch, not real-time) |
| Email Notification Service | Cannot send payment confirmations | No (async, can catch up) |
| Accounting System | Missing transaction data | No (end-of-day reconciliation) |
| Customer Dashboard | Transaction history unavailable | Yes (customers cannot see orders) |

---

## Architecture Overview

### C4 Architecture Diagram
```
Reference: https://wiki.company.com/payment-service/architecture
Download: payment-service-c4-diagram.pdf
Last updated: 2024-02-15

High-level components:
- API Gateway (Kong) → Payment Service Pod (Kubernetes)
  ├─ Payment Repository (PostgreSQL 15.2)
  ├─ Stripe Adapter (payment processor)
  ├─ Cache (Redis cluster)
  ├─ Message Queue (RabbitMQ, AMQP)
  └─ PCI Vault (HashiCorp Vault for card storage)
```

---

## Key Dependencies

| Service/System | Type | Criticality | SLA | Fallback if Unavailable |
|---|---|---|---|---|
| **Stripe API** | Payment Processor | Critical | 99.99% | Use Fallback Processor (Adyen), alert PagerDuty immediately |
| **PostgreSQL Database** | Data Store | Critical | 99.95% | Connect to read-replica, attempt failover, escalate to DBA |
| **Redis Cache** | Cache/Session Store | High | 99.9% | Degrade gracefully, queries bypass cache |
| **RabbitMQ** | Message Queue | High | 99.5% | Payment goes through but notifications may be delayed |
| **Auth Service** | Authentication | Critical | 99.99% | Fallback to cached auth tokens (5 min TTL) |
| **Vault** | Secrets Storage | Critical | 99.99% | Cached keys available for 1 hour, alert security team |
| **Monitoring (Datadog)** | Observability | Medium | N/A | Service continues, but visibility reduced. Page ops team. |

---

## SLA and SLO Definitions

### Service Level Agreement (SLA)
- **Availability Target:** 99.9% (max 43 minutes downtime per month)
- **Uptime monitored:** 24/7, measured across all instances
- **Customer-facing impact:** If availability drops below 99.9%, triggers SLA breach credits to customers

### Service Level Objectives (SLO)

| Metric | Target | Measurement | Alert Threshold |
|--------|--------|-------------|-----------------|
| **Availability** | 99.9% | (total requests - errors) / total requests | <99.8% (breach by 0.1%) |
| **Latency (P99)** | 500ms | 99th percentile request time | >750ms (50% above target) |
| **Error Rate** | <0.1% | 4xx + 5xx errors / total requests | >0.15% (50% above target) |
| **Throughput** | 10K RPS min | Requests per second capacity | <9K RPS indicates capacity issue |
| **Payment Success Rate** | 99.5% | Successful vs failed payments | <99.0% indicates fraud blocks or processor issue |

### SLO Rationale
- P99 latency of 500ms: User-acceptable for payment (not instant, but fast)
- Error rate <0.1%: Allows 1 error per 1000 requests, typical for stable system
- Payment success 99.5%: 0.5% covers legitimate declines (expired card, insufficient funds, fraud block)

---

## Monitoring and Alerting

### Monitoring Dashboard
- **URL:** https://datadog.company.com/dashboard/payment-service-main
- **Metrics:** Request latency, error rate, database connection pool, cache hit ratio, Stripe API status
- **Refreshed:** Auto-refresh every 30 seconds
- **Alert Rules:** Defined below

### Key Metrics to Watch

During an incident, immediately check these metrics:

1. **Error Rate:** Check Datadog query: `avg:payment_service.error_rate{environment:prod}` (target: <0.1%)
2. **Response Latency:** Check `avg:payment_service.request_duration_p99{environment:prod}` (target: <500ms)
3. **Database Connections:** Check `avg:postgres.connections{service:payment_service}` (alert if >80% of pool)
4. **Cache Hit Ratio:** Check `avg:redis.hit_rate{service:payment_service}` (should be >80% for performance)
5. **Payment Success Rate:** Custom metric `avg:payment_service.success_rate{environment:prod}` (target: >99.5%)
6. **Message Queue Depth:** Check RabbitMQ console for queue backlog (should be ~0, concerning if >1000)
7. **Stripe API Status:** Check `https://status.stripe.com` for outages
8. **Database Replication Lag:** Check `SELECT max(pg_last_wal_receive_lsn() - '0/0') FROM pg_catalog.pg_replication_slots;` should be <100ms

### Alert Definitions

| Alert Name | Condition | Severity | Action |
|---|---|---|---|
| **High Error Rate** | Error rate >0.5% for 5 min | Critical (P0) | Page on-call, check logs, page platform team if not understood |
| **High Latency P99** | P99 latency >1000ms for 10 min | High (P1) | Page on-call, check database metrics, check Stripe API status |
| **Payment Success Rate Low** | Success rate <98% for 5 min | Critical (P0) | Page on-call + fraud team, check fraud rules, check processor status |
| **Database Connection Pool Full** | Available connections <5 for 2 min | High (P1) | Page on-call + DBA, likely connection leak, restart service |
| **Redis Unavailable** | Redis connection fails for 1 min | High (P1) | Page on-call + platform team, service degrades but continues |
| **Stripe API Timeout** | 3+ timeout errors in 2 min | Critical (P0) | Check Stripe status page, consider switching to fallback processor |
| **Payment Queue Backlog** | RabbitMQ depth >5000 for 10 min | High (P1) | Page on-call, scale up message consumers |
| **Out of Memory** | Memory usage >90% for 5 min | Critical (P0) | Page on-call, likely memory leak, restart pods |

---

## Common Incidents and Troubleshooting

### Incident 1: High Error Rate (Status Code 5xx)

#### Symptoms
- Datadog shows error rate suddenly spike from 0.05% to 5%
- Customers report "Payment failed" errors
- Response times might be normal or slow

#### Likely Causes
1. **Database gone away:** Connection lost to PostgreSQL
2. **External processor timeout:** Stripe API timing out
3. **Out of memory:** Process crash due to memory leak
4. **Unhandled exception:** Code bug causing crashes
5. **Cache failure:** Redis down, cascading failures on cache misses

#### Investigation Steps

**Step 1: Check Service Health (30 seconds)**
```bash
# Check pod restart count
kubectl get pods -n payment -l app=payment-service
# If RESTARTS is high (>0 recent), service is crashing

# Check pod events
kubectl describe pod payment-service-abc123 -n payment
# Look for "CrashLoopBackOff" or OOMKilled messages
```

**Step 2: Check Logs (1 minute)**
```bash
# View last 100 error logs
kubectl logs -n payment deployment/payment-service \
  --tail=100 --timestamps=true | grep -i error

# Search for specific error
kubectl logs -n payment deployment/payment-service | grep -i "connection refused"

# Use log streaming for live monitoring
kubectl logs -n payment deployment/payment-service -f
```

**Step 3: Check Dependencies (2 minutes)**
```bash
# Test database connectivity
kubectl exec -it payment-service-abc123 -n payment -- \
  psql -U payment_user -h postgres-primary.default \
  -d payment_db -c "SELECT 1"
# Should return: 1 (if connection ok)
# If fails: "connection refused" or timeout

# Test Stripe API
curl -H "Authorization: Bearer $STRIPE_API_KEY" \
  https://api.stripe.com/v1/charges?limit=1
# Should return 200 OK with charge data
# If fails: Check status.stripe.com

# Test Redis
kubectl exec -it payment-service-abc123 -n payment -- \
  redis-cli -h redis-cluster.default ping
# Should return: PONG (if connected)
```

**Step 4: Check Resource Usage (1 minute)**
```bash
# Check CPU and memory
kubectl top pods -n payment -l app=payment-service

# If memory >90%: Likely memory leak
# If CPU >95%: Likely hot loop or stuck process

# Get detailed metrics
kubectl describe node <node-name> | grep -A 20 "Allocated resources"
```

#### Resolution Steps

**If database is down:**
1. Contact DBA immediately: `@oncall-dba #payments-oncall`
2. Check if read-replica is available: `psql -h postgres-replica.default -d payment_db`
3. If replica available, consider failover (requires DBA approval)
4. Alternatively, scale down service to reduce connection pressure while DBA fixes
5. Point connection string to replica temporarily (edit ConfigMap)

**If Stripe is down:**
1. Check https://status.stripe.com
2. If Stripe has outage, switch to fallback processor: Scale down Stripe adapter pods, scale up Adyen adapter
3. Notify customers: "We're experiencing payment delays due to processor issues, investigating"
4. Update status page: https://status.company.com

**If service is out of memory:**
1. Restart pods: `kubectl rollout restart deployment/payment-service -n payment`
2. Pods will respawn with clean memory state
3. If memory leak is real, restarts will be temporary. Must fix code.
4. Look for: Large array accumulation, connection leaks, cache unlimited growth

**If logs show unhandled exception:**
1. Note the exception message and line number
2. Immediately create incident ticket with logs attached
3. Contact developer on-call
4. Rollback to previous version if code change is recent: See Rollback section below

#### Escalation Path
- **0-5 min:** Page on-call engineer, troubleshoot
- **5-15 min:** If root cause not identified, page platform team lead
- **15-30 min:** If service still down, page VP of Engineering
- **>30 min:** Page CEO, customer success team to communicate with customers

---

### Incident 2: High Latency (P99 > 1000ms)

#### Symptoms
- P99 latency in Datadog shows spike from 200ms to 2000ms
- Error rate still normal (<0.1%)
- Customers report slow checkout

#### Likely Causes
1. **Slow database query:** N+1 query, missing index, heavy transaction
2. **Stripe API slow:** Their servers responding slowly
3. **Network degradation:** Latency between service and dependency
4. **Resource contention:** Service competing for CPU/memory with other processes
5. **Cache misses:** Cache down or cache key format changed

#### Investigation Steps

**Step 1: Identify slow operation (2 minutes)**
```bash
# Get slow query log from database
psql -U payment_user -h postgres-primary.default -d payment_db
# Run this query to find slowest queries:
SELECT mean_exec_time, calls, query FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;

# Also check active queries:
SELECT pid, usename, state, query, wait_event FROM pg_stat_activity
WHERE state != 'idle' ORDER BY query_start;
```

**Step 2: Check Stripe API latency (1 minute)**
```bash
# Monitor Stripe requests in logs
kubectl logs -n payment deployment/payment-service -f | grep "stripe"

# Check tail latency from Stripe metric
grep "stripe.request_duration" /var/log/payment-service.log | tail -20
```

**Step 3: Check resource bottleneck (1 minute)**
```bash
# CPU and memory
kubectl top pods -n payment -l app=payment-service
# If CPU stuck near 100%, likely hot loop or lock contention

# Check network
# Note: Kubernetes network metrics less obvious, check cloud provider console
```

#### Resolution Steps

**If database slow:**
1. Identify slow query from `pg_stat_statements`
2. Check if query is missing index: `EXPLAIN ANALYZE <slow_query>;`
3. Add index if missing: `CREATE INDEX idx_user_id ON payments(user_id);`
4. If query N+1, implement query batching in code
5. Temporary workaround: Increase statement timeout or cache results

**If Stripe slow:**
1. Check https://status.stripe.com
2. Check Stripe status metrics in Datadog
3. If Stripe issue, no resolution needed (their problem). Notify customers.
4. Consider implementing request timeout and circuit breaker

**If network issue:**
1. Check network path: `kubectl exec -it payment-service-abc123 -n payment -- traceroute api.stripe.com`
2. Check packet loss: `kubectl exec -it payment-service-abc123 -n payment -- ping -c 10 api.stripe.com`
3. Contact platform/network team if issue identified

---

### Incident 3: Service Down / Crash Loop

#### Symptoms
- Service pods stuck in CrashLoopBackOff state
- API returns 503 Service Unavailable
- Datadog shows 0% availability
- Error rate 100% (all requests failing)

#### Likely Causes
1. **Code bug in new deployment:** Uncaught exception on startup
2. **Configuration error:** Invalid environment variable, bad config
3. **Dependency boot failure:** Cannot connect to database on startup
4. **Out of memory (OOMKilled):** Process exceeds memory limit

#### Investigation Steps

**Step 1: Check pod status (30 seconds)**
```bash
# Get pod status
kubectl get pods -n payment -l app=payment-service
# Look for status: CrashLoopBackOff

# Detailed info
kubectl describe pod payment-service-abc123 -n payment
# Look at "Last State" section for exit code
# Exit code 137 = OOMKilled
# Exit code 1 = Application error
```

**Step 2: Check startup logs (1 minute)**
```bash
# Get boot logs
kubectl logs -n payment payment-service-abc123 --previous
# This shows logs from before crash

# Or use stern for real-time logs
stern payment-service -n payment --timestamps=true
```

**Step 3: Check recent deployments (1 minute)**
```bash
# What version is deployed?
kubectl get deployment payment-service -n payment \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
# Output: payment-service:3.2.1

# See deployment history
kubectl rollout history deployment/payment-service -n payment
# Shows all recent deployments

# Check what changed
kubectl rollout history deployment/payment-service -n payment --revision=5
```

#### Resolution Steps (Priority)

**If code bug in new deployment:**
1. **IMMEDIATE:** Check current image version
2. If recently deployed (within last hour), consider rollback
3. Check deployment logs: `kubectl logs -n payment deployment/payment-service`
4. If issue is obvious (NullPointerException, etc.), rollback to previous version
5. See Rollback section below for step-by-step

**If configuration error:**
1. Check ConfigMap: `kubectl get configmap payment-config -n payment -o yaml`
2. Check environment variables: `kubectl exec -it payment-service-abc123 -n payment -- env | grep PAYMENT`
3. Look for missing required variables (DATABASE_URL, STRIPE_API_KEY, etc.)
4. Fix ConfigMap and re-apply: `kubectl apply -f configmap.yaml`
5. Restart pods: `kubectl rollout restart deployment/payment-service -n payment`

**If dependency connection fails:**
1. Check database: `psql -U payment_user -h postgres-primary.default -d payment_db -c "SELECT 1"`
2. Check Redis: `redis-cli -h redis-cluster.default ping`
3. Check Vault (for API keys): `curl -k https://vault.default:8200/v1/sys/health`
4. If dependency is down, wait for it to come up. Service will automatically restart when dependency available.

**If out of memory:**
1. Check pod memory limit: `kubectl describe pod payment-service-abc123 -n payment | grep memory`
2. Increase memory limit (edit deployment YAML): Change `memory: 512Mi` to `memory: 1Gi`
3. Redeploy: `kubectl apply -f payment-service-deployment.yaml`
4. If memory issue persists, likely memory leak in code. Contact developer on-call.

#### Rollback Procedure

If service deployed buggy code, rollback to previous stable version:

```bash
# See deployment history
kubectl rollout history deployment/payment-service -n payment

# Rollback to previous version
kubectl rollout undo deployment/payment-service -n payment

# Or rollback to specific revision
kubectl rollout undo deployment/payment-service -n payment --to-revision=4

# Verify rollback
kubectl get pods -n payment -l app=payment-service
# Pods should be running (not CrashLoopBackOff)

# Check logs for successful startup
kubectl logs -n payment deployment/payment-service --tail=50
```

---

### Incident 4: Database Connectivity Issue

#### Symptoms
- Errors: "connection refused" or "too many connections"
- High latency due to connection timeouts
- Datadog shows database connection pool exhausted

#### Likely Causes
1. **Connection leak:** Application not closing database connections
2. **Database restarted:** Lost all active connections
3. **Network isolated:** Cannot reach database from application
4. **Connection limit reached:** Too many concurrent connections

#### Investigation Steps

```bash
# Check connection pool status from application
kubectl exec -it payment-service-abc123 -n payment -- \
  curl localhost:8080/metrics | grep "db.connections"

# Check database active connections
psql -U payment_user -h postgres-primary.default -d payment_db
SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;

# Check max connections limit
SHOW max_connections;

# Test direct connection from pod
kubectl exec -it payment-service-abc123 -n payment -- \
  psql -U payment_user -h postgres-primary.default -d payment_db -c "SELECT 1"
```

#### Resolution Steps

**If connection leak:**
1. Restart pods to clear connections: `kubectl rollout restart deployment/payment-service -n payment`
2. Monitor connection pool to ensure not filling up again
3. Contact development team to investigate code for proper connection closing
4. Review recent code changes that might cause leak

**If database restarted:**
1. Verify database is up: `psql -U postgres -h postgres-primary.default -c "SELECT 1"`
2. Service will automatically reconnect
3. Check application logs for reconnection messages
4. Perform incident review to understand why database restarted

**If network isolated:**
1. Check network policies: `kubectl get networkpolicies -n payment`
2. Verify DNS resolution: `kubectl exec -it payment-service-abc123 -n payment -- nslookup postgres-primary.default`
3. Check security groups if using AWS
4. Contact platform/network team for network debugging

**If connection limit reached:**
1. Check current limit: `psql -c "SHOW max_connections;"`
2. Increase limit (requires database restart): `ALTER SYSTEM SET max_connections = 300;`
3. Restart database (coordinate with DBA)
4. As temporary fix, restart application pods to free connections

#### Disaster Recovery (DR) Failover

If primary database is completely down:

```bash
# Verify primary is unreachable
psql -U payment_user -h postgres-primary.default -d payment_db -c "SELECT 1"
# Should timeout or refuse connection

# Failover to read replica
# 1. Promote replica to primary (run this on replica):
SELECT pg_promote();

# 2. Update connection string in ConfigMap
kubectl edit configmap payment-config -n payment
# Change DATABASE_URL from postgres-primary.default to postgres-replica.default

# 3. Restart application pods
kubectl rollout restart deployment/payment-service -n payment

# 4. Verify connection works
kubectl exec -it payment-service-abc123 -n payment -- \
  psql -U payment_user -h postgres-replica.default -d payment_db -c "SELECT 1"
```

---

### Incident 5: Message Queue Backlog / Consumer Lag

#### Symptoms
- RabbitMQ console shows queue depth >1000
- Payment confirmations are delayed
- Customers report not receiving email confirmations

#### Likely Causes
1. **Consumer crashed or slow:** Message processor not consuming
2. **Queue destination service down:** Consumer trying to send to down service
3. **Poison message:** Invalid message causes exception, stops consumer

#### Investigation Steps

```bash
# Check RabbitMQ queue depth
kubectl exec -it rabbitmq-0 -n queue -- \
  rabbitmqctl list_queues name messages consumers

# Check consumer status
kubectl get pods -n notification | grep payment-notification-consumer

# Check logs
kubectl logs -n notification payment-notification-consumer-abc123 \
  --tail=100 | grep -i "error\|exception"
```

#### Resolution Steps

**If consumer slow/stuck:**
1. Restart message consumer: `kubectl rollout restart deployment/payment-notification-consumer -n notification`
2. Queue should start draining as consumer processes messages
3. Monitor progress: `kubectl logs -n notification payment-notification-consumer-abc123 -f`

**If poison message:**
1. Identify problematic message in queue (inspect oldest message)
2. Dead-letter it: Move to dead-letter queue for review
3. Consumer will move to next message and continue processing
4. Investigate message format and fix producer code

**If destination service down:**
1. Check notification service status: `kubectl get pods -n notification -l app=email-service`
2. Restart if needed: `kubectl rollout restart deployment/email-service -n notification`
3. Queue will resume processing once destination is up

---

## Diagnostic Commands Reference

### Kubernetes / Pod Management

```bash
# Get all payment service pods and their status
kubectl get pods -n payment -l app=payment-service -o wide

# Get detailed info on specific pod
kubectl describe pod payment-service-abc123 -n payment

# Stream real-time logs from pod
kubectl logs -n payment deployment/payment-service -f

# Execute command in pod (e.g., bash shell)
kubectl exec -it payment-service-abc123 -n payment -- /bin/bash

# Get resource usage (CPU, memory)
kubectl top pods -n payment -l app=payment-service

# Get pod events (crashes, restarts)
kubectl get events -n payment --sort-by='.lastTimestamp'

# Port-forward to access local service
kubectl port-forward -n payment svc/payment-service 8080:8080
# Now can access http://localhost:8080 locally
```

### Database Diagnostics

```bash
# Connect to primary database
psql -U payment_user -h postgres-primary.default -d payment_db

# Check active connections
SELECT pid, usename, application_name, state, query, query_start
FROM pg_stat_activity WHERE state != 'idle';

# Find slow queries
SELECT mean_exec_time, calls, query FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;

# Check connection limit
SHOW max_connections;

# Check current connection count
SELECT count(*) FROM pg_stat_activity;

# Check replication status
SELECT * FROM pg_stat_replication;

# Kill long-running query (if blocking others)
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid != pg_backend_pid();

# Check table size
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

# Analyze query plan
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM payments WHERE status = 'pending';

# Check index usage
SELECT schemaname, tablename, indexname, idx_scan FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### Stripe API Diagnostics

```bash
# Check Stripe status (from curl)
curl -s https://status.stripe.com/api/v2/status.json | jq '.page.status'

# Test Stripe API connectivity from pod
kubectl exec -it payment-service-abc123 -n payment -- \
  curl -H "Authorization: Bearer $STRIPE_API_KEY" \
  https://api.stripe.com/v1/charges?limit=1

# List recent charges (for debugging)
stripe charges list --limit=10 --api-key=$STRIPE_API_KEY

# Retry failed webhook
stripe events resend <event_id> --api-key=$STRIPE_API_KEY
```

### Monitoring Queries (Datadog)

```
# Average error rate for last hour
avg:payment_service.error_rate{environment:prod}.rollup(avg, 3600)

# P99 latency trend
avg:payment_service.request_duration{quantile:0.99,environment:prod}

# Success rate of payments
avg:payment_service.success_rate{environment:prod}

# Redis cache hit ratio
avg:redis.hit_rate{service:payment_service}

# Database connection pool available
avg:postgres.connection_pool.available{service:payment_service}
```

---

## Rollback Procedure

If a bad deployment is causing issues, rollback to previous stable version:

### Step-by-Step Rollback

```bash
# 1. Verify current deployment version
kubectl get deployment payment-service -n payment \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
# Example output: payment-service:3.2.1

# 2. View deployment history
kubectl rollout history deployment/payment-service -n payment
# Shows: REVISION CHANGE-CAUSE
#        5        Rollout: payment-service:3.2.1
#        4        Rollout: payment-service:3.2.0
#        3        Rollout: payment-service:3.1.9

# 3. View what changed in specific revision
kubectl rollout history deployment/payment-service -n payment --revision=4
# Shows what was in that revision

# 4. Perform rollback to previous version
kubectl rollout undo deployment/payment-service -n payment

# OR rollback to specific revision
kubectl rollout undo deployment/payment-service -n payment --to-revision=4

# 5. Monitor rollback progress
kubectl rollout status deployment/payment-service -n payment
# Should show "deployment "payment-service" successfully rolled out"

# 6. Verify pods are healthy
kubectl get pods -n payment -l app=payment-service
# All pods should be Running and Ready (1/1 or 2/2 etc.)

# 7. Check logs confirm successful startup
kubectl logs -n payment deployment/payment-service --tail=30 --timestamps=true

# 8. Verify metrics recovering
# Check Datadog for error rate, latency returning to normal

# 9. Communicate with team
# Post in #payments-oncall: "Rolled back to version 3.2.0 due to [issue]. Investigating root cause."
```

### Post-Rollback Actions
1. Create incident ticket with timeline and impact
2. Schedule incident review for tomorrow (post-incident)
3. Contact development team about what went wrong
4. Implement fix and test thoroughly in staging
5. Re-deploy with fix after incident review approval

---

## Escalation Matrix

### Severity Levels and Response

| Severity | Error Rate | Latency | Response | Escalation |
|---|---|---|---|---|
| **P0 - Critical** | >5% or any down | Any | <5 min response, page immediately | On-call + Manager + Director |
| **P1 - High** | 1-5% | P99 >1s | <15 min response, page within 5 min | On-call + Manager |
| **P2 - Medium** | 0.1-1% | P99 500ms-1s | <30 min response | On-call engineer |
| **P3 - Low** | <0.1% | P99 <500ms | <2 hour response | Assigned engineer |

### Escalation Chain

**0-5 minutes (Page on-call):**
- Slack: `@payment-service-oncall`
- PagerDuty: Trigger P0 alert
- SMS to on-call if page not acknowledged in 2 min

**5-15 minutes (If unresolved):**
- Page team lead: `@payment-platform-lead`
- Page platform team: Might need infrastructure help
- Update #incidents Slack channel with status

**15-30 minutes (If still unresolved):**
- Page VP of Engineering
- Customer success team: Prepare customer communication
- Consider activating war room (zoom call #payment-incident-war-room)

**30+ minutes (If service still down):**
- Page CEO
- Customer success: Begin notifying customers
- Schedule post-incident review

---

## Post-Incident Review Template

After incident is resolved, fill out this template:

### Incident Summary
- **Incident ID:** INC-2024-001
- **Service:** Payment Processing Service
- **Duration:** 23 minutes (14:32 UTC - 14:55 UTC)
- **Impact:** ~15,000 failed transactions, $7,500 revenue impact
- **Root Cause:** Database connection pool exhausted due to connection leak

### Incident Timeline

| Time | Event | Owner |
|------|-------|-------|
| 14:32 | Error rate spike detected by Datadog | Monitoring system |
| 14:34 | On-call engineer paged | PagerDuty |
| 14:36 | Investigation started, identified connection pool issue | alice@company.com |
| 14:40 | Restarted payment service pods | alice@company.com |
| 14:42 | Service recovered, error rate returned to baseline | alice@company.com |
| 14:55 | Created incident ticket and war room closed | alice@company.com |

### Root Cause Analysis
The payment service was leaking database connections. Each payment request was creating a connection but not closing it properly. After ~500 requests, the connection pool (configured for 100 connections) was exhausted, causing all subsequent requests to fail with "connection timeout" error.

This was caused by a code change merged 2 days ago (commit abc123). The change modified the payment processor adapter but forgot to close the database connection in the error path.

### Impact
- 15,037 failed payment attempts (0.3% of daily volume)
- ~$7,500 in lost revenue (calculated at $0.50 average transaction value)
- 1,200 customer support tickets filed
- 23 minutes of unavailability (SLA violation)

### Action Items for Prevention

| Action | Owner | Priority | Deadline |
|--------|-------|----------|----------|
| Fix database connection leak in commit abc123 | bob@company.com | Critical | Before next deployment |
| Add unit test for connection cleanup | bob@company.com | High | Before merge |
| Implement connection pool monitoring alerts | charlie@company.com | High | End of week |
| Code review process audit (how did this slip?) | alice@company.com | Medium | 1 week |
| Add connection leak detection to integration tests | david@company.com | Medium | 2 weeks |

### Follow-Up Meeting
- Scheduled for March 15, 10:00 AM
- Attendees: Payment team, platform team, QA team
- Goal: Discuss prevention, improve on-call runbook
