# Handoff #025 — 2026-04-30

**From:** Tech Lead (`tl`)
**To:** Release Owner (`release-owner`)
**Phase:** Implementation / Release Verification
**Session duration:** ~1 hour

---

## Completed Deliverables

| Artifact | Path | Status |
|----------|------|--------|
| Webhook FK/RBAC/live-smoke fix | `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/WebhookService.java` | ✅ Done |
| Membership lookup | `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/repository/MemberLookupRepository.java` | ✅ Done |
| Space ownership lookup | `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/repository/SpaceLookupRepository.java` | ✅ Done |
| Webhook delivery persistence guard | `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/WebhookDispatcher.java` | ✅ Done |
| Webhook authz HTTP status mapping | `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/controller/WebhookController.java` | ✅ Done |
| Contract coverage | `platform/ruleforge-event-service/src/test/java/io/ruleforge/event/service/WebhookServiceAuthContractTest.java` | ✅ Done |
| Live smoke contract updates | `scripts/smoke-test.sh` | ✅ Done |
| Defect closeout | `docs/testing/bugs/QE-E6-004-webhook-created-by-fk-live-smoke.md` | ✅ Done |

## Key Decisions Made

- **Preserve schema semantics:** `webhooks.created_by` remains a `members.id` FK. Event-service resolves the member id from `(org_id, user_id)` before persistence.
- **Enforce webhook create RBAC in service:** ORG-scope create requires OWNER/ADMIN; create generally requires EDITOR+; SPACE-scope create verifies the space belongs to the authenticated org.
- **Return 403 for authenticated denials:** Webhook controller maps service `AccessDeniedException` to HTTP 403 to match project API conventions.
- **Update smoke expectations to current DTOs:** Smoke now accepts `secretOnCreation`, `successDeliveries`, `deliveryTimeline`, and `[REDACTED]` credential placeholders.

## Verification

```text
./gradlew :ruleforge-event-service:test
BUILD SUCCESSFUL

./gradlew :ruleforge-event-service:bootJar
BUILD SUCCESSFUL

./scripts/smoke-test.sh
PASS: 128  FAIL: 0  SKIP: 1  Total: 129
```

## Open Questions for Next Agent

- [ ] The single smoke skip is cleanup-related: `DELETE /v1/rules/{smoke-test-rule}` returned HTTP 500. It is not part of QE-E6-004, but should be tracked if release policy requires zero skips.

## Risks / Watch-outs

- The local event-service process used for final verification was run in a foreground tool session and stopped after verification. Restart the normal dev stack (`make dev-up` or `./scripts/start-local.sh --skip-build --skip-infra`) before additional live manual testing.

## Starting Point for Next Agent

Read `docs/testing/bugs/QE-E6-004-fix-plan.md` first, then use the verification block above as the release evidence.
