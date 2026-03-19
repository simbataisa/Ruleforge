# Error Handling Patterns for Enterprise Microservices

## Error Taxonomy

### System Errors
Unexpected, non-recoverable failures in infrastructure or runtime environment.

**Examples:**
- Database connection pool exhaustion
- Out of memory exception
- Disk space exhausted
- Unable to bind to network port

**Handling:**
- Log at ERROR or CRITICAL level
- Alert operators immediately
- Return 500 Internal Server Error to clients
- Do NOT retry automatically
- Requires manual intervention to resolve

```java
try {
    hikariDataSource.getConnection();
} catch (SQLException e) {
    logger.error("Database connection pool exhausted", e);
    alerting.notify("CRITICAL: DB pool exhausted", AlertSeverity.P1);
    throw new SystemException("Database unavailable", e);
}
```

### Domain Errors
Expected errors within the business domain (business logic validation failures).

**Examples:**
- User not found (lookup failed)
- Insufficient account balance
- Invalid email format
- Resource already exists (duplicate)
- Permission denied
- Invalid state transition (trying to cancel an already-cancelled subscription)

**Handling:**
- Log at WARN or INFO level (expected occurrence)
- Return 4xx status code to client (client's responsibility to fix)
- Include domain error code for programmatic handling
- No retry (client must fix the request)

```java
public class InvoiceService {
    public void payInvoice(String invoiceId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId)); // 404

        if (userAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Account balance " + userAccount.getBalance() +
                " is less than payment amount " + amount
            ); // 400
        }

        invoice.markAsPaid();
    }
}
```

### Validation Errors
Request data does not meet constraints (schema, format, business rules).

**Examples:**
- Missing required field
- Invalid JSON syntax
- Email format invalid
- String exceeds max length
- Negative number in quantity field
- Invalid enum value

**Handling:**
- Log at WARN level (common, expected)
- Return 400 Bad Request
- Include detailed field-level errors
- Include constraint information (min/max length, pattern, etc.)

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "trace_id": "123e4567-e89b-12d3-a456-426614174000",
    "details": {
      "field_errors": [
        {
          "field": "email",
          "value": "invalid-email",
          "constraint": "valid email format",
          "message": "Email must be valid RFC 5322 format"
        },
        {
          "field": "age",
          "value": -5,
          "constraint": "min value 0",
          "message": "Age must be >= 0"
        },
        {
          "field": "name",
          "value": null,
          "constraint": "required",
          "message": "Name is required"
        }
      ]
    }
  }
}
```

### Integration Errors
Errors from calling external services or dependencies.

**Examples:**
- Timeout calling another microservice
- External API returned error (payment gateway, email service)
- Database connection refused
- Cache unavailable
- Message queue broker unreachable

**Handling:**
- Determine if error is transient (retry-able) or permanent (fail fast)
- Log at WARN level for transient, ERROR for permanent
- Use circuit breaker pattern for repeated failures
- Return 503 Service Unavailable if critical dependency down
- Implement fallback/graceful degradation when possible

```java
@Service
public class PaymentService {
    private final CircuitBreaker circuitBreaker;
    private final PaymentGateway paymentGateway;

    public PaymentResult processPayment(Payment payment) {
        try {
            return circuitBreaker.execute(() ->
                paymentGateway.charge(payment) // May timeout or fail
            );
        } catch (CircuitBreakerOpenException e) {
            logger.error("Payment gateway circuit breaker open; payment service degraded", e);
            return PaymentResult.DEFERRED; // Retry later
        } catch (TimeoutException e) {
            logger.warn("Payment gateway timeout", e);
            throw new IntegrationException("Payment gateway timed out", e);
        } catch (PaymentGatewayException e) {
            logger.error("Payment gateway error", e);
            throw new IntegrationException("Payment processing failed", e);
        }
    }
}
```

---

## Error Response Schema

All error responses must follow this standard envelope format.

### Standard Error Response
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Resource with ID 'res-123' not found",
    "details": {
      "resource_id": "res-123",
      "resource_type": "Invoice"
    },
    "trace_id": "123e4567-e89b-12d3-a456-426614174000",
    "timestamp": "2026-03-14T10:30:00.123Z"
  }
}
```

### Schema Definition
```json
{
  "type": "object",
  "required": ["error"],
  "properties": {
    "error": {
      "type": "object",
      "required": ["code", "message", "trace_id", "timestamp"],
      "properties": {
        "code": {
          "type": "string",
          "pattern": "^[A-Z_]+$",
          "description": "Error code for programmatic handling (immutable for API version)"
        },
        "message": {
          "type": "string",
          "description": "Human-readable error message (can change between versions)"
        },
        "details": {
          "type": "object",
          "description": "Additional context specific to the error type",
          "additionalProperties": true
        },
        "trace_id": {
          "type": "string",
          "pattern": "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
          "description": "Distributed trace ID for correlation across services"
        },
        "timestamp": {
          "type": "string",
          "format": "date-time",
          "description": "ISO 8601 timestamp when error occurred"
        }
      }
    }
  }
}
```

### Implementation (Java/Spring)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException e, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                code = "RESOURCE_NOT_FOUND",
                message = e.getMessage(),
                details = Map.of(
                    "resource_id", e.getResourceId(),
                    "resource_type", e.getResourceType()
                ),
                traceId = MDC.get("traceId"),
                timestamp = Instant.now()
            ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                code = "VALIDATION_FAILED",
                message = "Request validation failed",
                details = Map.of("field_errors", e.getFieldErrors()),
                traceId = MDC.get("traceId"),
                timestamp = Instant.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception e) {
        logger.error("Unhandled exception", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred",
                details = Map.of(),
                traceId = MDC.get("traceId"),
                timestamp = Instant.now()
            ));
    }
}
```

---

## HTTP Status Code Decision Guide

```
┌─────────────────────────────────────┐
│ Is the request syntactically valid? │
└────────────┬────────────────────────┘
             │
        ┌────┴────┐
        │ No      │ Yes
        ▼         ▼
      400       Is auth header valid?
      Bad       ┌──────────────────┐
      Request   │                  │
              ┌─┴──┬──────┐        │
              │No │ Yes   │ No (expired/invalid)
              ▼   ▼       ▼
             403 Proceed? 401
            Forbidden  Unauthorized
                      ▼
                  Can authenticate?
                  ┌──────────┐
                  │          │
                ┌─┴─┐        │
                │No │ Yes    │
                ▼   ▼        ▼
               503  Does user have permission?
          Service  ┌──────────────────┐
          Unavail. │                  │
                 ┌─┴──┐               │
                 │No │ Yes           │
                 ▼   ▼               ▼
                403 Does resource exist?
              Forbidden ┌──────────────────┐
                        │                  │
                      ┌─┴──┐               │
                      │No │ Yes           │
                      ▼   ▼               ▼
                     404  Can process?
                  Not Found┌─────────┐
                          │         │
                        ┌─┴─┐     ┌─┴─┐
                        │No │     │Yes│
                        ▼   ▼     ▼   ▼
                      409/429   200/201
                  Conflict/Rate Limited
```

### Status Code Mapping Table

| Code | Reason | When | Example |
|---|---|---|---|
| **2xx Success** | | | |
| 200 | OK | Successful GET, successful sync operation | Retrieve user profile |
| 201 | Created | Resource created via POST | POST /users returns 201 with Location header |
| 202 | Accepted | Async operation queued | Long-running batch job accepted |
| 204 | No Content | Success with empty response | DELETE resource, or update with no response |
| **3xx Redirection** | | | |
| 301 | Moved Permanently | Resource permanently moved | Old API endpoint redirected to new |
| 302 | Found | Temporary redirect | OAuth redirect to auth server |
| 304 | Not Modified | Resource unchanged (etag match) | Client has latest cached version |
| **4xx Client Error** | | | |
| 400 | Bad Request | Request validation failed | Missing required field, invalid JSON |
| 401 | Unauthorized | Authentication failed/required | Missing or expired bearer token |
| 403 | Forbidden | Authenticated, insufficient permission | User lacks required scope |
| 404 | Not Found | Resource doesn't exist | User ID not found in database |
| 409 | Conflict | Concurrent modification or duplicate | Etag mismatch, duplicate idempotency key |
| 429 | Too Many Requests | Rate limit exceeded | Rate limit: 1000 requests/min exceeded |
| **5xx Server Error** | | | |
| 500 | Internal Server Error | Unexpected server error | Unhandled exception (include trace ID) |
| 503 | Service Unavailable | Dependency down, maintenance | Database unavailable, dependency circuit breaker open |

---

## Domain Error Modelling

### Define Typed Domain Exceptions
```java
// Base domain exception (unchecked)
public abstract class DomainException extends RuntimeException {
    private final String errorCode;

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// Specific domain exceptions
public class UserNotFoundException extends DomainException {
    private final String userId;

    public UserNotFoundException(String userId) {
        super("USER_NOT_FOUND", "User with ID '" + userId + "' not found");
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}

public class InsufficientFundsException extends DomainException {
    private final BigDecimal balance;
    private final BigDecimal requiredAmount;

    public InsufficientFundsException(BigDecimal balance, BigDecimal requiredAmount) {
        super("PAYMENT_INSUFFICIENT_FUNDS",
            "Account balance " + balance + " is less than required " + requiredAmount);
        this.balance = balance;
        this.requiredAmount = requiredAmount;
    }
}

public class InvalidStateTransitionException extends DomainException {
    private final String currentState;
    private final String attemptedState;

    public InvalidStateTransitionException(String resource, String currentState, String attemptedState) {
        super("INVALID_STATE_TRANSITION",
            resource + " cannot transition from " + currentState + " to " + attemptedState);
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }
}
```

### Error Code Naming Convention
Format: `[DOMAIN]_[SPECIFIC_ERROR]`

**Examples:**
```
USER_NOT_FOUND
PAYMENT_INSUFFICIENT_FUNDS
PAYMENT_PROCESSING_FAILED
INVOICE_ALREADY_PAID
SUBSCRIPTION_CANNOT_CANCEL_INACTIVE
RESOURCE_PERMISSION_DENIED
DATABASE_CONSTRAINT_VIOLATION
DUPLICATE_RESOURCE
INVALID_EMAIL_FORMAT
```

---

## Retry Patterns

### When to Retry

**Retry for transient errors (temporary, self-healing):**
- Network timeouts
- Temporary service unavailability (503)
- Database connection refused
- Briefly overloaded service

**Don't retry for:**
- Validation errors (400)
- Permission errors (401, 403)
- Resource not found (404)
- Rate limits (429) - use exponential backoff instead
- Application-level errors (5xx from malformed request)

### Exponential Backoff with Jitter

**Formula:** `delay = min(base * (multiplier ^ attempt) + random_jitter, max_delay)`

```java
@Configuration
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        // Exponential backoff: 1s, 2s, 4s, 8s (max 30s)
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(1000); // 1 second
        policy.setMultiplier(2.0);
        policy.setMaxInterval(30000); // 30 seconds max
        template.setBackOffPolicy(policy);

        // Retry 3 times max
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}

// Usage with Spring Retry
@Service
public class PaymentGatewayClient {
    @Retryable(
        value = {TimeoutException.class, ConnectException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2.0,
            maxDelay = 30000
        )
    )
    public PaymentResult charge(Payment payment) {
        return paymentGateway.charge(payment);
    }

    @Recover
    public PaymentResult recoverCharge(TimeoutException e, Payment payment) {
        logger.error("Payment charge failed after retries", e);
        return PaymentResult.DEFERRED; // Mark for manual retry later
    }
}
```

### Max Attempts
- Default: 3 attempts
- Maximum: 5 (too many retries wastes resources)
- For critical operations (payment): 2-3 attempts
- For non-critical operations (cache fetch): 3-5 attempts

**Total retry time = sum of delays**
- 3 attempts, base 1s, multiplier 2: 1s + 2s + 4s = 7 seconds max
- 5 attempts, base 1s, multiplier 2, max 30s: 1s + 2s + 4s + 8s + 30s = 45 seconds max

---

## Circuit Breaker Pattern

Prevent cascading failures when downstream service is unhealthy.

### States

**Closed State (Normal):**
- Service is healthy
- Requests pass through
- Count failures
- Transition to OPEN if failure threshold exceeded

**Open State (Failing):**
- Service is unhealthy
- All requests fail immediately (fail fast)
- No attempt to call service
- After timeout, transition to HALF_OPEN

**Half-Open State (Recovery):**
- Testing if service recovered
- Allow limited probe requests
- If probes succeed: transition to CLOSED
- If probes fail: transition to OPEN

### Circuit Breaker Configuration

```java
@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreakerFactory circuitBreakerFactory() {
        Resilience4jCircuitBreakerFactory factory = new Resilience4jCircuitBreakerFactory();

        factory.configureDefault(id -> new Resilience4jConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                // Failure rate threshold (5% = 5 out of 100 requests)
                .failureRateThreshold(5.0f)
                // Min requests before evaluating failure rate
                .minimumNumberOfCalls(100)
                // Count only these as failures
                .recordExceptions(TimeoutException.class, ConnectException.class)
                // Ignore these (don't count as failures)
                .ignoreExceptions(ValidationException.class)
                // Time to wait before transitioning to HALF_OPEN
                .waitDurationInOpenState(Duration.ofSeconds(60))
                // Max requests in HALF_OPEN state
                .permittedNumberOfCallsInHalfOpenState(10)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                // Request timeout
                .timeoutDuration(Duration.ofSeconds(2))
                .build())
            .build());

        return factory;
    }
}

// Usage
@Service
public class ExternalService {
    private final CircuitBreakerFactory cbFactory;

    public Result callExternalService(Request request) {
        return cbFactory.create("externalService")
            .run(() -> externalServiceClient.call(request),
                 throwable -> fallback(request));
    }

    private Result fallback(Request request) {
        logger.warn("External service circuit breaker open; using fallback");
        return Result.CACHED_VALUE; // Return stale data or default
    }
}
```

### Configuration Reference

| Parameter | Value | Notes |
|---|---|---|
| `failureRateThreshold` | 5-10% | % of failures to trigger OPEN |
| `minimumNumberOfCalls` | 50-100 | Evaluate after this many calls |
| `waitDurationInOpenState` | 30-60s | How long to wait before HALF_OPEN |
| `permittedNumberOfCallsInHalfOpenState` | 5-10 | Probe requests in HALF_OPEN |
| `recordExceptions` | TimeoutException, ConnectException | Failures to count |
| `ignoreExceptions` | ValidationException | Don't count as failures |
| `timeoutDuration` | 2-5s | Request timeout |

---

## Timeout Hierarchy

Ensure timeouts cascade from client → gateway → service → database.

**Example: Client calls Gateway calls Service calls Database**
```
Client Timeout:     20 seconds (total request time limit)
├─ Gateway Timeout: 15 seconds (includes service time)
│  ├─ Service Timeout: 10 seconds (includes database)
│  │  ├─ Database Query Timeout: 5 seconds
│  │  └─ Buffer: 5 seconds for I/O, GC, etc.
│  └─ Buffer: 5 seconds for response processing
└─ Buffer: 5 seconds for response handling
```

### Timeout Values Table

| Layer | Timeout | Buffer | Total |
|---|---|---|---|
| **Database Query** | 2-5s | - | 2-5s |
| **Service Internal** | 5-10s | 2s | 5-10s |
| **Service → Dependency** | 5-8s | 2s | 7-10s |
| **API Gateway** | 10-15s | 2s | 12-15s |
| **Client (SDK/CLI)** | 15-20s | 3s | 18-20s |

**Rule:** Each layer adds 2-3s buffer for overhead, GC, context switching.

---

## Bulkhead Pattern (Thread Pool Isolation)

Prevent one slow dependency from consuming all request threads.

```java
@Configuration
public class BulkheadConfig {
    // Separate thread pool for payment service calls
    @Bean(name = "paymentThreadPool")
    public Executor paymentThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("payment-");
        return executor;
    }

    // Separate thread pool for email service calls
    @Bean(name = "emailThreadPool")
    public Executor emailThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        return executor;
    }
}

@Service
public class OrderService {
    @Async("paymentThreadPool") // Uses isolated thread pool
    public CompletableFuture<PaymentResult> processPayment(Order order) {
        return CompletableFuture.supplyAsync(() ->
            paymentGateway.charge(order.getAmount())
        );
    }

    @Async("emailThreadPool")
    public CompletableFuture<Void> sendConfirmationEmail(Order order) {
        return CompletableFuture.runAsync(() ->
            emailService.sendOrderConfirmation(order)
        );
    }
}
```

### Connection Pool Sizing

**Formula:** `pool_size = core_count * (1 + wait_time / compute_time)`

For database connections:
- **Wait time:** Database latency (10-100ms typical)
- **Compute time:** Request processing (5-50ms)
- **Ratio:** 1-2 typically
- **Example:** 8 cores, 2.0 ratio = 16 connections

**Recommended sizes:**
- Database pool: 10-25 (depends on throughput)
- HTTP client pool: 50-100 (per service)
- Kafka consumer threads: 4-8 (per topic partition count)

---

## Saga Error Compensation

Handle distributed transaction failures across multiple services.

### Forward Recovery (Happy Path)
When a step fails but subsequent steps can succeed if retried.

```java
@Service
public class OrderSaga {
    // Saga steps with compensations
    public Order processOrder(CreateOrderRequest request) {
        // Step 1: Reserve inventory
        Inventory reservation = inventoryService.reserve(request.getItems());

        try {
            // Step 2: Process payment
            PaymentResult payment = paymentService.charge(request.getAmount());

            // Step 3: Create shipment
            Shipment shipment = shippingService.createShipment(
                request.getAddress(),
                request.getItems()
            );

            // All steps successful
            return new Order(reservation, payment, shipment);

        } catch (PaymentFailedException e) {
            // Compensate: Release inventory reservation
            inventoryService.release(reservation);
            throw e; // Propagate to client
        }
    }
}
```

### Backward Recovery (Compensation)
Explicit compensation transactions to undo already-completed steps.

```java
@Service
public class PaymentSaga {
    @Transactional
    public void processPaymentSaga(String orderId) {
        try {
            // Step 1: Deduct from account
            Account account = accountService.deductAmount(orderId, amount);

            // Step 2: Call external payment processor
            ExternalPaymentId externalId =
                externalPaymentService.processPayment(orderId, amount);

            // Step 3: Record in ledger
            ledgerService.recordTransaction(orderId, externalId, amount);

        } catch (ExternalPaymentFailedException e) {
            logger.error("External payment failed; compensating...", e);

            // Compensate Step 1: Refund amount to account
            accountService.refundAmount(orderId, amount);

            // Compensate Step 2: Cancel external payment (idempotent)
            externalPaymentService.cancelPayment(externalId);

            // Mark saga as failed
            sagaRepository.markFailed(orderId);
            throw e;
        }
    }
}
```

### Compensation Transaction Table

| Step | Operation | Compensation | Idempotent? |
|---|---|---|---|
| 1 | Reserve inventory | Release reservation | Yes (id-based) |
| 2 | Charge payment | Refund charge | Yes (idempotency key) |
| 3 | Create shipment | Cancel shipment | Yes (use status checks) |
| 4 | Update ledger | Reverse ledger entry | Yes (entry id-based) |

---

## Dead Letter Queue Handling

### When to Send to DLQ
- Message fails processing after 3 retries
- Retry backoff exhausted (20+ seconds of retries)
- Poison pill message (causes unhandled exception)
- Message schema incompatible with current service version

```java
@Service
public class EventListener {
    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleUserEvent(UserEvent event, Acknowledgment ack) {
        try {
            notificationService.sendNotification(event);
            ack.acknowledge(); // Mark as processed
        } catch (PermanentException e) {
            // Unrecoverable error; send to DLQ
            logger.error("Unrecoverable error; sending to DLQ", e);
            deadLetterQueue.send(new DeadLetterMessage(event, e));
            ack.acknowledge(); // Don't retry
        } catch (TransientException e) {
            // Transient error; let Kafka retry
            logger.warn("Transient error; will retry", e);
            throw e; // Don't acknowledge; Kafka will retry
        }
    }
}
```

### DLQ Monitoring

**Alert Rules:**
- DLQ message count > 10 in 5 minutes → Page on-call (P2)
- DLQ message count > 100 in 1 hour → Page on-call (P1)
- DLQ not drained after 24 hours → Daily report

### DLQ Replay Procedure

1. **Diagnose the issue:**
   ```bash
   # List messages in DLQ
   kafka-console-consumer --bootstrap-servers localhost:9092 \
     --topic user-events.dlq --from-beginning | jq '.error_reason'
   ```

2. **Fix the underlying issue** (code bug, data format change, dependency issue)

3. **Replay messages:**
   ```java
   @Service
   public class DLQReplayService {
       public void replayFromDLQ() {
           List<DeadLetterMessage> messages =
               dlqRepository.findAll(Status.PENDING);

           for (DeadLetterMessage msg : messages) {
               try {
                   eventProcessor.process(msg.getOriginalEvent());
                   dlqRepository.markReplayed(msg);
               } catch (Exception e) {
                   logger.warn("Replay failed for message {}", msg.getId(), e);
                   // Keep in DLQ for next retry
               }
           }
       }
   }
   ```

---

## Observability for Errors

### Error Rate SLO Targets

| Service Type | P99 Target | Error Budget |
|---|---|---|
| **Critical (payments, auth)** | < 0.5% | 0.5% (3 minutes/month) |
| **High-priority (core features)** | < 1% | 1% (7 minutes/month) |
| **Standard (non-critical)** | < 2% | 2% (14 minutes/month) |

### Alerting Thresholds

```yaml
# Prometheus alerting rules
groups:
  - name: error_alerts
    rules:
      - alert: HighErrorRate
        expr: |
          (rate(http_requests_total{status=~"5.."}[5m]) /
           rate(http_requests_total[5m])) > 0.05
        for: 5m
        annotations:
          severity: page
          description: "{{ $value }}% of requests are errors"

      - alert: DLQBacklog
        expr: kafka_topic_partition_current_offset{topic="*.dlq"} > 100
        for: 15m
        annotations:
          severity: page
          description: "DLQ has {{ $value }} messages"

      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        annotations:
          severity: page
          description: "Circuit breaker {{ $labels.name }} is OPEN"
```

### Runbook Template

```markdown
# High Error Rate Runbook

## Alert: Error rate > 5% for 5 minutes

### Diagnosis
1. Check error rate graph in Grafana (last 1 hour)
2. Query top error codes:
   ```
   topk(5, rate(http_requests_total{status="500"}[5m]))
   ```
3. Check logs for exceptions:
   ```
   level:ERROR service:my-service | stats count by error_code
   ```

### Common Causes
- Database down or slow (query latency > 5s)
- Dependency circuit breaker open
- Memory leak causing OOM errors
- Misconfiguration (wrong DB credentials)

### Recovery
1. Check dependencies: `kubectl get pods -n production`
2. If database issue: check connection pool, restart affected services
3. If memory issue: trigger pod restart or GC
4. Rollback recent deployment if error rate correlated with deploy
```
