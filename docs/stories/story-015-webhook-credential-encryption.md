# Story 015 — CredentialEncryptionService + Startup Migration

**Story ID:** story-015
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Backend Engineer
**Priority:** P0 — Sprint 5.1 (security enabler; all auth injection stories depend on this)
**Estimate:** 3 story points
**Depends on:** Story 014 (schema must exist)

---

## User Story

**As a** security-conscious platform,
**I want** all webhook auth credentials encrypted with AES-256-GCM before persistence,
**So that** a database breach does not expose API keys, passwords, or tokens in plaintext.

---

## Background

ADR-007 decided: AES-256-GCM application-layer encryption, key from `WEBHOOK_CREDENTIAL_KEY` env var (32 bytes, base64-encoded), IV random per encryption, stored as `Base64(IV || ciphertext || auth_tag)` in the `auth_config` TEXT column.

This story implements:
1. `WebhookAuthConfig` sealed interface hierarchy (Java 21 sealed records)
2. `CredentialEncryptionService` — encrypt/decrypt
3. `WebhookCredentialMigrationService` — `ApplicationRunner` that encrypts existing plaintext `secret` values into `auth_config` on startup (idempotent)

---

## Acceptance Criteria

1. `WebhookAuthConfig` is a sealed interface with `record` implementations for: `None`, `SigningSecret`, `ApiKey`, `BasicAuth`, `BearerToken`, `CustomHeader`.
2. `CredentialEncryptionService.encrypt(WebhookAuthConfig)` returns a non-null, non-empty Base64 string.
3. `CredentialEncryptionService.decrypt(String encryptedBlob, WebhookAuthType type)` returns the original `WebhookAuthConfig` record equal to the input.
4. Encrypting the same config twice produces **different** ciphertext (random IV per call).
5. Decrypting a tampered ciphertext throws `WebhookCredentialDecryptionException` (custom unchecked exception).
6. `WebhookCredentialMigrationService` (Spring `ApplicationRunner`) on startup:
   - Finds all webhooks where `auth_type = 'SIGNING_SECRET'` AND `auth_config IS NULL` AND `secret IS NOT NULL`
   - Encrypts `secret` value → writes to `auth_config`
   - Does NOT clear `secret` column (deferred to v2)
   - Is idempotent (skips rows where `auth_config` is already set)
   - Logs count of migrated rows at INFO level (not the secrets themselves)
7. `application.yml` has `ruleforge.webhook.credential-key: ${WEBHOOK_CREDENTIAL_KEY:CHANGE_ME_IN_PRODUCTION_32b}` (dev default clearly labelled insecure).
8. Unit tests cover: encrypt→decrypt round-trip, tamper detection, same plaintext → different ciphertext.
9. `./gradlew :ruleforge-event-service:test` passes.

---

## Files to Create

### `io.ruleforge.event.model.WebhookAuthConfig` (sealed interface)

```java
package io.ruleforge.event.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebhookAuthConfig.None.class, name = "NONE"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.SigningSecret.class, name = "SIGNING_SECRET"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.ApiKey.class, name = "API_KEY"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BasicAuth.class, name = "BASIC_AUTH"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BearerToken.class, name = "BEARER_TOKEN"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.CustomHeader.class, name = "CUSTOM_HEADER")
})
public sealed interface WebhookAuthConfig permits
        WebhookAuthConfig.None,
        WebhookAuthConfig.SigningSecret,
        WebhookAuthConfig.ApiKey,
        WebhookAuthConfig.BasicAuth,
        WebhookAuthConfig.BearerToken,
        WebhookAuthConfig.CustomHeader {

    record None() implements WebhookAuthConfig {}
    record SigningSecret(String secret) implements WebhookAuthConfig {}
    record ApiKey(String headerName, String apiKey) implements WebhookAuthConfig {}
    record BasicAuth(String username, String password) implements WebhookAuthConfig {}
    record BearerToken(String token) implements WebhookAuthConfig {}
    record CustomHeader(String headerName, String headerValue) implements WebhookAuthConfig {}
}
```

### `io.ruleforge.event.service.CredentialEncryptionService`

```java
package io.ruleforge.event.service;

@Service
public class CredentialEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecretKeySpec secretKey;
    private final ObjectMapper objectMapper;

    public CredentialEncryptionService(
            @Value("${ruleforge.webhook.credential-key}") String base64Key,
            ObjectMapper objectMapper) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) throw new IllegalArgumentException(
            "WEBHOOK_CREDENTIAL_KEY must be 32 bytes (256-bit) base64-encoded");
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.objectMapper = objectMapper;
    }

    public String encrypt(WebhookAuthConfig config) {
        // 1. Serialize config to JSON bytes
        // 2. Generate 12-byte random IV using SecureRandom
        // 3. AES/GCM/NoPadding cipher, init with ENCRYPT_MODE, IvParameterSpec, GCM_TAG_LENGTH
        // 4. doFinal(plaintext) → ciphertext (includes GCM auth tag appended)
        // 5. Return Base64.getEncoder().encodeToString(concat(iv, ciphertext))
    }

    public WebhookAuthConfig decrypt(String encryptedBlob, WebhookAuthType authType) {
        // 1. Base64 decode → bytes
        // 2. Extract IV (first 12 bytes), ciphertext+tag (remaining)
        // 3. AES/GCM/NoPadding, init with DECRYPT_MODE
        // 4. doFinal → plaintext bytes; throw WebhookCredentialDecryptionException on AEADBadTagException
        // 5. Deserialize JSON using objectMapper.readValue(plaintext, WebhookAuthConfig.class)
    }
}
```

### `io.ruleforge.event.service.WebhookCredentialMigrationService`

```java
package io.ruleforge.event.service;

@Component
@Slf4j
public class WebhookCredentialMigrationService implements ApplicationRunner {

    private final WebhookRepository webhookRepository;
    private final CredentialEncryptionService encryptionService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<WebhookEntity> toMigrate = webhookRepository
            .findByAuthTypeAndAuthConfigIsNullAndSecretIsNotNull(WebhookAuthType.SIGNING_SECRET);

        if (toMigrate.isEmpty()) {
            log.info("Webhook credential migration: no rows to migrate");
            return;
        }

        int count = 0;
        for (WebhookEntity webhook : toMigrate) {
            WebhookAuthConfig config = new WebhookAuthConfig.SigningSecret(webhook.getSecret());
            webhook.setAuthConfig(encryptionService.encrypt(config));
            // Note: webhook.secret NOT cleared here — deferred to v2
            count++;
        }
        webhookRepository.saveAll(toMigrate);
        log.info("Webhook credential migration: encrypted {} signing secrets", count);
    }
}
```

### `io.ruleforge.event.exception.WebhookCredentialDecryptionException`

```java
package io.ruleforge.event.exception;

public class WebhookCredentialDecryptionException extends RuntimeException {
    public WebhookCredentialDecryptionException(String webhookId, Throwable cause) {
        super("Failed to decrypt credentials for webhook " + webhookId, cause);
    }
}
```

---

## Files to Modify

### `platform/ruleforge-event-service/src/main/resources/application.yml`

Add:
```yaml
ruleforge:
  webhook:
    # CHANGE IN PRODUCTION — use WEBHOOK_CREDENTIAL_KEY env var
    # Must be 32-byte base64-encoded AES-256 key
    # Generate: openssl rand -base64 32
    credential-key: ${WEBHOOK_CREDENTIAL_KEY:Y2hhbmdlbWVpbnByb2R1Y3Rpb24zMmJ5dGVz}
```

### `WebhookRepository.java`

Add query method:
```java
List<WebhookEntity> findByAuthTypeAndAuthConfigIsNullAndSecretIsNotNull(WebhookAuthType authType);
```

---

## Test Requirements

### `CredentialEncryptionServiceTest.java`

```java
@Test void encryptThenDecrypt_ApiKey_roundTrip() { ... }
@Test void encryptTwice_sameInput_differentCiphertext() { ... }
@Test void decrypt_tamperedCiphertext_throwsException() { ... }
@Test void decrypt_wrongKeyLength_throwsAtConstruction() { ... }
@Test void encrypt_nullConfig_throwsNPE() { ... }
```

### `WebhookCredentialMigrationServiceTest.java`

```java
@Test void run_migratesUnencryptedSecrets_setsAuthConfig() { ... }
@Test void run_skipsAlreadyMigratedRows() { ... }
@Test void run_noRowsToMigrate_logsInfo_doesNotThrow() { ... }
```

---

## Definition of Done

- [ ] `WebhookAuthConfig` sealed interface with all 6 record types
- [ ] `CredentialEncryptionService` with `encrypt()` and `decrypt()` using AES-256-GCM
- [ ] `WebhookCredentialMigrationService` runs on startup, encrypts existing secrets, is idempotent
- [ ] `WebhookCredentialDecryptionException` created
- [ ] `application.yml` updated with `credential-key` property (insecure dev default clearly labelled)
- [ ] `WebhookRepository` query method added
- [ ] `authConfig` and credential fields excluded from all log statements and `toString()`
- [ ] Unit tests pass: round-trip, tamper detection, idempotency
- [ ] `./gradlew :ruleforge-event-service:test` passes

---

## Notes for Implementer

- Use `SecureRandom` (not `Random`) for IV generation.
- `@JsonSubTypes` with `@JsonTypeInfo` on `WebhookAuthConfig` enables Jackson polymorphic deserialization — test this with all 6 subtypes round-tripped through JSON.
- The `CredentialEncryptionService` is a singleton bean; `SecretKeySpec` is created once at construction. Do not recreate it per call.
- In `WebhookCredentialMigrationService`, wrap the entire migration in `@Transactional` so a partial failure rolls back cleanly.
- The `secret` column is intentionally kept populated after migration — `WebhookDeliveryService` in Story 016 must read from `auth_config` (not `secret`) for new delivery path. The old `secret`-based path can be removed in v2.
