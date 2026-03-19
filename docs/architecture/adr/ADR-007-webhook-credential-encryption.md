# ADR-007: Webhook Credential Encryption Strategy

**Date:** 2026-03-18
**Status:** Accepted
**Author:** Solution Architect (BMAD Epic 5)

---

## Context

RuleForge webhooks now support multiple outbound authentication types: API Key, Basic Auth, Bearer Token, Signing Secret, and future OAuth 2.0. Each type requires storing sensitive credential values (API keys, passwords, tokens) alongside the webhook configuration in PostgreSQL.

The existing `webhooks.secret` column stores HMAC signing secrets as plaintext. This was acceptable for signing secrets (low-value, rotatable), but is not acceptable for API keys and passwords that belong to third-party systems. A database breach would expose all customer credentials in cleartext.

**Constraints:**
- No new infrastructure (no KMS, no HSM) — MVP must use existing stack
- Encryption key must not be stored in the database
- Credentials must be decryptable at runtime by `event-service`
- Credentials must never appear in logs, audit events, or GET API responses
- Java standard library (`javax.crypto`) must be sufficient — no new dependencies

---

## Decision

Store all webhook auth credentials as **AES-256-GCM encrypted JSONB** in a new `auth_config TEXT` column. The encryption key is a 32-byte base64-encoded secret sourced from environment variable `WEBHOOK_CREDENTIAL_KEY`, loaded into Spring via `@Value("${ruleforge.webhook.credential-key}")`.

**Encryption scheme:**
- Algorithm: AES/GCM/NoPadding (256-bit key, 128-bit auth tag)
- IV: 12 bytes, randomly generated per encryption (never reused)
- Storage format: `Base64(IV || ciphertext || GCM_auth_tag)` as a single TEXT column
- Type discriminator: stored alongside as `auth_type` VARCHAR column (not encrypted, needed for routing)

**Decryption:**
- Performed in-memory only within `CredentialEncryptionService`
- Decrypted `WebhookAuthConfig` is passed directly to `WebhookAuthInjector` — never serialized back to DB or logged

---

## Rationale

1. **AES-256-GCM provides confidentiality and integrity**: The GCM auth tag detects tampering. If a ciphertext is corrupted or modified, decryption will fail with `AEADBadTagException` — preventing silent data corruption.

2. **Per-encryption random IV**: Each `encrypt()` call generates a fresh 12-byte IV, so two webhooks with the same credentials produce different ciphertext — prevents frequency analysis attacks.

3. **Type discriminator unencrypted**: `auth_type` (e.g., `API_KEY`, `BASIC_AUTH`) is stored in plaintext. This is intentional: the type is not sensitive (it reveals the pattern, not the credential), and the `OutboxPoller` needs it to select the correct `WebhookAuthInjector` without decrypting first.

4. **Application-layer encryption over DB-level encryption**: PostgreSQL's `pgcrypto` or transparent data encryption (TDE) would protect against disk-level attacks but not application-level credential reads. Application-layer encryption ensures credentials are encrypted even in backups, query logs, and replication streams.

5. **Single column (TEXT)**: A structured multi-column approach (e.g., separate `api_key`, `username`, `password` columns) would reveal the credential schema and leave null columns for unused types. A single encrypted JSON blob is simpler and harder to query without the key.

---

## Consequences

**Positive:**
- Credentials at rest are AES-256 encrypted even if database is compromised
- GCM auth tag prevents silent corruption
- No new infrastructure dependencies
- Encryption/decryption is <1ms overhead

**Negative:**
- **Key management risk**: If `WEBHOOK_CREDENTIAL_KEY` is lost or rotated without re-encrypting existing rows, all credentials become unreadable. Mitigation: document key backup procedure; implement re-encryption script before any key rotation.
- **Key rotation complexity**: Rotating the encryption key requires re-encrypting all `auth_config` rows. This is deferred to v2 with a `WebhookCredentialReEncryptionService`.
- **Cannot query/filter on credential values**: Encrypted JSONB is opaque to SQL. This is fine — no use case requires querying webhook credentials.

---

## Alternatives Considered

### Option 1: PostgreSQL `pgcrypto` symmetric encryption
- **Pros**: Key managed at DB level; no application code needed
- **Rejected**: pgcrypto `pgp_sym_encrypt` uses OpenPGP format (more overhead); key must be passed per query; query logs may contain decryption calls. Worse developer ergonomics.

### Option 2: AWS KMS / HashiCorp Vault envelope encryption
- **Pros**: Enterprise-grade key management, rotation, audit logging
- **Rejected for MVP**: Requires new infrastructure. Deferred to v2 when KMS integration is added. The `CredentialEncryptionService` interface is designed so the implementation can be swapped to KMS-backed encryption without changing callers.

### Option 3: Separate `webhook_credentials` table with column-level encryption
- **Pros**: More normalized; easier to audit which columns hold sensitive data
- **Rejected**: More joins needed in hot delivery path; credential column schema exposes auth type structure; same encryption complexity as chosen approach without benefits.

---

## Assumptions

- `event-service` is the only service that reads or writes `auth_config` — no other service needs access
- `WEBHOOK_CREDENTIAL_KEY` will be provided via Docker/K8s secrets in production, and via `.env.local` in development
- A default insecure key is provided in `application.yml` for local dev only (clearly marked `# CHANGE IN PRODUCTION`)

---

## Review Date

Revisit at v2 epic kickoff: evaluate AWS KMS integration for production key management.
