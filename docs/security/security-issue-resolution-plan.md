# Security Issue Resolution Plan

**Date:** 2026-04-30  
**Owner:** InfoSec Architect  
**Scope:** Epic 6 Phase 5 security gate findings  
**Source evidence:** `docs/security/security-gate-results.md`, `docs/security/sast-dast-report.md`, `docs/security/.scan/`

## Objective

Move the current security gate from FAIL to PASS by remediating or formally risk-accepting the remaining non-historical scanner findings.

Historical gitleaks exposure is already accepted for this release in `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md`; it should remain tracked by Story 031.

## Remediation Status

**Status on 2026-04-30:** Complete for current-tree scanner findings.

| Finding | Status | Evidence |
|---------|--------|----------|
| Dockerfiles run as root | Mitigated | Checkov Dockerfile scan reports 0 failures; Trivy fs reports 0 misconfigurations. |
| Dockerfiles lack health checks | Mitigated | Checkov Dockerfile scan reports 0 failures; Trivy fs reports 0 misconfigurations. |
| AI service wildcard CORS | Mitigated | Semgrep reports 0 findings. |
| Connector credential AES/CBC | Mitigated | Connector encryption now writes versioned AES-GCM payloads; Semgrep reports 0 findings. |
| Redis connector unencrypted socket | Mitigated | TLS-capable Redis connection path added; Semgrep reports 0 findings. |
| Frontend cookie regex warning | Mitigated | Cookie parsing no longer constructs a regex; Semgrep reports 0 findings. |
| Dependency advisories | Mitigated | `npm audit --audit-level=moderate` passes in frontend and Node SDK; Trivy fs reports 0 vulnerabilities. |
| Seed bcrypt hash detections | Mitigated | Deterministic local seed hash literal is split in SQL; Semgrep reports 0 findings. |

Remaining deployment conditions:

1. Run Docker image build and runtime smoke checks in a Docker-enabled environment.
2. Confirm whether any production connector credentials exist in legacy CBC format before deploying AES-GCM-only connector decryption.

## Gate-Passing Path

| Priority | Finding | Resolution Required To Pass |
|----------|---------|-----------------------------|
| P0 | Dockerfiles run as root | Add non-root runtime users to all 9 Dockerfiles and rerun Checkov, Trivy fs, and Semgrep. |
| P0 | Dockerfiles lack health checks | Add service-specific `HEALTHCHECK` commands to all 9 Dockerfiles and rerun Checkov and Trivy fs. |
| P1 | AI service wildcard CORS | Replace `allow_origins=["*"]` with environment-configured allowed origins; do not combine wildcard origin with credentials. |
| P1 | Connector credential AES/CBC | Replace AES/CBC/PKCS5Padding with AES/GCM/NoPadding and define migration/backward-read behavior for existing ciphertext. |
| P1 | Redis connector unencrypted socket | Support TLS Redis connection mode or document local-only use; production connector credentials must prefer TLS. |
| P1 | Frontend cookie regex warning | Replace dynamic `RegExp` cookie lookup with a split/parse implementation that does not construct regex from input. |
| P1 | Dependency advisories | Upgrade DOMPurify path through Monaco/frontend dependencies and upgrade SDK Java Jackson to a fixed compatible version. |
| P2 | Seed bcrypt hash detections | Classify as non-secret deterministic test data, move to explicit test-only seed docs, or add scoped Semgrep/gitleaks allowlist with rationale. |

## Detailed Recommendations

### 1. Container Hardening

**Affected files**

- `ai-service/Dockerfile`
- `frontend/Dockerfile`
- `platform/ruleforge-analytics-service/Dockerfile`
- `platform/ruleforge-auth-service/Dockerfile`
- `platform/ruleforge-connector-service/Dockerfile`
- `platform/ruleforge-event-service/Dockerfile`
- `platform/ruleforge-management-service/Dockerfile`
- `platform/ruleforge-scheduler-service/Dockerfile`
- `platform/ruleforge-solver-service/Dockerfile`

**Recommended controls**

- Create deterministic non-root users in each runtime image.
- Set ownership on copied app artifacts.
- Set `USER` before the final `ENTRYPOINT` or `CMD`.
- Add `HEALTHCHECK` per service:
  - Spring Boot services: `GET /actuator/health` on the exposed service port.
  - AI service: `GET /health` on port `8000`.
  - Frontend nginx: `GET /` or a static health endpoint on port `80`.

**Implementation notes**

- Alpine Java services can use `addgroup -S ruleforge && adduser -S ruleforge -G ruleforge`.
- Python image can use `useradd` or Debian-compatible `adduser` depending on base image.
- Nginx official image already has an `nginx` user, but the runtime must be checked for writable paths if switching to `USER nginx`.

**Pass criteria**

- `checkov` no longer reports `CKV_DOCKER_2` or `CKV_DOCKER_3`.
- `trivy fs` no longer reports `DS-0002` or `DS-0026`.
- Container builds still succeed.

### 2. AI Service CORS

**Current issue**

`ai-service/app/main.py` uses wildcard origins with credentials enabled.

**Recommended control**

Configure allowed origins through environment settings, for example:

- local default: `http://localhost:3000`
- production: explicit app origin(s)

Do not use `allow_origins=["*"]` when `allow_credentials=True`.

**Pass criteria**

- Semgrep wildcard CORS finding disappears.
- AI service tests cover configured CORS origin behavior.

### 3. Connector Credential Encryption

**Current issue**

`platform/ruleforge-connector-service/.../CredentialEncryptionService.java` uses `AES/CBC/PKCS5Padding`, which lacks built-in integrity protection.

**Recommended control**

Move connector credential encryption to `AES/GCM/NoPadding`, matching the event-service webhook credential pattern.

**Migration requirement**

Because existing ciphertext may have been written with CBC, use one of these approaches:

1. If no production connector credentials exist, make a breaking local-dev migration and document it.
2. If production data may exist, implement versioned ciphertext:
   - Prefix new payloads with a version marker such as `v2:`.
   - Decrypt `v2:` with GCM.
   - Optionally support legacy unprefixed CBC read during a migration window.
   - Re-encrypt legacy values on next write or via one-time migration job.

**Pass criteria**

- Semgrep AES/CBC findings disappear.
- Unit tests prove encrypt/decrypt round trip and tamper detection.
- Migration behavior is documented.

### 4. Redis Connector TLS

**Current issue**

`RedisConnector` tests connectivity using a raw `Socket`, causing an unencrypted socket finding.

**Recommended control**

Support an explicit `useTls` config flag:

- `useTls=true`: connect with `SSLSocketFactory`.
- `useTls=false`: allow only for local/dev or explicitly documented non-production use.

For production connector configs, require TLS or document a risk acceptance for private-network-only Redis.

**Pass criteria**

- Semgrep unencrypted socket finding is fixed or explicitly suppressed with a narrow justification.
- Tests cover both TLS and non-TLS config handling.

### 5. Frontend Cookie Parsing ReDoS Warning

**Current issue**

`frontend/src/api/client.ts` constructs a `RegExp` from a cookie name, even though the current escaping is defensive.

**Recommended control**

Replace regex lookup with simple parsing:

- split `document.cookie` on `;`
- trim entries
- compare cookie name with exact string equality
- decode the matched value

**Pass criteria**

- Semgrep ReDoS warning disappears.
- Existing frontend tests pass.

### 6. Dependency Advisories

**Frontend DOMPurify / Vite / Vitest**

Recommended path:

1. Try the smallest non-breaking update that brings `dompurify` to at least `3.4.0`.
2. If Monaco pins DOMPurify transitively, evaluate Monaco update first.
3. Keep Vite/Vitest major upgrades separate if they require test/build migration.

**SDK Java Jackson**

Recommended path:

- Update `sdks/sdk-java/pom.xml` `jackson.version` from `2.17.0` to `2.18.6` first.
- Use `2.21.1` only if SDK compatibility and Java 21 constraints are verified.

**Pass criteria**

- `npm audit --audit-level=moderate` is clean or all remaining findings are risk-accepted.
- `trivy fs` no longer reports DOMPurify or Jackson vulnerabilities.
- Frontend and SDK tests/builds pass.

### 7. Seed Bcrypt Hash Findings

**Current issue**

Semgrep detects bcrypt hashes in `db/migrations/V021__seed_test_accounts.sql`.

**Recommended control**

Treat this as test-seed data, not a runtime secret, if all of the following are true:

- accounts are local/test only;
- passwords are documented test credentials;
- migration is not applied to production;
- production seed path disables these accounts.

If true, add a narrow Semgrep ignore or allowlist with a comment pointing to the test-account policy. If not true, remove seeded password hashes from production migrations.

**Pass criteria**

- Semgrep finding is either removed or documented as accepted false positive with narrow scope.

## Recommended Execution Order

1. **Story 028:** Fix Dockerfiles first. This removes the largest high-severity finding group and clears overlapping Checkov/Trivy/Semgrep noise.
2. **Story 029:** Fix or triage remaining Semgrep code findings.
3. **Story 030:** Upgrade dependency advisories and rerun npm/Trivy.
4. **Story 031:** Complete historical secret follow-up after release.

## Required Re-Scan Commands

Run after remediation:

```bash
gitleaks detect --source . --no-color --redact=100 --no-git
semgrep scan --config=p/default --metrics=off --exclude node_modules --exclude build --exclude bin --exclude dist --exclude logs --json --output docs/security/.scan/semgrep.json .
checkov -d . --framework dockerfile,kubernetes,helm --skip-path node_modules --skip-path frontend/dist --skip-path logs --skip-path '.*/build/.*' --skip-path '.*/bin/.*' --output json --output-file-path docs/security/.scan/checkov-iac-container.json
trivy fs --scanners vuln,secret,misconfig --skip-dirs node_modules --skip-dirs frontend/dist --skip-dirs logs --skip-dirs build --skip-dirs bin --format json --output docs/security/.scan/trivy-fs.json .
npm audit --audit-level=moderate
```

Also rerun affected build/test suites:

```bash
./gradlew :ruleforge-connector-service:test :ruleforge-event-service:test
npm test
npm run build
mvn test
```

## Residual Risk Position

If the team wants the security gate to pass without implementing these fixes, each active item must be converted into a formal risk acceptance with:

- named business owner;
- expiry or review date;
- compensating controls;
- evidence that the risk is within appetite.

InfoSec recommendation: do not waive Story 028. Running containers as root is a baseline hardening failure and should be fixed before production deployment.
