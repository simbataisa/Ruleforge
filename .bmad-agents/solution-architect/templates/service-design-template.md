# Microservice Design Template

**Service Name:** [Service Name]
**Service Owner (Team):** [Team Name]
**Domain/Bounded Context:** [DDD Domain Context]
**Created:** [YYYY-MM-DD]
**Last Updated:** [YYYY-MM-DD]
**Status:** Proposed | Approved | Building | Live | Deprecated
**Design Document ID:** [ARCH-SVC-XXXX]

---

## Service Metadata

| Property | Value |
|----------|-------|
| Service Name | [Canonical name, e.g., order-service] |
| Service Type | Core domain service \| Supporting service \| Infrastructure service |
| Domain Ownership | [Business domain this service belongs to] |
| Team Owner | [Team name, team lead, Slack channel] |
| Communication Channel | [#team-channel on Slack, weekly sync time] |
| On-Call Rotation | [PagerDuty service name, escalation] |
| Deployment Frequency | [Weekly / Bi-weekly / On-demand] |
| Go-Live Target | [Date or milestone] |
| Integration Points | [Number of synchronous + asynchronous consumers] |

---

## Service Purpose

**One-line purpose statement:**
[Concise statement of what this service does, who it serves, what value it provides]

Example: "Order Service manages the complete lifecycle of customer orders from creation through fulfillment and returns."

**What this service IS responsible for:**
- [Capability 1]
- [Capability 2]
- [Capability 3]
- [Capability 4: e.g., validating order business rules]

**What this service IS NOT responsible for:**
- [Domain that belongs to another service, with link to that service]
- [Domain that is still unclear — escalate to architecture review]
- [Planned future capability that is out of current scope]

**Service boundaries (anti-corruption layer):**
If adapting external system contracts, document the translation layer that prevents your domain model from being polluted by external schemas.

---

## Domain Model

**Aggregates and key entities:**

| Entity | Description | Owned By | Responsibility |
|--------|-------------|----------|-----------------|
| [Aggregate Root Name] | [Description] | [This service] | [What behavior does it encapsulate?] |
| [Entity Name] | [Description] | [This service] | [Validation rules, invariants] |
| [Value Object] | [Immutable value, e.g., Money, DateRange] | [This service] | [Encapsulate related attributes] |
| [External Aggregate Reference] | [Reference to aggregate owned by another service] | [Other Service] | [How is it referenced? By ID only] |

**Example table (Order Service):**

| Entity | Description | Owned By | Responsibility |
|--------|-------------|----------|-----------------|
| Order (Aggregate Root) | Represents a customer order | Order Service | Order state transitions, line item validation |
| LineItem | Product and quantity for one line | Order Service | Price calculation, discount application |
| DeliveryAddress | Shipping destination | Order Service | Address validation, format normalization |
| Customer ID (Reference) | Link to customer in Customer Service | Customer Service | By ID only; never duplicate customer data |
| Payment (Aggregate Root) | Payment transaction record | Payment Service | By event subscription; never queried directly |

**Invariants and constraints:**
- [Business rule that must always be true, e.g., "An order must have at least one line item"]
- [Constraint on state transitions, e.g., "Order can only be cancelled if status is Pending or Confirmed"]
- [Data validation, e.g., "Line item quantity must be positive integer between 1 and 9999"]

---

## Service Boundaries

### Publishes (Outbound Events)

This service emits domain events that other services consume. Events are the primary contract between services.

| Event Name | Schema / Key Fields | Consumers | Frequency | Retention | Notes |
|------------|-------------------|-----------|-----------|-----------|-------|
| OrderCreated | orderId, customerId, totalAmount, createdAt | Billing Service, Inventory Service, Notification Service | On order creation | 30 days | Idempotent; consumer must handle duplicates |
| OrderConfirmed | orderId, confirmedAt, estimatedDeliveryDate | Fulfillment Service, Notification Service | When payment captured | 30 days | Signals readiness for fulfillment |
| OrderShipped | orderId, trackingNumber, carrierCode, shippedAt | Customer Service, Analytics | When fulfillment packs item | 90 days | Provides shipment visibility |
| OrderCancelled | orderId, reason, cancelledAt, refundAmount | Payment Service, Inventory Service | When customer/system cancels | 30 days | Triggers compensation logic |

**Event schema example (OrderCreated):**
```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "aggregateId": "order-id",
  "aggregateType": "Order",
  "timestamp": "ISO8601",
  "version": 1,
  "payload": {
    "orderId": "string",
    "customerId": "string",
    "totalAmount": {"amount": "decimal", "currency": "string"},
    "lineItems": [{"productId": "string", "quantity": "int", "unitPrice": "decimal"}],
    "shippingAddress": {"street": "string", "city": "string", "postalCode": "string", "country": "string"},
    "createdAt": "ISO8601"
  }
}
```

### Consumes (Inbound Events)

This service subscribes to events published by other services. Define why the event is consumed and what action is triggered.

| Event Name | Producer Service | Why Consumed | Action Taken | Failure Handling |
|------------|------------------|--------------|--------------|-----------------|
| CustomerCreated | Customer Service | Cache customer profile locally | Store customer metadata | Retry up to 5 times, then DLQ |
| PaymentCaptured | Payment Service | Confirm payment success before fulfillment | Transition order to Confirmed state | Publish OrderPaymentPending event, manual reconciliation |
| InventoryDeducted | Inventory Service | Confirm items allocated | Update order status, trigger fulfillment | Publish OrderInventoryFailed event, reverse reservation |
| InventoryReserved | Inventory Service | Provisional hold on inventory | Update order status to Awaiting Payment | Timeout after 15 minutes if payment not received |

### Synchronous Dependencies

This service calls other services synchronously for real-time data or operations. Define SLA and failure modes.

| Dependent Service | Endpoint | Purpose | Protocol | Timeout | Retry Policy | SLA | Fallback |
|-------------------|----------|---------|----------|---------|--------------|-----|----------|
| Customer Service | GET /customers/{id} | Look up customer profile | REST/HTTP | 2s | Exponential backoff, max 3 | 99.9% availability | Return cached profile from last 24h |
| Inventory Service | POST /reservations | Reserve items for order | REST/HTTP | 3s | Exponential backoff, max 2 | 99.5% availability | Fail order with "Inventory unavailable" |
| Payment Service | POST /transactions/authorize | Authorise payment | REST/HTTP | 5s | No retry (payment idempotency key) | 99.99% availability | Return 503 to client, allow retry |
| Notification Service | POST /notifications/send | Send confirmation email | REST/HTTP | 1s | Fire-and-forget, async fallback to queue | 95% availability | Publish to event queue for async retry |

---

## API Surface

**Service protocol(s):** REST, gRPC, GraphQL, Event-driven (select applicable)

**Authentication method:** OAuth2 bearer token | API key | mTLS | Service account | None (internal only)

**High-level endpoint list:**

```
POST   /orders                          Create new order
GET    /orders/{orderId}                Get order details
GET    /orders?customerId={id}&limit=10 List customer orders with pagination
PUT    /orders/{orderId}                Update order (address change, etc.)
DELETE /orders/{orderId}                Cancel order
PATCH  /orders/{orderId}/status         Transition order state

POST   /orders/{orderId}/payments       Record payment for order
GET    /orders/{orderId}/shipments      Get shipment details

POST   /orders/batch-cancel             Bulk cancel orders (admin)
```

**API versioning strategy:** URL path (/v1/, /v2/) | Header (Accept: application/vnd.api+v2+json) | Query param (?apiVersion=v2)

**Rate limiting:** [Requests per second per client, burst limits]
Example: 100 RPS sustained, 200 RPS burst (30-second window)

**API documentation:** [Link to OpenAPI/Swagger, GraphQL schema, gRPC proto files]

---

## Data Model

### Primary Datastore

| Property | Value |
|----------|-------|
| Technology | PostgreSQL / MySQL / MongoDB / DynamoDB / etc. |
| Multi-region | Yes / No |
| Managed service | Yes (AWS RDS) / No (self-hosted) |
| Backup strategy | Daily incremental, weekly full, replicated to secondary region |

**Rationale for choice:**
[Why this technology? Relational for strict consistency? Document for flexibility? Graph for relationships? Key-value for speed?]

### Schema Overview

**Core tables:**

```sql
-- Orders table: Main aggregate root
CREATE TABLE orders (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  total_amount DECIMAL(12, 2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'USD',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(100),
  source VARCHAR(50), -- web, mobile, api
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  INDEX idx_customer_id (customer_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);

-- Order line items
CREATE TABLE order_line_items (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  product_id UUID NOT NULL, -- reference only, no FK
  quantity INT NOT NULL CHECK (quantity > 0),
  unit_price DECIMAL(12, 2) NOT NULL,
  discount_amount DECIMAL(12, 2) DEFAULT 0,
  line_total DECIMAL(12, 2) GENERATED AS (quantity * unit_price - discount_amount),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  INDEX idx_order_id (order_id)
);

-- Audit / event log
CREATE TABLE order_events (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  event_payload JSONB NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_order_id (order_id),
  INDEX idx_occurred_at (occurred_at)
);
```

**Data ownership rules:**
- This service OWNS orders, line items, order status, delivery addresses.
- This service does NOT own customer data (read-only reference); customer data owned by Customer Service.
- This service does NOT own payment details; payment data owned by Payment Service.
- This service does NOT own inventory; inventory data owned by Inventory Service.

**PII (Personally Identifiable Information) fields:**
- customer_id: Not PII (UUID reference), but links to PII.
- shippingAddress: Classified as PII; encrypted at rest, masked in logs.
- customerEmail: If stored, classified as PII; never logged.
- paymentMethod: Sensitive data; never stored, always delegated to Payment Service.

**Data retention policy:**
- Active orders: Indefinite (owned by this service).
- Cancelled orders: Retained for 7 years (legal compliance for order history).
- Order audit events: Retained for 7 years.
- Customer references: Immediately soft-deleted if customer deleted (cascade from Customer Service event).

---

## Technology Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| Language | Python 3.11 | 3.11+ | Team expertise, readability, extensive libraries (FastAPI, SQLAlchemy) |
| Web Framework | FastAPI | 0.95+ | Async support, automatic API documentation, type hints, fast performance |
| ORM | SQLAlchemy | 2.0+ | Flexible query API, raw SQL escape hatch, multi-DB support |
| Database | PostgreSQL | 14+ | ACID compliance, JSON support, strong consistency, proven at scale |
| Cache | Redis | 7.0+ | Sub-millisecond latency, session storage, distributed locks |
| Message Broker | Kafka | 3.4+ | Durable event log, consumer groups, topic replay, audit trail |
| Logging | Datadog / ELK | Latest | Centralized logs, structured JSON, trace correlation |
| Monitoring | Prometheus + Grafana | Latest | Metrics, dashboards, alerting, industry standard |
| Container Runtime | Docker | 24+ | Container orchestration, easy deployment, consistent environments |
| IaC | Terraform / Helm | Latest | Infrastructure as code, GitOps, reproducible deployments |

**Dependency tree:**
- FastAPI → Starlette (ASGI framework)
- SQLAlchemy → psycopg2 (PostgreSQL driver)
- Kafka consumer group → confluent-kafka (C extension for performance)

---

## Non-Functional Requirements

### Performance

| Metric | Target | Measurement | Notes |
|--------|--------|-------------|-------|
| API P99 latency | < 200ms | End-to-end at BFF | Excludes network latency to client |
| API P95 latency | < 100ms | Measured at API service | After warm cache |
| Cache hit ratio | > 80% | Daily metric | For customer profile lookups |
| Database query P99 | < 50ms | Slow query log | Queries on indexed fields |
| Event publish latency | < 100ms | Measured at broker | Async, non-blocking |

### Throughput

| Metric | Target | Sustained | Burst | Notes |
|--------|--------|-----------|-------|-------|
| Orders created / sec | 100 RPS | Peak hours 8am-2pm | 200 RPS for 5 min | Peak times during sales campaigns |
| Queries / sec | 500 RPS | Read-heavy workload | 1000 RPS | Customer viewing order history |
| Events published / sec | 50 events/sec | Steady state | 100 events/sec | Each order generates 3-5 events |

### Availability

| SLA | Target | Monthly Downtime | Notes |
|-----|--------|------------------|-------|
| Service availability | 99.9% | 43 minutes | Excludes planned maintenance |
| API endpoint availability | 99.95% | 22 minutes | Critical customer-facing endpoint |
| Database availability | 99.99% | 4 minutes | With replication and failover |

### Data Resilience

| Metric | Target | Notes |
|--------|--------|-------|
| Recovery Point Objective (RPO) | < 1 hour | Maximum data loss acceptable after failure |
| Recovery Time Objective (RTO) | < 15 minutes | Time to restore service to full operation |
| Backup frequency | Every 6 hours | Incremental; daily full backups |
| Point-in-time recovery | 7 days | Ability to restore to any point in last 7 days |

---

## Scalability Strategy

**Horizontal scaling:** YES — All service instances are stateless and interchangeable.

**Scaling mechanism:**
- Kubernetes HPA (Horizontal Pod Autoscaler) monitors CPU and memory.
- Scale-out trigger: CPU > 70% or memory > 80%.
- Scale-in trigger: CPU < 30% for 5 minutes.
- Min replicas: 2 (for HA), Max replicas: 20.

**Stateless design confirmation:**
- User session state: Stored in Redis, not in service memory.
- Request correlation: Passed via HTTP headers (trace ID).
- Caching: Cache-aside pattern; service tolerates cache misses.
- Database connections: Pooled via PgBouncer, not embedded in process.

**Session handling (if applicable):**
- Sessions stored in Redis with TTL (30 minutes of inactivity).
- JWT token in Authorization header; Redis used only for session metadata.
- Cross-region session support: Redis replicated to secondary region.

**Database scaling:**
- Write master in primary region; read replicas in secondary regions.
- Replication lag monitored; read-after-write consistency enforced for critical reads.
- Connection pooling: PgBouncer max 100 connections per replica.
- Query optimization: Indexed scans < 1000 rows; table scans trigger alert.

---

## Security Design

### Authentication and Authorization

**Auth method:** OAuth2 bearer token with JWT claims.

**Token issuer:** Corporate identity provider (Okta / Azure AD / custom Keycloak).

**Authorization model:** Role-based access control (RBAC).

| Role | Permissions | Notes |
|------|-----------|-------|
| customer | view own orders, create orders | Scoped to their customer_id |
| merchant | view all orders, update order status | Admin role for fulfillment team |
| support | view all orders, add notes, cancel with reason | Read-only except cancellation |
| admin | full access | Bypass all authorization |

**Claim validation:**
- JWT signature verified with IdP public key (cached, refreshed daily).
- aud (audience) claim validated = service_id.
- exp (expiration) claim checked; reject if expired.
- Scope claim parsed; permission mapped to operation.

### Data Encryption

**At rest:**
- Database: TDE (Transparent Data Encryption) enabled.
- Backups: AES-256 encryption, separate KMS key per environment.
- Redis cache: In-transit encryption only (cache is volatile).

**In transit:**
- All external HTTP calls: HTTPS with TLS 1.3.
- Internal service-to-service: mTLS with service certificates rotated every 90 days.
- Database connection: TLS to PostgreSQL (require SSL mode).

**Sensitive fields:**
- PII fields: Consider field-level encryption for compliance (GDPR, CCPA).
- Payment data: Never stored; always handled by Payment Service via reference.

### Secret Management

**Secrets stored in:** HashiCorp Vault / AWS Secrets Manager / Azure Key Vault.

**Rotation policy:**
- Database credentials: Rotated every 90 days.
- API keys for external services: Rotated every 180 days.
- TLS certificates: Auto-renewed 30 days before expiry.

**Secrets accessed:**
- Via sidecar agent (e.g., Consul template) or SDK.
- Secrets injected as environment variables at container startup.
- Never commit secrets to Git; use .gitignore and pre-commit hooks.

### Network Policy

**Ingress:**
- API service only accepts from API Gateway / Load Balancer.
- No public IP exposure; all via service mesh or ingress controller.

**Egress:**
- Outbound calls to external services: Via proxy, traffic logged.
- Calls to internal services: Via service mesh for mTLS.
- Database: Private network only, no internet routing.

---

## Observability Design

### Metrics

**Key business metrics:**

| Metric | Frequency | Purpose |
|--------|-----------|---------|
| orders_created_total (counter) | Per request | Track order creation rate |
| order_value_sum (gauge) | Per order | Revenue tracking |
| order_fulfillment_latency_seconds (histogram) | Per order | Fulfillment speed SLA |
| order_cancellation_rate (gauge) | Per minute | Churn analysis |

**Key technical metrics:**

| Metric | Frequency | Purpose |
|--------|-----------|---------|
| http_request_duration_seconds (histogram) | Per request | API latency distribution (P50, P95, P99) |
| http_requests_total (counter) | Per request | Request rate by endpoint, method, status |
| database_connection_pool_active (gauge) | Per minute | Connection pool utilization |
| cache_hit_ratio (gauge) | Per minute | Cache effectiveness |
| kafka_consumer_lag_seconds (gauge) | Per minute | Event processing delay |

**Alerting thresholds:**

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| HighLatency | P99 latency > 500ms for 5 min | Warning | Page on-call; investigate slow queries |
| HighErrorRate | Error rate > 5% for 2 min | Critical | Page on-call; rollback if recent deploy |
| LowCacheHitRatio | Cache hit < 60% for 10 min | Warning | Investigate; check Redis connectivity |
| HighDatabaseConnections | Active connections > 90 | Warning | Check for connection leaks; consider scaling |

### Logging

**Log aggregation:** Datadog / ELK Stack / Splunk.

**Log level defaults:** INFO in production, DEBUG in development.

**Key log events:**

| Event | Level | Fields | Purpose |
|-------|-------|--------|---------|
| Order created | INFO | orderId, customerId, amount, timestamp | Audit trail |
| Order status changed | INFO | orderId, oldStatus, newStatus, reason, timestamp | State transitions |
| External API call failed | WARN | serviceName, endpoint, statusCode, retryAttempt, latency | Dependency issues |
| Validation error | WARN | orderId, fieldName, error, timestamp | Data quality |
| Access denied (authz failure) | WARN | userId, operation, resource, reason | Security events |

**Structured logging (JSON):**
```json
{
  "timestamp": "2024-03-14T10:30:45Z",
  "service": "order-service",
  "level": "INFO",
  "traceId": "a4c1a6b3-c5d9-4e2f-9a1b-d8f7c3e5a9b2",
  "spanId": "b7f9d2e4-a1c3-4e6f-8b3d-c5a7e9f1b3d5",
  "event": "OrderCreated",
  "orderId": "ORD-2024-001",
  "customerId": "CUST-5678",
  "amount": 129.99,
  "currency": "USD"
}
```

### Distributed Tracing

**Trace propagation:** W3C Trace Context standard (traceparent header).

**Key traces to instrument:**
- Order creation end-to-end (from API to database to event publish).
- External service calls (Customer Service, Payment Service, Inventory Service).
- Database queries (identify N+1 queries, slow queries).

**Sampling:** 100% for errors, 10% for normal requests (adjust based on volume).

---

## Deployment Model

**Containerization:** YES — Docker image, multi-stage build, <500MB final image size.

**Container registry:** Docker Hub / AWS ECR / Azure ACR / Private registry.

**Orchestration platform:** Kubernetes (EKS / AKS / self-managed).

### Kubernetes Resources

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
spec:
  replicas: 3  # HA baseline
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
        version: v1.2.3
    spec:
      containers:
      - name: order-service
        image: docker.io/company/order-service:1.2.3
        ports:
        - containerPort: 8080 name: http
        - containerPort: 9090 name: metrics
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: order-service-secrets
              key: database-url
        resources:
          requests:
            cpu: "100m"        # Minimum CPU
            memory: "256Mi"    # Minimum memory
          limits:
            cpu: "500m"        # Maximum CPU
            memory: "512Mi"    # Maximum memory
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        securityContext:
          runAsNonRoot: true
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
```

### Health Check Endpoints

| Endpoint | Method | Purpose | Response |
|----------|--------|---------|----------|
| /health/live | GET | Kubernetes liveness probe | 200 OK if process alive |
| /health/ready | GET | Kubernetes readiness probe | 200 OK if ready to accept traffic (DB connected, cache healthy) |
| /metrics | GET | Prometheus metrics scrape endpoint | Prometheus format text |

### Deployment Strategy

**Strategy:** Rolling deployment with 25% max surge.

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1           # One extra pod during update
      maxUnavailable: 0     # No pods offline (HA during deploy)
```

**Deployment process:**
1. Build Docker image, push to registry.
2. Update Kubernetes Deployment manifest with new image tag.
3. kubectl apply (or GitOps tool like ArgoCD).
4. Kubernetes rolls out: new pod started, waits for readiness, old pod drained.
5. Health checks validate each new pod before removing old pod.
6. Deployment complete when all pods are new version.

**Rollback procedure:** `kubectl rollout undo deployment/order-service -n production`

---

## Migration / Transition Plan

(Complete this section if replacing an existing service or migrating from monolith.)

**Current state:** [Describe existing order handling system]

**Target state:** This new microservice.

**Migration phases:**

**Phase 1: Strangler Fig (Weeks 1-4)**
- Deploy new service in parallel.
- Route 10% of new order requests to new service (canary).
- New service reads from old database (read-replica).
- Monitor error rates, latency, data consistency.

**Phase 2: Gradual Traffic Shift (Weeks 5-8)**
- Increase traffic to 25%, then 50%, then 100%.
- Validate all orders in new service are correct (compare with old system).
- Run parallel comparison tests; catch discrepancies.

**Phase 3: Data Migration (Weeks 9-10)**
- Migrate historical orders to new database (backfill).
- Validate row counts, totals, key fields match.
- Enable dual-write: write to both old and new databases.

**Phase 4: Cutover (Week 11)**
- Stop accepting new orders in old system.
- Final data sync; verify consistency.
- Redirect all reads to new service.
- Monitor for issues; rollback plan ready.

**Phase 5: Deprecation (Week 12+)**
- Keep old system read-only for 30 days (safety).
- Archive old service infrastructure.
- Update runbooks, documentation.

**Rollback criteria:**
- Data loss or corruption detected.
- Error rate > 5% sustained for > 5 minutes.
- P99 latency degradation > 50%.
- Availability < 99%.

---

## Open Questions and Risks

### Open Questions

1. **Reporting requirements:** Do analytics / reporting teams need real-time order data, or can reports be run on read replicas with 1-hour lag?
2. **Bulk operations:** Will there be bulk order import from partners? Does current API handle CSV/batch endpoints?
3. **Order modification:** Can customers modify orders after placement? Only address, or also line items?
4. **Tax calculation:** Is tax calculated in this service or delegated to Tax Service?
5. **Inventory reservation TTL:** How long should inventory be reserved pending payment? 15 min? 1 hour?

### Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|-----------|
| Database replication lag causes stale reads | Data inconsistency shown to users | Medium | Implement read-after-write consistency; document eventual consistency in UX |
| Kafka consumer lag grows (event processing slow) | Order fulfillment delays | Medium | Monitor consumer lag; auto-scale worker service; circuit break if lag > 10 min |
| External service (Payment) timeout blocks order creation | User sees failures; support escalation | Medium | Implement timeout + circuit breaker; queue order for async processing; retry with backoff |
| Redis cache corruption or data loss | Session loss; users logged out; performance degradation | Low | Redis cluster with replication; AOF persistence; point-in-time restore; alert on eviction rate |
| Compliance audit finds PII not encrypted | Regulatory fine | Low | Encrypt sensitive fields at rest; audit logs encrypted; comply with GDPR/CCPA; document data handling |

---

## Success Criteria

- [ ] Service deployed to production with 99.9% availability SLA.
- [ ] P99 API latency < 200ms under peak load (100 RPS).
- [ ] All orders migrated from legacy system with zero data loss.
- [ ] Team trained on runbooks and pager on-call rotation established.
- [ ] Security audit passed; PII handling compliance verified.
- [ ] Event contracts documented and published to event catalogue.
- [ ] Auto-scaling validated under load test.
- [ ] Disaster recovery tested; RTO < 15 min confirmed.

---

## Approval Sign-Off

| Role | Name | Date | Approval |
|------|------|------|----------|
| Solution Architect | | | |
| Enterprise Architect | | | |
| Security Architect | | | |
| Team Tech Lead | | | |
| Product Owner | | | |

