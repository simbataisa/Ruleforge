# Enterprise Architecture — Remaining Design Inputs

## Changelog

- 2026-10-25: Record production-design gaps after completing the approved brownfield inventory.

**Inventory task:** Applied and independently reviewed as a source-derived document.
**Full enterprise architecture mandate:** needs_clarification.

Required input before production architecture can be finalized:

| Missing evidence / decision | Suggested owner |
|---|---|
| BA requirements analysis; workload, business criticality and measurable NFRs | Business Analyst / Product Owner |
| Hosting/provider/region, multi-environment strategy and accountable owners | Enterprise Architect / stakeholders |
| Data classification, regulatory scope, residency and control mapping | Product Owner / InfoSec |
| SLOs/SLIs, alert routing, logs/metrics/tracing platform | EA / operations |
| RTO/RPO, backup and restore verification, DR cadence | EA / operations / Product Owner |
| Cost envelope, capacity model and allocation | EA / budget owner |
| Executable CI/CD, security gates, secrets and deployment controls | DevSecOps |

No ea-done readiness signal is emitted. The ea-blocked signal refers to these design inputs; it does not invalidate the completed onboarding inventory. Solution Architect onboarding may read the evidence inventory, but no production platform choice or implementation kickoff is authorized.
