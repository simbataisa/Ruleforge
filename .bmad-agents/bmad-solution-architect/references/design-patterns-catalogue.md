# Enterprise Software Design Patterns Catalogue

**Last Updated:** [YYYY-MM-DD]
**Maintained By:** Architecture Team
**Version:** 1.0

This document is a reference catalogue of proven design patterns used in enterprise software architecture. It serves as a guide for solution architects and senior engineers when designing systems and making architectural decisions.

---

## Table of Contents

1. [Integration Patterns](#integration-patterns)
2. [Microservices Patterns](#microservices-patterns)
3. [Data Patterns](#data-patterns)
4. [Resilience Patterns](#resilience-patterns)

---

## Integration Patterns

### Backend for Frontend (BFF)

**Intent:** Provide a tailored API layer between frontend clients and backend services to optimise data and interaction patterns for different client types (web, mobile, third-party).

**Problem:**
- Multiple frontend clients have different API requirements (web needs aggregated data; mobile needs minimal payload; third-party needs raw data).
- Exposing all backend services directly to clients creates tight coupling and security exposure.
- Clients must orchestrate multiple service calls, increasing latency and complexity.

**Solution:**
Create dedicated backend gateway services, one per client type or consuming application. Each BFF:
- Aggregates data from multiple backend services.
- Transforms API responses to match client expectations.
- Implements client-specific authentication and session management.
- Applies client-specific business logic (e.g., currency conversion for mobile app in specific region).

**Structure:**
```
Web Client (React)    →   BFF (Node.js)    →   {Order Service, Payment Service, Inventory Service}
Mobile Client (iOS)   →   Mobile BFF        →   {Order Service, Payment Service}
Third-party API       →   Partner BFF       →   {Public Order Service, Analytics}
```

**When to use:**
- Multiple distinct client types with different API contracts.
- Clients span different trust boundaries or security models.
- Client-specific authentication (OAuth for web, API key for third-party).
- Heavy aggregation needed (reducing client chattiness).

**When NOT to use:**
- Single client type only (overhead not justified).
- Minimal service aggregation needed (direct API calls simpler).
- Clients highly homogeneous in requirements.

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Loose coupling | Clients independent of backend | Additional layer to maintain |
| Optimised APIs | Each client gets perfect contract | More services to deploy |
| Security | Authentication layer isolates backends | Increased complexity |
| Latency | Aggregation reduces client round-trips | Additional hop through BFF |

**Related patterns:** API Gateway, Strangler Fig, Anti-Corruption Layer.

**Example implementation (Node.js BFF for web client):**
```javascript
app.get('/api/v1/orders/:id', async (req, res) => {
  const orderId = req.params.id;

  // Aggregate from multiple services
  const [order, payment, shipment] = await Promise.all([
    orderService.getOrder(orderId),
    paymentService.getPayment(orderId),
    shipmentService.getShipment(orderId)
  ]);

  // Transform to web client contract
  res.json({
    id: order.id,
    items: order.items.map(item => ({...item})),
    total: order.total,
    paymentStatus: payment.status,
    tracking: shipment.trackingNumber
  });
});
```

---

### API Gateway

**Intent:** Provide a single entry point for all client requests; handle cross-cutting concerns (authentication, rate limiting, request routing, response transformation) centrally.

**Problem:**
- Direct client access to multiple backend services creates security and operational challenges.
- Cross-cutting concerns (auth, rate limiting, logging) duplicated across services.
- Backend service addresses exposed to clients; API changes require client updates.
- No central place to implement request/response policies.

**Solution:**
Deploy a gateway service that sits before all backend services. Gateway responsibilities:
- Route requests to correct backend based on path, method, headers.
- Enforce authentication and rate limiting.
- Log all requests for audit and debugging.
- Transform requests and responses (e.g., API versioning).
- Handle request/response compression.

**Structure:**
```
Client → API Gateway → {Service A, Service B, Service C}
         (central policies)
```

**Responsibilities of API Gateway:**

| Responsibility | Example |
|---|---|
| Request routing | Route /orders/* → Order Service, /payments/* → Payment Service |
| Authentication | Validate JWT, extract user claims |
| Rate limiting | 100 requests/sec per client, burst 200 |
| Request validation | Ensure Content-Type is application/json |
| Response transformation | Convert XML to JSON, add CORS headers |
| Load balancing | Round-robin across backend service instances |
| Circuit breaking | Fail-fast if backend service is down |
| Logging and monitoring | Audit trail, metrics for all requests |

**When to use:**
- Multiple backend services need unified entry point.
- Central policies must be enforced (auth, rate limiting, logging).
- API versioning needed.
- Traffic shaping or request throttling required.

**When NOT to use (prefer direct service calls if):**
- Single monolithic backend service.
- Extremely low latency critical (gateway adds hop).
- Simple systems with minimal cross-cutting concerns.

**API Gateway vs. BFF comparison:**

| Aspect | API Gateway | BFF |
|--------|---|---|
| Purpose | Central entry point, cross-cutting concerns | Client-specific data aggregation |
| Scope | All requests | Requests from specific client type |
| Responsibility | Routing, auth, rate limit | Transformation, aggregation, client logic |
| Number deployed | 1 (shared) | 1-N (per client type) |
| Use together? | YES. Gateway first, then route to BFFs. | |

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Single entry point | Centralised control | Single point of failure |
| Cross-cutting concerns | Consistent policy enforcement | Gateway complexity |
| Security | Auth enforced once | Gateway must be secure (attacker target) |
| Flexibility | Easy to add services | Tight coupling to gateway impl |

**Popular implementations:** Nginx, Kong, AWS API Gateway, Ambassador, Envoy.

**Related patterns:** BFF, Service Mesh, Load Balancer.

---

### Strangler Fig

**Intent:** Gradually replace a monolithic legacy system with a new microservices architecture by deploying new services in parallel and incrementally redirecting traffic until legacy system is fully decommissioned.

**Problem:**
- Large monolithic system must be modernised; rewriting entirely is risky and expensive.
- Cannot halt feature development while rewriting.
- Need to validate new architecture works correctly before full cutover.

**Solution:**
1. Deploy new services alongside legacy system.
2. Route a small portion of traffic to new services (e.g., 5% canary).
3. Monitor for errors and data consistency issues.
4. Gradually increase traffic (5% → 25% → 50% → 100%).
5. Once all traffic on new services, decommission legacy system.

**Phases:**

**Phase 1: Parallel Deployment** (Weeks 1-4)
- New services deployed; legacy system unchanged.
- Router/gateway routes subset of traffic to new services.
- 90% traffic → Legacy, 10% traffic → New.
- Monitor errors, latency, data validation.

**Phase 2: Gradual Traffic Shift** (Weeks 5-8)
- Increase percentage incrementally (25%, 50%, 75%).
- Run shadow testing (duplicate all reads to both systems, compare results).
- Identify and fix discrepancies in new system.

**Phase 3: Data Migration** (Weeks 9-10)
- If needed, migrate historical data from legacy to new.
- Dual-write mode: write to both systems for 1-2 weeks.
- Verify new system read matches legacy system read.

**Phase 4: Complete Cutover** (Week 11)
- Route 100% traffic to new services.
- Monitor for issues; rollback plan ready.
- Disable writes to legacy system (read-only mode).

**Phase 5: Legacy Decommission** (Week 12+)
- Keep legacy system read-only for 30 days (safety valve).
- Archive data.
- Shut down legacy infrastructure.

**Structure:**

```
Strangler Pattern Timeline:
Week 1-4:    Legacy [████████████████░░]  New [░░░░░░░░░░░░░░░░░░]
Week 5-8:    Legacy [██████████░░░░░░░░░░]  New [██████████░░░░░░░░░░]
Week 9-10:   Legacy [██████░░░░░░░░░░░░░░]  New [██████████████░░░░░░]
Week 11+:    Legacy [░░░░░░░░░░░░░░░░░░░░]  New [████████████████████]
```

**When to use:**
- Large monolithic system must be modernised incrementally.
- Zero downtime migration required.
- Legacy system must keep running during transition.
- Team wants to validate new system in production before full cutover.

**When NOT to use:**
- System small enough to rewrite atomically.
- Legacy system can afford downtime.
- Systems have incompatible data models (migration too complex).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Low risk | Errors caught on small traffic | Dual system operation longer |
| Gradual | Incremental validation | Complex routing and monitoring |
| Reversible | Easy rollback if issues | Legacy system must stay operational |
| Learning | New team learns in production | Potential data inconsistencies |

**Related patterns:** Anti-Corruption Layer, Adapter Pattern, Saga.

**Example implementation (using proxy/gateway):**
```yaml
# Route configuration in API Gateway
routes:
  - path: /orders/*
    traffic:
      - destination: legacy-service
        weight: 90%    # 90% to legacy
      - destination: order-service  # new
        weight: 10%    # 10% to new
    headers:
      X-Route-Version: "2024-03"
    logging: true      # Log all routing decisions
```

---

### Anti-Corruption Layer

**Intent:** Protect your domain model from being polluted by external system schemas and contracts. Translate external contracts into your internal domain language.

**Problem:**
- Consuming external system's API directly exposes your code to external changes.
- External schema may not match your domain concepts (terminology, structure, constraints).
- External system may have poor data quality or inconsistent semantics.
- Tight coupling makes it hard to switch external systems.

**Solution:**
Create an adapter layer between your domain model and external system. Translate external contracts (API responses, event schemas) into your internal domain language. Changes to external system are absorbed by adapter; internal code unaffected.

**Structure:**
```
Your Domain Model (Customer, Order)
        ↕
Anti-Corruption Layer (Adapter, Translator)
        ↕
External System API (Account, Order, LineItem)
```

**Example (Order domain consuming external CRM):**

External CRM API returns:
```json
{
  "account_id": "ACC-123",
  "account_name": "Acme Corp",
  "account_status": "ACTIVE",
  "credit_limit_cents": 50000,
  "last_order_date_epoch": 1705254000
}
```

Your domain model wants:
```python
class Customer:
  id: str
  name: str
  creditLimit: Money
  isActive: bool
  lastOrderDate: datetime
```

Anti-corruption layer (translator):
```python
def translate_crm_account_to_customer(crm_account):
  return Customer(
    id=crm_account['account_id'],
    name=crm_account['account_name'],
    creditLimit=Money(
      amount=crm_account['credit_limit_cents'] / 100,
      currency='USD'
    ),
    isActive=crm_account['account_status'] == 'ACTIVE',
    lastOrderDate=datetime.fromtimestamp(crm_account['last_order_date_epoch'])
  )
```

**When to use:**
- Consuming external system whose model differs from yours.
- External system is unstable or may be replaced.
- Maintaining isolation between bounded contexts.
- External system has poor data quality (filtering/cleaning needed).

**When NOT to use:**
- Minimal adaptation needed (cost of layer not justified).
- Complete integration with external system (no translation needed).
- Single, stable integration with well-designed external API.

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Domain isolation | Internal model stable | Translation code to maintain |
| Flexibility | Easy to swap external systems | Performance cost of translation |
| Data quality | Can filter and validate data | Additional layer of complexity |
| Versioning | External API changes don't break domain | Needs maintenance as external system evolves |

**Related patterns:** Adapter, Facade, Transformer.

---

## Microservices Patterns

### Service Mesh (Istio / Linkerd)

**Intent:** Manage service-to-service communication, reliability, security, and observability without modifying application code.

**Problem:**
- Direct service-to-service communication requires libraries (circuit breaker, retry logic) in every service.
- Difficult to enforce consistent policies (mTLS, rate limiting, logging) across all services.
- Observability (distributed tracing, metrics) requires instrumentation in application code.
- Debugging production issues requires understanding distributed system interactions.

**Solution:**
Deploy a service mesh control plane (e.g., Istio) and sidecars (Envoy proxies) alongside each service instance. Sidecars intercept all network traffic, implementing:
- mTLS encryption and certificate management.
- Retry logic and circuit breaking.
- Rate limiting and traffic shaping.
- Distributed tracing and metrics collection.
- Load balancing and failover.

**Structure:**
```
Service A (with Envoy sidecar)
    ↓ (mTLS, observability managed by sidecar)
Service B (with Envoy sidecar)
    ↕ (all traffic intercepted by sidecars)
Service C (with Envoy sidecar)

Control Plane (Istio)
  - Configures sidecars
  - Manages certificates
  - Collects metrics and traces
```

**Capabilities:**

| Capability | Example |
|---|---|
| Circuit breaking | Trip circuit if service error rate > 5% |
| Retry logic | Retry transient failures with exponential backoff |
| Rate limiting | Limit requests from single client to 1000/sec |
| mTLS | Automatically encrypt and verify all inter-service calls |
| Load balancing | Distribute requests across service replicas |
| Traffic splitting | Canary deploy: 90% to stable, 10% to new version |
| Distributed tracing | Trace requests across all services |
| Metrics | Collect latency, error rate, throughput per service |

**When to use:**
- Microservices architecture with 10+ services.
- Need consistent cross-cutting concerns (mTLS, observability) without touching app code.
- Complex traffic management (canary, A/B testing, circuit breaking).
- Security requirements (mTLS for all inter-service communication).

**When NOT to use:**
- Monolithic architecture (service mesh overhead not justified).
- Small number of services (< 5); direct libraries simpler.
- Extremely latency-sensitive systems (sidecar proxy adds ~10-20ms).
- Organization lacks operational expertise (steep learning curve).

**Overhead cost:**
- CPU: Envoy sidecar uses ~50-100m CPU per instance.
- Memory: ~100-200 MB per sidecar.
- Latency: ~5-15ms per hop (varies by configuration).
- Operational complexity: Requires dedicated platform team.

**Popular implementations:** Istio (feature-rich, complex), Linkerd (lightweight, focused), Consul (multi-cloud).

**Related patterns:** Sidecar, Circuit Breaker, Bulkhead.

---

### Sidecar Pattern

**Intent:** Deploy auxiliary service (sidecar) alongside main application to handle cross-cutting concerns without modifying application code.

**Problem:**
- Application needs to implement logging, monitoring, secret injection, traffic management.
- Adding this logic to application code couples application to infrastructure concerns.
- Hard to update cross-cutting concerns without redeploying application.

**Solution:**
Deploy a sidecar container (separate process) in same pod/machine as application. Sidecar:
- Intercepts network traffic (proxy).
- Handles infrastructure concerns (logging, monitoring, config injection).
- Communicates with application via local network.

**Common sidecars:**

| Sidecar | Purpose | Example |
|---------|---------|---------|
| Envoy proxy | Network traffic interception, load balancing, mTLS | Service mesh (Istio) |
| Fluentd / Filebeat | Log aggregation | Collect app logs, ship to ELK |
| Prometheus exporter | Metrics collection | Expose Prometheus format metrics |
| Config agent | Fetch and inject secrets | HashiCorp Consul template, AWS Secrets Manager sidecar |
| API gateway | Rate limiting, auth | Kong, Nginx sidecar |

**Structure:**
```yaml
Pod:
  - Main Application Container (Order Service)
  - Sidecar 1: Envoy proxy (network interception)
  - Sidecar 2: Fluentd (log shipping)
  - Sidecar 3: Config agent (secret injection)

  All containers share:
  - Network namespace (localhost communication)
  - Storage volumes (log files, configs)
  - CPU and memory
```

**When to use:**
- Cross-cutting concern needed across many services.
- Don't want to modify application code.
- Different services use different languages (standardise via sidecar).
- Rapid iteration on infrastructure without app redeployment.

**When NOT to use:**
- Low latency critical (sidecar hop adds latency).
- Simple concern easily integrated into application.
- Resources constrained (each sidecar consumes resources).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Separation of concerns | App code clean; infra separate | Complexity of multi-container orchestration |
| Language agnostic | Works with any language | Inter-process communication overhead |
| Reusability | Same sidecar across many apps | Resource consumption (multiple sidecars) |
| Updates | Infra updates without app redeployment | Potential for version mismatch |

**Related patterns:** Service Mesh, Adapter, Proxy.

---

### Saga Pattern

**Intent:** Manage distributed transactions across multiple microservices without traditional ACID transactions (which don't scale in distributed systems).

**Problem:**
- Distributed transaction across multiple services: Order Service → Payment Service → Inventory Service → Fulfillment Service.
- Traditional 2-phase commit doesn't scale; requires locks and introduces bottlenecks.
- If one service fails, partial state is left in other services.
- No central transaction coordinator in microservices.

**Solution:**
Break distributed transaction into local transactions in each service. Coordinate via events (choreography) or orchestrator (orchestration). If any step fails, compensating transactions undo previous steps.

**Saga approaches:**

### Choreography-based Saga

Each service listens for events and publishes events. No central orchestrator.

**Flow: Create Order → Capture Payment → Deduct Inventory → Create Shipment**

1. Client sends `CreateOrder` request to Order Service.
2. Order Service creates order in PENDING state, publishes `OrderCreated` event.
3. Payment Service listens, captures payment, publishes `PaymentCaptured` event (or `PaymentFailed` if error).
4. Inventory Service listens, deducts items, publishes `InventoryDeducted` event.
5. Fulfillment Service listens, creates shipment, publishes `ShipmentCreated` event.
6. Order Service listens, transitions order to CONFIRMED state.

**Compensating transactions (if Payment fails):**

3. Payment Service publishes `PaymentFailed` event (due to insufficient funds).
4. Inventory Service listens, reverses deduction, publishes `InventoryReserved` event.
5. Order Service listens, transitions order to CANCELLED state.
6. Client notified of failure.

```
Order Service: PENDING → CONFIRMED (if all steps succeed)
Order Service: PENDING → CANCELLED (if any step fails and compensates)
```

**Choreography advantages:**
- Decoupled; no central orchestrator.
- Each service is independent.

**Choreography disadvantages:**
- Hard to visualise overall flow (scattered across services).
- Difficult to debug (event chains complex).
- Risk of circular dependencies between services.

### Orchestration-based Saga

Central orchestrator directs each service in sequence.

**Flow: same as above, but via Saga Orchestrator**

1. Client sends `CreateOrder` to Saga Orchestrator.
2. Orchestrator sends `CapturePayment` command to Payment Service.
3. Payment Service responds with success; Orchestrator sends `DeductInventory` to Inventory Service.
4. Inventory Service responds with success; Orchestrator sends `CreateShipment` to Fulfillment Service.
5. Fulfillment Service responds; Orchestrator tells Order Service to confirm order.
6. Orchestrator sends response to client.

**Compensating transactions (if any step fails):**

3. Inventory Service fails; Orchestrator sends `ReverseDeduction` to Inventory Service.
4. Orchestrator sends `ReversePayment` to Payment Service.
5. Orchestrator tells Order Service to cancel order.
6. Orchestrator returns error to client.

**Orchestration advantages:**
- Clear flow; easy to visualise and debug.
- Centralised error handling and compensation.

**Orchestration disadvantages:**
- Single point of failure (orchestrator must be highly available).
- Tightly coupled to orchestrator; orchestrator must know all service contracts.

**Comparison:**

| Aspect | Choreography | Orchestration |
|--------|---|---|
| Coupling | Decoupled | Tightly coupled to orchestrator |
| Debuggability | Hard (scattered logic) | Easy (centralised flow) |
| Failure handling | Complex (implict) | Simple (explicit in orchestrator) |
| Scalability | Scales well | Orchestrator can become bottleneck |
| Complexity | Distributed complexity | Centralised complexity |

**When to use:**
- Long-running transactions across multiple services.
- Services owned by different teams (cannot add sync dependency).
- Failure recovery and compensating transactions needed.

**When NOT to use:**
- Single service or tightly integrated monolith (use DB transactions).
- Real-time transactions requiring immediate consistency.
- Saga steps are tightly coupled (consider merging into single service).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Distributed transactions | Scales across services | Eventual consistency |
| Failure handling | Compensating logic | Complex to implement and test |
| Loose coupling | Services independent | Hard to visualise overall flow |
| Tooling | Frameworks available | Requires training and discipline |

**Popular implementations:** Temporal, Cadence, NServiceBus, MassTransit, Axon Framework.

**Related patterns:** Event Sourcing, CQRS, Retry, Timeout.

---

### CQRS (Command Query Responsibility Segregation)

**Intent:** Separate read and write models to optimise each independently. Read model is optimised for queries; write model is optimised for consistency and transactions.

**Problem:**
- Single database model must serve both writes (transactions, consistency) and reads (complex queries, aggregations).
- Complex queries require expensive JOINs across many tables.
- High read load causes contention with writes (lock conflicts).
- Scaling reads independently from writes difficult.

**Solution:**
Maintain separate models:
- **Write Model:** Optimised for transactions and consistency. Normalized schema. Owned by single service.
- **Read Model:** Denormalised, optimised for queries. Can be distributed to multiple regions. Updated asynchronously from write model via events.

**Structure:**
```
Write Side:                          Read Side:
┌─────────────┐                     ┌──────────────┐
│ API: Create │                     │ API: Query   │
│  Update     │                     │   Aggregate  │
│  Delete     │                     │   Filter     │
└──────┬──────┘                     └──────┬───────┘
       │                                   │
       ↓                                   ↑
┌──────────────────────────────────────┐
│ Write Model (Normalized DB)          │
│ Orders table (ACID, transactions)    │
│ Publishes events on mutation         │
└──────────────────────────────────────┘
       │
       │ Events (OrderCreated, OrderConfirmed, OrderShipped)
       │
       ↓
┌──────────────────────────────────────┐
│ Read Model (Elasticsearch, Redis)    │
│ OrdersView (denormalised, queryable) │
│ Updated asynchronously               │
└──────────────────────────────────────┘
```

**Read model sync strategies:**

| Strategy | Mechanism | Latency | Consistency |
|----------|-----------|---------|---|
| Event-driven | Read model subscribes to events published by write model | 100ms-1s | Eventual |
| Polling | Read model polls write model for changes | 1-5s (depending on poll frequency) | Eventual |
| Query delegation | Read query delegates to write model (no separate read model) | < 100ms | Strong |
| Dual-write | Write side writes to both models in same transaction | < 100ms | Strong (if sync) |

**Example (Order Service):**

**Write model (normalized):**
```sql
-- Write side: normalized schema
CREATE TABLE orders (id, customer_id, status, total_amount, ...);
CREATE TABLE order_items (id, order_id, product_id, quantity, unit_price, ...);
```

**Read model (denormalised):**
```json
// Elasticsearch read model (denormalised for fast queries)
{
  "orderId": "ORD-123",
  "customerId": "CUST-456",
  "customerName": "Acme Corp",
  "status": "SHIPPED",
  "totalAmount": 1250.00,
  "items": [
    {"productId": "PROD-1", "productName": "Widget", "quantity": 5, "unitPrice": 50},
    {"productId": "PROD-2", "productName": "Gadget", "quantity": 2, "unitPrice": 125}
  ],
  "shippingAddress": "123 Main St, Anytown",
  "createdAt": "2024-01-15T10:30:00Z",
  "shipmentTrackingNumber": "1Z999AA10123456784"
}
```

**When to use:**
- Complex queries dominating system (reporting, analytics).
- High read load; independent read scaling needed.
- Eventual consistency acceptable (typical in microservices).
- Multiple clients with different query requirements.

**When NOT to use:**
- Strict consistency required on all reads (use single model with DB transactions).
- Simple CRUD operations (overhead not justified).
- Minimal reporting requirements.

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Read performance | Optimised queries, no JOINs | Eventual consistency lag |
| Write consistency | ACID transactions in write model | Complex event-driven sync |
| Scalability | Independent read/write scaling | Dual data storage |
| Flexibility | Clients tailor read models | Multiple models to maintain |

**Popular implementations:** Greg Young Event Store, Axon Framework, MassTransit, Spring Data.

**Related patterns:** Event Sourcing, Saga, API Gateway.

---

### Event Sourcing

**Intent:** Store all changes to domain state as an immutable sequence of events. Reconstruct current state by replaying events.

**Problem:**
- Traditional databases store only current state; history is lost.
- Auditing and debugging require explicit audit logging.
- Understanding why state changed requires separate investigation.
- Scaling read queries difficult without denormalisation.

**Solution:**
Store all events (state changes) in append-only event log. Current state derived by replaying events from beginning. Create projections (denormalised views) for efficient querying.

**Structure:**
```
Event Log (append-only):
  1. OrderCreated: {orderId: ORD-1, customerId: CUST-1, items: [...]}
  2. PaymentCaptured: {orderId: ORD-1, amount: 1000}
  3. InventoryDeducted: {orderId: ORD-1, items: [...]}
  4. OrderConfirmed: {orderId: ORD-1}
  5. ShipmentCreated: {orderId: ORD-1, trackingNumber: "1Z999AA..."}

Current State (reconstructed):
  Order(
    id: ORD-1,
    status: SHIPPED,
    payment: CAPTURED,
    items: [...],
    shipmentTracking: "1Z999AA..."
  )

Projection (denormalised view):
  OrderDetailsView:
    - orderId: ORD-1
    - customerName: "Acme Corp"
    - status: SHIPPED
    - items: [...]
```

**Key components:**

| Component | Purpose |
|---|---|
| Event Store | Append-only log of all events. Immutable. Replicated and backed up. |
| Aggregates | Reconstruct domain objects by replaying events. |
| Projections | Denormalised views built from event log. Updated asynchronously. |
| Snapshots | Checkpoint of aggregate state at event 1000 (to avoid replaying all 1000 events) |
| Event Handlers | Listen to events and update projections or trigger other services |

**Example (Order aggregate):**

```python
class OrderAggregate:
  def __init__(self, events: List[Event]):
    self.id = None
    self.status = "PENDING"
    self.items = []
    self.total = 0

    # Replay all events to reconstruct current state
    for event in events:
      self.apply(event)

  def apply(self, event):
    if isinstance(event, OrderCreated):
      self.id = event.orderId
      self.items = event.items
      self.total = event.total
    elif isinstance(event, OrderConfirmed):
      self.status = "CONFIRMED"
    elif isinstance(event, OrderCancelled):
      self.status = "CANCELLED"
      # Publish compensation events
      yield InventoryReleased(self.id, self.items)
      yield PaymentRefunded(self.id, self.total)
```

**Snapshotting (performance optimisation):**

Instead of replaying 10,000 events from the beginning, take a snapshot every 1000 events:

```
Events 1-1000 → Snapshot(state at event 1000)
Events 1001-2000 → Snapshot(state at event 2000)

To reconstruct current state:
  1. Load snapshot at event 2000 (O(1) instead of O(n))
  2. Replay events 2001-2100 (only latest 100 events)
```

**When to use:**
- Complete audit trail required (compliance, debugging).
- Temporal queries important (what was state at point-in-time?).
- Event-driven architecture with multiple projections.
- Complex state transitions (saga compensation).

**When NOT to use:**
- Strong consistency on reads critical (use DB transactions instead).
- Simple CRUD with no audit requirements.
- Extreme write throughput (event store can become bottleneck).
- Events grow unbounded without cleanup policy.

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Complete audit trail | Immutable history of all changes | Storage and replay overhead |
| Temporal queries | Answer "what was state at time T?" | Event log eventually grows large |
| Flexibility | Projections can be rebuilt anytime | Complexity of event-driven system |
| Debugging | Replay events to understand failures | Requires discipline in event design |

**Storage overhead:**
- Event log grows unbounded without cleanup.
- Typical enterprise system generates 100-1000 events/sec.
- Store 1000 events/sec × 86,400 sec/day = 86 million events/day.
- Archive old events; keep last N years.

**Popular implementations:** EventStoreDB, Axon Framework, Kafka (as event log), Apache Pulsar.

**Related patterns:** CQRS, Saga, Snapshot.

---

## Data Patterns

### Database per Service

**Intent:** Give each microservice ownership of its data. Services don't share databases; each service has its own datastore, ensuring independence and loose coupling.

**Problem:**
- Shared database between services creates tight coupling.
- Schema changes to shared database affect all services.
- One service's data corruption affects all services.
- Scaling is difficult; can't scale one service's database independently.

**Solution:**
Each service owns its data and datastore. Service A cannot directly query Service B's database. If Service A needs data from Service B, it calls Service B's API or subscribes to Service B's events.

**Structure:**
```
Service A              Service B              Service C
    │                     │                     │
    ▼                     ▼                     ▼
PostgreSQL          PostgreSQL            MongoDB
(Service A's       (Service B's           (Service C's
 schema)            schema)                schema)
    │                     │                     │
    └─────────────────────┼─────────────────────┘
              Events & APIs for cross-service queries
```

**Data ownership rules:**

| Data | Owned By | How Others Access |
|------|----------|---|
| Order data | Order Service | Via Order Service API or OrderCreated events |
| Payment data | Payment Service | Via Payment Service API or PaymentCaptured events |
| Inventory data | Inventory Service | Via Inventory Service API or InventoryDeducted events |
| Customer data | Customer Service | Via Customer Service API (read-only) |

**Cross-service queries (when needed):**

Instead of direct SQL join:
```sql
-- WRONG: Direct cross-service join
SELECT o.id, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id;
```

Do one of:

1. **Call API:**
```python
order = order_service.get_order(order_id)
customer = customer_service.get_customer(order.customer_id)
result = {order: order, customer: customer}
```

2. **Denormalize in read model (CQRS):**
```python
# Elasticsearch projection: OrderDetailsView includes customer name
# Updated asynchronously when CustomerUpdated event published
result = elasticsearch.search("OrderDetailsView", order_id)
```

3. **Cache customer reference (cache-aside):**
```python
customer = cache.get(f"customer:{customer_id}")
if not customer:
  customer = customer_service.get_customer(customer_id)
  cache.set(f"customer:{customer_id}", customer, ttl=3600)
return customer
```

**When to use:**
- Multiple microservices need independent data ownership.
- Teams own separate services and don't want to coordinate schema changes.
- Horizontal scaling of individual services needed.
- One service's failure must not affect others' data access.

**When NOT to use:**
- Monolithic architecture (single database simpler).
- Strict consistency required (joins and transactions needed).
- Data tightly related (natural to keep in single database).
- Minimal cross-service queries.

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Independence | Services own data; schema changes independent | Complex to query across services |
| Loose coupling | Services don't depend on shared database | May require data denormalisation |
| Scalability | Each service's DB scaled independently | Eventual consistency between services |
| Flexibility | Each service chooses DB type (SQL/NoSQL) | More operational overhead (more DBs) |

**Related patterns:** CQRS, Event Sourcing, API Gateway, Anti-Corruption Layer.

---

### Shared Database (Anti-Pattern, Sometimes Pragmatic)

**Intent:** Multiple services share a single database for convenience and consistency. **WARNING: This is generally an anti-pattern; use only when justified.**

**Problem:**
- Multiple teams want strong ACID consistency across services.
- Distributed transactions complex and expensive.
- Database per service introduces eventual consistency.

**Solution:**
All services share one database and schema. Use database transactions for ACID consistency. Simple querying without service calls.

**When to use (rarely):**
- Strong consistency absolutely required (financial transactions).
- Services tightly integrated (difficult to separate data models).
- Small, legacy system not worth refactoring.
- Heavy cross-service querying makes API calls impractical.

**CRITICAL rules if using shared database:**
1. **Schema ownership:** Define which service "owns" each table (can modify schema).
2. **Foreign key constraints:** Minimise foreign keys across services (limits service independence).
3. **No direct table access:** Services only access tables they own; others via stored procedures.
4. **Data isolation:** Use row-level security or soft deletes to isolate data.
5. **Strong contracts:** Document schema changes; notify dependent services early.

**Example schema ownership:**
```sql
-- Order Service owns these tables
CREATE TABLE orders (id, customer_id, status, ...);
CREATE TABLE order_items (id, order_id, product_id, ...);

-- Payment Service owns these tables
CREATE TABLE payments (id, order_id, amount, status, ...);

-- Inventory Service owns these tables
CREATE TABLE inventory (id, product_id, quantity, ...);

-- RULE: Order Service can only SELECT from payments, inventory (read-only)
-- RULE: Order Service must not issue UPDATE/DELETE to tables it doesn't own
-- RULE: Schema changes to any table require advance notice to all services
```

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Consistency | ACID transactions across services | Tight coupling (hard to separate) |
| Simplicity | Joins work; no API calls | Scaling difficult; single DB bottleneck |
| Performance | No network overhead | Contention between services |
| Maintenance | Single schema to maintain | Coordination overhead for changes |

**When to migrate away:**
- Services need to scale independently.
- Teams require autonomy over schema.
- Database becomes bottleneck.

---

### Outbox Pattern

**Intent:** Guarantee that when a service persists state to database, it also publishes events. Solves the dual-write problem (cannot atomically write to DB and publish to message queue).

**Problem:**
- Service updates database AND publishes event to queue.
- If service crashes between DB update and event publish, event is lost.
- If event queue rejects the message, DB is already updated (inconsistent state).

**Solution:**
1. Service writes both the aggregate AND an outbox event entry in the same database transaction.
2. Separate process (outbox poller) reads unpublished events from outbox table.
3. Poller publishes events to message queue.
4. Once published successfully, poller deletes outbox entry.

**Structure:**
```
Service Code:
  1. BEGIN TRANSACTION
  2. INSERT INTO orders (...) VALUES (...)
  3. INSERT INTO outbox_events (event_type, event_payload, published=false) VALUES (...)
  4. COMMIT TRANSACTION
  ← Guaranteed: Order persisted AND event in outbox

Outbox Poller:
  1. SELECT * FROM outbox_events WHERE published = false
  2. Publish each event to message queue
  3. UPDATE outbox_events SET published = true WHERE id = ...
  4. Optional: Archive old entries
```

**Implementation example:**

```python
class OrderService:
  def create_order(self, order_data):
    with db.transaction():
      # Write order aggregate
      order = Order(...)
      db.session.add(order)
      db.session.flush()  # Assign ID

      # Write outbox event in same transaction
      outbox_event = OutboxEvent(
        aggregate_id=order.id,
        event_type="OrderCreated",
        event_payload=json.dumps({
          "orderId": order.id,
          "customerId": order.customer_id,
          "items": order.items
        }),
        published=False,
        created_at=datetime.now()
      )
      db.session.add(outbox_event)
      db.session.commit()  # Both committed atomically

    return order

class OutboxPoller:
  def poll(self):
    unpublished = db.query(OutboxEvent).filter_by(published=False).limit(100).all()

    for event in unpublished:
      try:
        kafka.publish(event.event_type, event.event_payload)
        db.update(OutboxEvent).filter_by(id=event.id).update({"published": True})
      except Exception as e:
        logger.error(f"Failed to publish event {event.id}: {e}")
        # Retry on next poll; event stays in outbox
```

**Failure scenarios handled:**

| Scenario | Outcome |
|----------|---------|
| Service crashes before DB commit | Order and event both rolled back (nothing published) |
| Service crashes after DB commit but before event publish | Event stays in outbox; poller publishes on retry |
| Poller crashes after publish but before marking published=true | Event published twice; must be idempotent |

**Idempotency requirement:**
- Consumers must handle duplicate events gracefully.
- Use event ID as idempotency key in consumer.
- Example: `INSERT ... ON CONFLICT (event_id) DO NOTHING`

**When to use:**
- Guaranteed event publishing required (no lost events).
- Database and message queue are separate systems.
- Coupling of DB and queue acceptable (single DB transaction).

**When NOT to use:**
- Single datastore (e.g., database with built-in message queue like PostgreSQL).
- Event loss acceptable (best-effort publishing).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Atomicity | Events never lost | Extra outbox table and polling logic |
| Reliability | No dual-write inconsistencies | Polling latency (events delayed by poll interval) |
| Simplicity | Single transaction for state + event | Consumer must handle duplicates |

**Related patterns:** Saga, Event Sourcing, Idempotency.

---

### Read Replicas

**Intent:** Distribute read load across multiple database instances. Primary handles writes; replicas handle reads.

**Problem:**
- Single database instance becomes bottleneck under high read load.
- Read queries lock resources; contention with write transactions.
- Cross-region users experience high latency to single primary.

**Solution:**
Replicate data from primary to one or more read replicas. All writes go to primary; reads can use replicas. Replicas updated asynchronously (lag of milliseconds to seconds).

**Structure:**
```
Writes        Reads
   ↓            ↑
Primary DB (PostgreSQL)
   ↓
  Replication Stream (write-ahead log)
   ↓         ↓         ↓
Replica 1  Replica 2  Replica 3
(Regional) (Backup)  (Analytics)
```

**Configuration:**

| Setting | Value | Purpose |
|---------|-------|---------|
| Replication lag | Typical 100-500ms | How far behind replicas are from primary |
| Replication mode | Synchronous / Asynchronous | Synchronous: primary waits for ack from replica; slower writes but stronger consistency |
| Failover | Automatic / Manual | Automatic: if primary dies, replica promoted automatically |
| Backup replica | Yes | One replica kept for backup; not used for reads |

**Latency considerations:**

| Scenario | Latency |
|----------|---------|
| Write to primary | 10-50ms |
| Read from primary | 5-20ms |
| Read from replica (same region) | 5-20ms + replication lag (100-500ms) |
| Read from replica (different region) | 20-100ms + replication lag |

**When to use:**
- Read load far exceeds write load (typical OLTP pattern).
- Multi-region deployment needed.
- Reporting workloads must not affect transactional system.
- Availability improvement (replica can take over if primary fails).

**When NOT to use:**
- Write-heavy workloads (replicas don't help with write bottleneck).
- Strong consistency required (replicas have lag).
- Single-region deployment with low load (complexity not justified).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Read scalability | Reads distributed across replicas | Eventual consistency (replication lag) |
| High availability | Replica can take over if primary fails | Synchronous replication slows writes |
| Analytics isolation | Analytics queries don't affect transactional DB | Extra infrastructure (replica servers) |
| Cost efficiency | Replicate to cheap storage for backups | Data bandwidth for replication |

**Related patterns:** CQRS, Database per Service, Sharding.

---

## Resilience Patterns

### Circuit Breaker

**Intent:** Prevent cascading failures by stopping calls to a failing service and returning fast without waiting for timeout.

**Problem:**
- Service A calls Service B (which is down or slow).
- Service A threads exhaust waiting for Service B to respond (default timeout 30s).
- Service A request queue backs up; resources exhausted.
- Other clients of Service A experience failures (cascading).

**Solution:**
Deploy circuit breaker between Service A and Service B. Circuit has three states:

**States:**

| State | Behavior | Trigger |
|-------|----------|---------|
| CLOSED (normal) | Requests pass through to Service B | < 5% error rate for last 100 requests |
| OPEN (trip) | Reject requests immediately; fail-fast | Error rate > 5% OR timeout rate > 3% |
| HALF_OPEN (recovery) | Allow limited requests to test if Service B recovered | After 30s timeout in OPEN state |

**Transition diagram:**
```
CLOSED ──(error rate > 5%)──→ OPEN ──(timeout, 30s)──→ HALF_OPEN ──(success)──→ CLOSED
  ↑                                                            │
  └────────────(failure)─────────────────────────────────────┘
```

**Configuration parameters:**

| Parameter | Typical Value | Purpose |
|-----------|---|---|
| failure_threshold | 5% | Error rate that triggers OPEN state |
| timeout_duration | 30 seconds | How long to stay OPEN before trying HALF_OPEN |
| half_open_max_calls | 3 | Number of requests to allow in HALF_OPEN state |
| window_size | 100 requests | Rolling window for calculating error rate |

**Example implementation (Resilience4j in Java):**

```java
CircuitBreaker breaker = CircuitBreaker.of("payment-service",
  CircuitBreakerConfig.custom()
    .failureRateThreshold(5.0)
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .permittedNumberOfCallsInHalfOpenState(3)
    .slidingWindowType(SlidingWindowType.COUNT_BASED)
    .slidingWindowSize(100)
    .build()
);

// Usage
try {
  response = breaker.executeSupplier(() -> paymentService.capture(orderId));
} catch (CallNotPermittedException e) {
  // Circuit is OPEN; fail-fast without calling payment service
  logger.warn("Payment service circuit breaker is open; failing fast");
  return error("Payment service temporarily unavailable");
} catch (Exception e) {
  // Other errors (timeout, network, etc.)
  logger.error("Payment service error: {}", e.getMessage());
  return error("Payment service error: " + e.getMessage());
}
```

**Metrics emitted:**

| Metric | Purpose |
|---|---|
| circuit_breaker_calls_total (counter) | Total calls attempted |
| circuit_breaker_calls_success_total (counter) | Calls that succeeded |
| circuit_breaker_calls_failure_total (counter) | Calls that failed |
| circuit_breaker_state (gauge) | Current state (CLOSED=0, HALF_OPEN=1, OPEN=2) |

**When to use:**
- Calling external service over network (HTTP, RPC).
- Service can fail or timeout.
- Cascading failures possible (shared resources, thread pools).
- Want to avoid thundering herd (all clients retrying at once).

**When NOT to use:**
- Local method calls (no network involved).
- Service never fails (overhead not justified).
- Single client (cascading failures irrelevant).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Failure isolation | Prevents cascading failures | Complexity of state machine |
| Fast failure | Fail-fast instead of hanging | Slightly increased error rate (intentional) |
| Graceful degradation | Allow partial functionality | Circuit must be observed and acted upon |

**Related patterns:** Bulkhead, Timeout, Retry, Fallback.

---

### Bulkhead

**Intent:** Isolate critical resources (thread pools, connection pools) so that one component's failure doesn't exhaust resources for other components.

**Problem:**
- Shared thread pool: If Service A (slow) uses all threads waiting on timeout, Service B (fast) has no threads and starts failing.
- Shared connection pool: Heavy workload from one service exhausts connections; other services cannot connect.

**Solution:**
Partition resources into separate "bulkheads" (compartments). Each service gets dedicated resource allocation. Failure in one bulkhead doesn't affect others.

**Types:**

### Thread Pool Isolation (Bulkhead)

```java
// Dedicated thread pool for payment service
ExecutorService paymentThreadPool = Executors.newFixedThreadPool(10);

// Dedicated thread pool for inventory service
ExecutorService inventoryThreadPool = Executors.newFixedThreadPool(10);

// Usage: each service uses its own pool
paymentThreadPool.submit(() -> paymentService.capture(orderId));
inventoryThreadPool.submit(() -> inventoryService.deduct(orderId));

// If payment service exhausts its 10 threads, inventory service still has 10 available
```

### Connection Pool Isolation

```properties
# Payment Service: dedicated connection pool
spring.datasource.hikari.maximumPoolSize=20
payment.datasource.url=jdbc:postgresql://db-primary:5432/payments
payment.datasource.hikari.maximum-pool-size=20

# Inventory Service: separate connection pool
inventory.datasource.url=jdbc:postgresql://db-primary:5432/inventory
inventory.datasource.hikari.maximum-pool-size=15
```

**Benefits:**
- Payment service slow? Only affects payment calls; inventory still responsive.
- Inventory service exhausts connections? Payment service still works.

**Comparison to shared pool:**
```
SHARED POOL (100 connections):
  Payment: uses 80 connections (slow service)
  Inventory: needs 20 connections, but only 20 available
  Result: Inventory blocked

BULKHEAD (dedicated pools):
  Payment: 80/100 connections in payment pool
  Inventory: 15/20 connections in inventory pool
  Result: Both services work, though payment is slow
```

**When to use:**
- Multiple critical services sharing resources.
- One service slower than others.
- Need to protect against resource exhaustion.
- Different services have different SLAs.

**When NOT to use:**
- Single service (isolation not needed).
- Plenty of resources (no contention).
- Services rarely fail (overhead not justified).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Resource isolation | One service's failure doesn't affect others | Over-provisioning (some resources unused) |
| Predictability | Each service has guaranteed resources | Complexity of separate pools |
| Failure containment | Blast radius limited | Potential for deadlock if pools misconfigured |

**Related patterns:** Circuit Breaker, Timeout, Retry.

---

### Retry with Backoff

**Intent:** Automatically retry failed requests with exponential backoff and jitter to handle transient failures without overwhelming the server.

**Problem:**
- Transient network errors, temporary unavailability should not cause permanent failures.
- Naive retry (retry immediately) can overwhelm recovering server (thundering herd).
- No backoff means high CPU usage when server is down.

**Solution:**
Retry failed requests with exponential backoff: wait longer between each retry. Add jitter to prevent synchronized retries from multiple clients.

**Backoff formula:**
```
delay = min(max_delay, base_delay * 2^(attempt - 1) + jitter)

where:
  base_delay = 100ms (typical)
  max_delay = 30s (typical)
  jitter = random(0, delay) to avoid thundering herd
```

**Example with 3 retries:**
```
Attempt 1: Immediate (0ms delay)
Attempt 2: Failed; wait ~100ms + jitter (0-100ms) = ~50ms
Attempt 3: Failed; wait ~200ms + jitter (0-200ms) = ~150ms
Attempt 4: Failed; wait ~400ms + jitter (0-400ms) = ~350ms
Total time: ~550ms

Without backoff (immediate retry):
Attempt 1: Immediate
Attempt 2: Immediate
Attempt 3: Immediate
Attempt 4: Immediate
Total time: ~0ms (but hammers the failing service)
```

**Implementation (Java with Resilience4j):**

```java
Retry retry = Retry.of("payment-service",
  RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(100))
    .intervalFunction(IntervalFunction.ofExponentialBackoff(100, 2, 0.5))
    .retryExceptions(IOException.class, TimeoutException.class)
    .ignoreExceptions(ValidationException.class)  // don't retry validation errors
    .build()
);

// Usage
response = retry.executeSupplier(() -> paymentService.authorize(orderId, amount));
```

**Retry policies by error type:**

| Error | Retryable? | Reason |
|-------|-----------|--------|
| Connection timeout | YES | Transient; server may recover |
| 503 Service Unavailable | YES | Server overloaded; will recover |
| 400 Bad Request | NO | Client error; retry won't fix |
| 401 Unauthorized | NO | Auth error; retry won't fix |
| Network error (SocketException) | YES | Transient; network may recover |
| Deadlock (DB) | YES | Transient; lock may be released |

**When to use:**
- Transient failures common (network timeouts, brief outages).
- Idempotent operations (safe to retry).
- Want to tolerate brief service unavailability.
- Acceptable to increase latency for reliability.

**When NOT to use:**
- Non-idempotent operations (e.g., payment charge twice).
- Strict latency requirements (retry adds delay).
- Service permanently down (retries waste resources).

**Idempotency requirement:**
To safely retry, operation must be idempotent (same result if called multiple times):

```
IDEMPOTENT:
  GET /orders/123  (read; safe to retry)
  PUT /orders/123  (replace; safe to retry if using same data)

NON-IDEMPOTENT:
  POST /payments   (create; creates duplicate if retried)
  DELETE /orders/123  (deletes twice; ok first time, error second time)
```

**Pattern: Idempotency keys for non-idempotent operations:**
```
POST /payments
  idempotency-key: "ORD-123-PAYMENT-1"
  body: {orderId: "ORD-123", amount: 1000}

Server stores idempotency key. If request arrives again with same key, return cached result instead of charging again.
```

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Reliability | Tolerates transient failures | Increased latency (wait for retries) |
| Simplicity | Automatic retry without code changes | Requires idempotency discipline |
| Server protection | Backoff prevents thundering herd | May not help if service permanently down |

**Related patterns:** Circuit Breaker, Timeout, Bulkhead.

---

### Timeout

**Intent:** Set maximum time to wait for remote call; fail fast if response doesn't arrive in time.

**Problem:**
- Slow or hung remote service blocks caller indefinitely.
- Caller's resources (threads, connections) exhausted waiting.
- Cascading failures (caller's caller also hangs).

**Solution:**
Set timeout on every remote call. If response doesn't arrive before timeout, fail and return error. Allow caller to handle failure (retry, fallback, circuit break).

**Timeout hierarchy:**

```
Overall request deadline (e.g., 5s from client perspective):
  │
  ├─ Service A timeout: 2s
  │  └─ Service B timeout: 1s
  │     └─ Service C timeout: 500ms
  │
  └─ Reserve: 500ms for error handling, logging, fallback
```

**Rule: timeouts decrease as you go deeper; reserve time for error handling.**

**Configuration:**

| Call Type | Typical Timeout |
|-----------|---|
| Fast local call (same DC) | 500ms |
| Normal remote call | 2-5s |
| Slow batch operation | 10-30s |
| Long-running job | minutes (use async instead) |

**Implementation (HTTP client with timeout):**

```python
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

session = requests.Session()
session.mount('http://', HTTPAdapter(
  max_retries=Retry(
    total=3,
    backoff_factor=0.5
  )
))

response = session.get(
  'http://payment-service/transactions/authorize',
  timeout=5,  # total timeout: 5 seconds
  headers={'X-Request-Timeout': '5000'}  # inform upstream
)
```

**Cascading timeout (inform downstream about remaining time):**

```python
import time

def call_order_service(order_id, overall_deadline_ms):
  remaining_ms = overall_deadline_ms - (time.time() * 1000)

  if remaining_ms <= 0:
    raise TimeoutError("No time left for call")

  # Reserve 100ms for error handling
  service_timeout = max(100, remaining_ms - 100) / 1000

  response = requests.get(
    f'http://order-service/orders/{order_id}',
    timeout=service_timeout,
    headers={'X-Deadline-Ms': str(overall_deadline_ms)}
  )
```

**When to use:**
- Calling any external service (HTTP, RPC, database).
- Hanging requests unacceptable.
- Want to fail fast and try alternatives (circuit breaker, fallback).

**When NOT to use:**
- Local synchronous operations (timeout not applicable).
- Long-running operations (use async jobs instead).
- Timeout < network round-trip time (will always timeout).

**Trade-offs:**

| Aspect | Benefit | Cost |
|--------|---------|------|
| Fast failure | Avoid hanging indefinitely | Legitimately slow requests timeout |
| Resource protection | Prevent resource exhaustion | Requires tuning right timeout value |
| Predictability | Known worst-case latency | May hurt availability if timeout too low |

**Related patterns:** Circuit Breaker, Retry, Bulkhead.

---

## Pattern Selection Guide

**Choosing the right pattern:**

| Problem | Patterns to Consider |
|---------|-----|
| Service-to-service call failing | Circuit Breaker, Timeout, Retry, Bulkhead |
| Distributed transaction across services | Saga (Choreography or Orchestration) |
| Multiple clients with different API needs | BFF, API Gateway |
| Need to query across services | CQRS, Database per Service + denormalisation |
| Event-driven architecture | Event Sourcing, Outbox Pattern, CQRS |
| Single point of failure | Service Mesh, Replication, Failover |
| Monolith to microservices migration | Strangler Fig, Anti-Corruption Layer |
| High read load on database | Read Replicas, CQRS |
| Audit and history required | Event Sourcing |

---

## Document Metadata

| Field | Value |
|-------|-------|
| Last Updated | [Date] |
| Maintained By | [Team] |
| Review Frequency | Quarterly |
| Next Review Date | [Date] |

