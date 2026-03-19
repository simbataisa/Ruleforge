# Story 022 — Custom Header Auth Injector

**Story ID:** story-022
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Backend Engineer
**Priority:** P0
**Estimate:** 1 story point
**Depends on:** Story 024 (AUTH_FAILURE status — implement 024 first)

---

## User Story

**As an** organization owner or editor,
**I want to** configure "Custom Header" as the webhook auth type with a header name and value,
**So that** RuleForge can deliver events to APIs that use arbitrary proprietary authentication headers (e.g., `X-Tenant-Token`, `X-Service-Key`).

---

## Context

The `WebhookAuthType.CUSTOM_HEADER` enum value and `WebhookAuthConfig.CustomHeader(headerName, headerValue)` record already exist. The `WebhookAuthInjectorRegistry` auto-discovers Spring `@Component` beans that implement `WebhookAuthInjector`. There is no `CustomHeaderInjector` bean — so `CUSTOM_HEADER` webhooks silently fall back to `NoneAuthInjector`, sending no auth header.

This story adds the single missing `CustomHeaderInjector` class. No sealed interface changes, no DB changes, no DTO changes.

**Pattern reference:** `ApiKeyAuthInjector` and `BasicAuthInjector` in `io.ruleforge.event.delivery` — same structure.

By the time `injectAuth` is called, `WebhookDispatcher` has already called `CredentialEncryptionService.decryptAuthConfig(config)` — so `config.headerValue()` arrives as plaintext.

---

## Acceptance Criteria

1. `CustomHeaderInjector.supportedType()` returns `WebhookAuthType.CUSTOM_HEADER`.
2. When `config` is `WebhookAuthConfig.CustomHeader` with non-blank `headerName` and `headerValue`, the outbound `HttpHeaders` gains entry `{headerName}: {headerValue}`.
3. When `headerName` is null or blank, the header name defaults to `"X-Custom-Header"`.
4. When `headerValue` is null or blank, `CustomHeaderInjector` throws `WebhookAuthException("CUSTOM_HEADER headerValue is blank")`.
5. The thrown `WebhookAuthException` causes `WebhookDispatcher` to return a `DeliveryResult` with `success=false`, `permanent=true`, and `AUTH_FAILURE` status (handled by existing `WebhookDispatcher` auth-failure path from Story 024).
6. `WebhookAuthInjectorRegistry.getFor(CUSTOM_HEADER)` returns `CustomHeaderInjector` (not the NONE fallback).
7. No `Authorization` header is added — only the custom-named header.
8. `cd platform && ./gradlew :ruleforge-event-service:test` passes — all existing injector tests still green.

---

## Files to Create

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/delivery/CustomHeaderInjector.java`

```java
package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class CustomHeaderInjector implements WebhookAuthInjector {

    @Override
    public WebhookAuthType supportedType() {
        return WebhookAuthType.CUSTOM_HEADER;
    }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.CustomHeader ch) {
            if (ch.headerValue() == null || ch.headerValue().isBlank()) {
                throw new WebhookAuthException("CUSTOM_HEADER headerValue is blank or missing");
            }
            String name = (ch.headerName() != null && !ch.headerName().isBlank())
                    ? ch.headerName()
                    : "X-Custom-Header";
            headers.set(name, ch.headerValue());
        }
    }
}
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/exception/WebhookAuthException.java`

```java
package io.ruleforge.event.exception;

/**
 * Thrown by WebhookAuthInjector implementations when authentication cannot be
 * applied (missing credential, SSRF block, token fetch failure). Results in
 * AUTH_FAILURE delivery status — permanent, no retry.
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

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/WebhookDispatcher.java`

Add `WebhookAuthException` catch block around the injector call (required by Story 024, but wired here if 024 is done first):

```java
// Existing call site — wrap with try/catch:
try {
    injector.injectAuth(headers, decryptedConfig, payloadBytes);
} catch (WebhookAuthException e) {
    log.warn("Auth injection failed for webhook {}: {}", webhook.getId(), e.getMessage());
    return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), true);
    //                                                                        ^^^^ permanent=true
}
```

*(If Story 024 is done first, this catch block already exists — no change needed.)*

---

## Files to Create (Tests)

### `platform/ruleforge-event-service/src/test/java/io/ruleforge/event/delivery/CustomHeaderInjectorTest.java`

```java
package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.*;

class CustomHeaderInjectorTest {

    private final CustomHeaderInjector injector = new CustomHeaderInjector();

    @Test
    void supportedType_isCustomHeader() {
        assertThat(injector.supportedType()).isEqualTo(WebhookAuthType.CUSTOM_HEADER);
    }

    @Test
    void injectAuth_addsConfiguredHeader() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("X-Service-Token", "secret123"), new byte[0]);
        assertThat(headers.getFirst("X-Service-Token")).isEqualTo("secret123");
    }

    @Test
    void injectAuth_defaultsHeaderNameWhenBlank() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("", "myvalue"), new byte[0]);
        assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("myvalue");
    }

    @Test
    void injectAuth_defaultsHeaderNameWhenNull() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader(null, "myvalue"), new byte[0]);
        assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("myvalue");
    }

    @Test
    void injectAuth_throwsAuthException_whenHeaderValueBlank() {
        assertThatThrownBy(() ->
            injector.injectAuth(new HttpHeaders(), new WebhookAuthConfig.CustomHeader("X-Hdr", ""), new byte[0])
        ).isInstanceOf(WebhookAuthException.class)
         .hasMessageContaining("headerValue");
    }

    @Test
    void injectAuth_throwsAuthException_whenHeaderValueNull() {
        assertThatThrownBy(() ->
            injector.injectAuth(new HttpHeaders(), new WebhookAuthConfig.CustomHeader("X-Hdr", null), new byte[0])
        ).isInstanceOf(WebhookAuthException.class);
    }

    @Test
    void injectAuth_doesNotAddAuthorizationHeader() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("X-Custom", "value"), new byte[0]);
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }
}
```

---

## Definition of Done

- [ ] `CustomHeaderInjector.java` created with `@Component` annotation
- [ ] `WebhookAuthException.java` created (if not already done by Story 024)
- [ ] `WebhookAuthInjectorRegistry` auto-discovers `CustomHeaderInjector` (no manual registration — it's Spring autowired)
- [ ] `WebhookDispatcher` catches `WebhookAuthException` and returns `permanent=true` result
- [ ] `CustomHeaderInjectorTest` passes with all 7 test cases
- [ ] `cd platform && ./gradlew :ruleforge-event-service:test` green
- [ ] No changes to existing injectors, sealed interface, DTOs, or DB schema
