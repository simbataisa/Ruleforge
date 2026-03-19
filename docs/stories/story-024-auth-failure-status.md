# Story 024 — AUTH_FAILURE Delivery Status

**Story ID:** story-024
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Backend Engineer
**Priority:** P0 — implement FIRST (prerequisite for Stories 022 and 023)
**Estimate:** 1 story point
**Depends on:** Epic 5 (complete)

---

## User Story

**As a** DevOps engineer monitoring webhook health,
**I want** a distinct `AUTH_FAILURE` delivery status separate from `FAILED`,
**So that** I can immediately distinguish "target endpoint is down" (transient — may recover) from "authentication is broken" (permanent — needs operator action), and not be paged about something that retrying will never fix.

---

## Context

Currently `WebhookDeliveryStatus` has: `PENDING`, `DELIVERING`, `DELIVERED`, `FAILED`, `IN_DLQ`, `CIRCUIT_OPEN`.

Both injector auth failures (missing credential, SSRF block, OAuth token fetch failure) and delivery failures (endpoint 500, network timeout) currently produce `FAILED` → eventually `IN_DLQ`. An operator cannot tell from the delivery log whether retrying would help.

This story adds `AUTH_FAILURE` as a new terminal status (permanent, no retry, does not trip the circuit breaker).

**This story also creates `WebhookAuthException`** — the unchecked exception thrown by all auth injectors when they cannot apply auth. This exception class is needed by Stories 022 and 023, so Story 024 should be implemented first.

---

## Acceptance Criteria

1. `WebhookDeliveryStatus.AUTH_FAILURE` is a valid enum value.
2. `WebhookAuthException` unchecked exception class exists in `io.ruleforge.event.exception`.
3. `WebhookDispatcher.deliverDirect()` catches `WebhookAuthException` and returns `DeliveryResult(success=false, statusCode=0, permanent=true, errorMessage=e.getMessage())` with auth-failure indicator.
4. `OutboxPoller` maps permanent auth failures to `AUTH_FAILURE` delivery status (not `IN_DLQ` or `FAILED`).
5. Circuit breaker logic does NOT increment the consecutive-failure counter for `AUTH_FAILURE` deliveries.
6. `GET /v1/webhooks/{id}/deliveries` returns entries with `status: "AUTH_FAILURE"` in the response.
7. Webhook entity remains `ACTIVE` (not `CIRCUIT_OPEN`) even after 5+ consecutive `AUTH_FAILURE` deliveries.
8. Existing `FAILED` → `IN_DLQ` flow is unchanged for non-auth delivery failures.
9. `cd platform && ./gradlew :ruleforge-event-service:test` passes.

---

## Files to Create

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/exception/WebhookAuthException.java`

```java
package io.ruleforge.event.exception;

/**
 * Thrown by WebhookAuthInjector implementations when authentication cannot be applied.
 * Signals AUTH_FAILURE — permanent, no retry, does not trigger circuit breaker.
 *
 * Examples: blank credential, SSRF-blocked tokenUrl, OAuth2 token endpoint non-2xx.
 */
public class WebhookAuthException extends RuntimeException {
    public WebhookAuthException(String message) {
        super(message);
    }
    public WebhookAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## Files to Modify

### 1. `WebhookDeliveryStatus` enum

Locate the enum (likely `WebhookDeliveryEntity.java` or a standalone enum file) and add:

```java
public enum WebhookDeliveryStatus {
    PENDING,
    DELIVERING,
    DELIVERED,
    FAILED,
    IN_DLQ,
    CIRCUIT_OPEN,
    AUTH_FAILURE   // permanent auth failure — no retry; does not trip circuit breaker
}
```

### 2. `WebhookDispatcher.java`

Add a `boolean authFailure` field to `DeliveryResult` record, and catch `WebhookAuthException`:

```java
public record DeliveryResult(
    boolean success,
    int statusCode,
    String responseBody,
    String payloadJson,
    String errorMessage,
    boolean permanent,
    boolean authFailure   // NEW — true when failure is due to auth setup, not endpoint
) {
    // Convenience constructor for non-auth results (backwards compat):
    public DeliveryResult(boolean success, int statusCode, String responseBody,
                          String payloadJson, String errorMessage, boolean permanent) {
        this(success, statusCode, responseBody, payloadJson, errorMessage, permanent, false);
    }
}
```

Catch `WebhookAuthException` around the injector call:

```java
try {
    injector.injectAuth(headers, decryptedConfig, payloadBytes);
} catch (WebhookAuthException e) {
    log.warn("Auth injection failed for webhook {}: {}", webhook.getId(), e.getMessage());
    return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), true, true);
    //                                                                     perm^  auth^
}
```

### 3. `OutboxPoller.java`

Update terminal status assignment logic:

```java
// After delivery returns permanent=true:
WebhookDeliveryStatus terminalStatus;
if (result.authFailure()) {
    terminalStatus = WebhookDeliveryStatus.AUTH_FAILURE;
    log.warn("Webhook {} auth failure (permanent) — delivery will not be retried: {}",
             webhookId, result.errorMessage());
    // Do NOT increment consecutiveFailures (circuit breaker not applicable)
} else {
    terminalStatus = WebhookDeliveryStatus.IN_DLQ;
    consecutiveFailures++;  // existing circuit breaker logic
}
delivery.setStatus(terminalStatus);
```

Ensure the circuit breaker trip check only evaluates `consecutiveFailures` (which is only incremented for non-auth failures).

---

## Files to Create (Tests)

### Add to existing `OutboxPollerTest` or create `WebhookDispatcherAuthFailureTest.java`

```java
@Test
void authFailure_isNotRetried_andDoesNotTripCircuitBreaker() {
    // Given: injector throws WebhookAuthException
    // When: delivery is attempted 6 times
    // Then: all 6 deliveries are AUTH_FAILURE
    //       webhook status remains ACTIVE (not CIRCUIT_OPEN)
    //       no delivery has status IN_DLQ
}

@Test
void normalFailure_stillTripsCircuitBreaker() {
    // Regression: non-auth failures still increment counter and trip breaker
}

@Test
void authFailureResult_hasCorrectFields() {
    // DeliveryResult with authFailure=true has permanent=true, success=false
}
```

---

## Definition of Done

- [ ] `WebhookAuthException.java` created
- [ ] `WebhookDeliveryStatus.AUTH_FAILURE` added to enum
- [ ] `DeliveryResult` has `authFailure` field with backwards-compat constructor
- [ ] `WebhookDispatcher` catches `WebhookAuthException` → `authFailure=true` result
- [ ] `OutboxPoller` maps auth failures to `AUTH_FAILURE` status (not IN_DLQ)
- [ ] Circuit breaker does NOT trip on `AUTH_FAILURE` deliveries
- [ ] `WebhookDeliveryDto` / API response includes `AUTH_FAILURE` as valid status value
- [ ] Unit tests for circuit breaker exclusion pass
- [ ] `cd platform && ./gradlew :ruleforge-event-service:test` green
