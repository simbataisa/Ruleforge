# Webhooks Enhancement — End-to-End Verification

## Prerequisites
- Local stack running: `./scripts/start-local.sh`
- All services healthy: `./scripts/smoke-test.sh --quick`
- Redis running (required for rate limiting): check with `docker ps | grep redis`

## Layer 1 & 2: Outbox + Multi-Scope

### DB Migration Verification
1. Run `make migrate` — V048 applies without errors
2. Check `webhooks` table: `docker exec -i docker-postgres-1 psql -U ruleforge -d ruleforge -c "\d webhooks"` — should show `scope_type`, `scope_id`, `org_id`, `consecutive_failures`, `circuit_opened_at` columns
3. Verify event-service applies V014: look for `"Successfully applied 1 migration to schema 'public'"` in event-service startup logs

### Transactional Outbox
4. Trigger a rule execution event (e.g., call `POST /v1/solve`)
5. Check `webhook_outbox` table for pending entries within 1s
6. Check `webhook_deliveries` after poller runs (within 5s) — entry should appear

## Layer 3: Security Hardening

### SSRF Protection
7. `curl -X POST http://localhost:8084/v1/webhooks -H "Authorization: Bearer $TOKEN" -d '{"url":"http://127.0.0.1/hook","events":["rule.published"],"scopeType":"ORG","scopeId":"..."}'`
   - Expected: 400 Bad Request with message about blocked address

### Replay-Safe Signing
8. Create a webhook, trigger a test delivery, inspect delivery headers in `webhook_deliveries.request_headers`
   - Should contain: `X-Webhook-Signature`, `X-Webhook-Timestamp`, `X-Webhook-Delivery-Id`, `X-Webhook-Id`, `X-Webhook-Schema-Version`, `X-Ruleforge-Event`

### Secret Rotation
9. `POST /v1/webhooks/{id}/rotate-secret` — response should contain new `secret` field
10. Old secret should no longer validate delivery signatures (verify by checking signature mismatch if you have a test receiver)

## Layer 4: Frontend

### Create Webhook (2-step)
11. Navigate to `http://localhost:3000/webhooks`
12. Click "Add Webhook" — scope picker cards (ORG / SPACE / FOLDER / RULE) should appear as Step 1
13. Select SPACE, choose a space, fill in URL + events
14. Submit — one-time secret banner should appear with copy button
15. Close modal — secret should NOT be accessible again (no persistent storage)

### Detail Page
16. Click any webhook row — should navigate to `/webhooks/:id`
17. Verify 4 tabs: Overview, Delivery Logs, Security, Settings
18. Click "Send Test" — delivery should appear in Delivery Logs tab (may need to refresh)
19. In Security tab — click "Rotate Secret" — confirm dialog should appear, then new secret shown

### Circuit Breaker Simulation
20. Create webhook pointing to `https://httpbin.org/status/500`
21. Trigger 5+ rule events that match the webhook scope
22. After 5 consecutive DLQ failures: check `webhooks.active=false` in DB
23. In frontend: circuit breaker banner should appear in `WebhookDetailPage` header
24. Click "Reset Circuit" — webhook re-enabled, `active=true` in DB

## Rollback
If V048 needs reverting:
```sql
-- Restore space_id from scope_id (for SPACE-scoped webhooks only)
ALTER TABLE webhooks ADD COLUMN space_id UUID;
UPDATE webhooks SET space_id = scope_id WHERE scope_type = 'SPACE';
ALTER TABLE webhooks DROP COLUMN scope_type, DROP COLUMN scope_id, DROP COLUMN org_id,
  DROP COLUMN name, DROP COLUMN consecutive_failures, DROP COLUMN circuit_opened_at,
  DROP COLUMN last_delivered_at;
```
Note: ORG/FOLDER/RULE scoped webhooks would be lost on rollback.
