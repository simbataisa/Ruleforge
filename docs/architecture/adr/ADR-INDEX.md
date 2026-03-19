# Architecture Decision Records — Index

| ADR | Title | Date | Status | Epic |
|---|---|---|---|---|
| ADR-001 | Slug Generation Strategy for Spaces — store in DB at write time | 2026-03-14 | Accepted | Epic 1 |
| ADR-002 | ruleCount Computation — real-time SQL COUNT from shared DB | 2026-03-14 | Accepted | Epic 1 |
| ADR-003 | Space-Scoped Rule API — use `/spaces/{id}/rules`, not `/rules?spaceId=` | 2026-03-14 | Accepted | Epic 2 |
| ADR-004 | Move Rule Implementation — dedicated `PATCH /rules/{id}/move` endpoint | 2026-03-14 | Accepted | Epic 2 |
| ADR-005 | Sandbox Space Detection — by `space.name === "Sandbox"` | 2026-03-15 | Accepted | Epic 3 |
| ADR-006 | Assign-vs-Create Separation — remove rule creation from space context | 2026-03-15 | Accepted | Epic 4 |
| ADR-007 | Webhook Credential Encryption — AES-256-GCM with app.yml key | 2026-03-18 | Accepted | Epic 5 |
| ADR-008 | Webhook Auth Injection — Strategy Pattern per auth type | 2026-03-18 | Accepted | Epic 5 |
| ADR-009 | Webhook Delivery Metrics — On-demand SQL aggregate | 2026-03-18 | Accepted | Epic 5 |
| ADR-010 | OAuth 2.0 Token Caching — Redis string with TTL, SET NX PX | 2026-03-19 | Accepted | Epic 6 |
