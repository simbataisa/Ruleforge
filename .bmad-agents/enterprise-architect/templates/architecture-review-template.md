# Enterprise Architecture Review Template

**Solution Name:** [Project/Solution Name]
**Review Date:** [YYYY-MM-DD]
**Review Duration:** [X hours, e.g., 2 days]
**Reviewing Architects:** [Names and titles]
**Document ID:** [ARCH-REVIEW-XXXX]
**Next Review Date:** [YYYY-MM-DD] (if approved; before go-live if conditional)

---

## Executive Summary

**Recommendation:** [APPROVE | APPROVE WITH CONDITIONS | REJECT | DEFER]

**Summary (2-3 sentences):**
[Concise statement of recommendation and key reason. Examples:
- "APPROVE: Solution well-designed, aligned with enterprise architecture, meets all critical requirements."
- "APPROVE WITH CONDITIONS: Solution sound but requires remediation of 3 security findings before go-live."
- "REJECT: Violates enterprise data architecture standards; proposes two-database pattern that contradicts MDM strategy."
]

**Key decision:**
[One-sentence decision statement. Example: "Proceed to development with conditions on data governance and authentication. Expected go-live Q2 2024."]

---

## Solution Overview

**Problem statement:**
[What business problem does this solution solve? What are the pain points?]

Example: "Current order fulfillment process is manual; 40% of orders have errors. Customers wait 5-7 days for fulfillment. Competitors promise 2-day fulfillment. Solution: implement Order Fulfillment Platform to automate end-to-end fulfillment."

**Solution at a glance:**
[Brief description of proposed solution architecture. 3-5 sentences.]

Example: "Implement new Order Fulfillment Platform built on microservices architecture. Components: Order Service (manages order state), Inventory Service (reserves stock), Fulfillment Service (orchestrates warehouse operations), Shipment Service (coordinates carriers). Services communicate via event-driven architecture (Kafka). All services containerised on Kubernetes. Expected to reduce fulfillment time to 24 hours."

**Scope:**
- In scope: [List major components and capabilities being built]
- Out of scope: [List items explicitly excluded; planned for future phases]

**Timeline:**
- Design phase: [Dates]
- Development: [Dates]
- Testing: [Dates]
- Go-live: [Target date]
- Post-launch support: [Duration]

**Investment:**
- Estimated budget: [Amount]
- Budget status: [On track / Over budget / Under budget]
- Funding approved: [Yes / No]

**Links to design documents:**
- [Link to architecture document]
- [Link to design specs]
- [Link to deployment plan]
- [Link to security assessment]

---

## Architecture Review Criteria

**Instructions:** For each criterion, evaluate the solution and mark: PASS | FAIL | PARTIAL | N/A

### Strategic Alignment

**Criterion:** Does the solution align with enterprise strategy, roadmap, and target architecture?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Alignment with 3-year IT roadmap | PASS / FAIL / PARTIAL | [Specific comments. Example: "Aligns with 'cloud-first' strategy. All infrastructure on AWS. Kubernetes deployment fits container strategy."] |
| Alignment with target architecture | PASS / FAIL / PARTIAL | [Example: "Uses microservices pattern aligned with target. Event-driven integration aligns with API-first strategy."] |
| Fits within business strategy | PASS / FAIL / PARTIAL | [Example: "Directly supports 'customer obsession' strategy by reducing fulfillment time."] |
| Portfolio fit (avoids redundancy) | PASS / FAIL / PARTIAL | [Example: "Replaces legacy fulfillment system. No overlap with other initiatives. Clear migration path."] |
| Approval from business sponsor | PASS / FAIL / PARTIAL | [Example: "VP Operations has signed off; Business case ROI approved."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Summary of strengths and concerns regarding strategic alignment]

**Remediation (if FAIL or PARTIAL):**
- [Required action to achieve PASS]

---

### Standards Compliance

**Criterion:** Does the solution follow approved technology standards and enterprise policies?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Technology stack approval | PASS / FAIL / PARTIAL | [Check against approved tech list. Example: "Python/FastAPI on approved stack. PostgreSQL approved. Docker/Kubernetes on approved infrastructure list."] |
| Naming conventions | PASS / FAIL / PARTIAL | [Example: "Service names follow kebab-case standard (order-service, inventory-service). Database schemas follow approved naming."] |
| Code quality standards | PASS / FAIL / PARTIAL | [Example: "Commits to use SonarQube for static analysis. Code coverage target 80%."] |
| Deployment standards | PASS / FAIL / PARTIAL | [Example: "Deployments via Kubernetes. Blue-green deployment strategy per standard."] |
| Documentation standards | PASS / FAIL / PARTIAL | [Example: "Architecture docs follow C4 model. API docs in OpenAPI 3.0 format."] |
| Change management process | PASS / FAIL / PARTIAL | [Example: "CAB review scheduled for all environment promotions. Change windows aligned with maintenance windows."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Summary of standards compliance]

**Remediation (if FAIL or PARTIAL):**
- [Specific standards that must be met; by what date]

---

### Security and Compliance

**Criterion:** Does the solution meet security requirements and regulatory compliance obligations?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Data classification and protection | PASS / FAIL / PARTIAL | [Example: "Handles customer PII (name, address, email); correctly classified as CONFIDENTIAL. Encryption at rest (AES-256) and in transit (TLS 1.3) implemented. Data retention policy 7 years per compliance requirement."] |
| Authentication and authorisation | PASS / FAIL / PARTIAL | [Example: "OAuth2 / OIDC to corporate identity provider. RBAC implemented. Service-to-service mTLS. No hardcoded credentials."] |
| Audit logging and compliance | PASS / FAIL / PARTIAL | [Example: "All state changes logged in immutable audit log. PII masked in logs. Logs retained 7 years. Audit log accessible for compliance audits."] |
| Vulnerability management | PASS / FAIL / PARTIAL | [Example: "Code scanned with SAST tools (SonarQube). Dependencies scanned with DAST tools (Snyk). Zero critical vulnerabilities. Known issues have mitigation plans."] |
| Security testing | PASS / FAIL / PARTIAL | [Example: "Pen testing completed by external firm. No critical findings. All recommendations addressed."] |
| Regulatory requirements | PASS / FAIL / PARTIAL | [Example: "Complies with SOC 2 Type II for service availability. GDPR-compliant (data subject rights, DPA with cloud provider). CCPA-compliant (consumer privacy disclosures)."] |
| Third-party risk | PASS / FAIL / PARTIAL | [Example: "Uses AWS services (approved vendor). SaaS dependencies on Zendesk (BAA signed). All vendors on approved vendor list."] |
| Secrets management | PASS / FAIL / PARTIAL | [Example: "All secrets in HashiCorp Vault. Rotated every 90 days. No hardcoded secrets in code. Pre-commit hooks block secret commits."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Summary of security posture]
- [Any critical vulnerabilities found]
- [Compliance gaps]

**Remediation (if FAIL or PARTIAL):**
- [Security findings with severity, due date, and owner]

Example:
- Critical: Implement TLS 1.3 on all service-to-service communication (due before go-live)
- Major: Conduct pen testing; address findings (due 2 weeks before go-live)
- Minor: Add PII masking to logs (due within 1 month of go-live)

---

### Integration Architecture

**Criterion:** Does the solution use approved integration patterns? Are APIs well-designed and contracts clear?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Integration pattern alignment | PASS / FAIL / PARTIAL | [Example: "Uses async events via Kafka for cross-service integration. Aligns with approved event-driven pattern. No point-to-point calls."] |
| API standards compliance | PASS / FAIL / PARTIAL | [Example: "All APIs RESTful with OpenAPI 3.0 documentation. Rate limiting (100 RPS per client). Versioning strategy (v1, v2 in URL path)."] |
| Event contract definition | PASS / FAIL / PARTIAL | [Example: "All events registered in central event catalogue. JSON schema defined for each event. Versioning strategy (major.minor) for events."] |
| Tight coupling assessment | PASS / FAIL / PARTIAL | [Example: "Services loosely coupled via events. No service calls service A → B → C chains (would create tight coupling)."] |
| Third-party integration | PASS / FAIL / PARTIAL | [Example: "Integrations with external services (payment, shipping) via Anti-Corruption Layer. External contract changes don't affect internal domain model."] |
| Integration testing | PASS / FAIL / PARTIAL | [Example: "Contract testing in place (consumer-driven contracts using Pact). Integration tests verify event formats."] |
| Backward compatibility | PASS / FAIL / PARTIAL | [Example: "API versioning allows consumers to upgrade at their own pace. Old API versions supported for 12 months."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Summary of integration architecture strength]
- [Any coupling concerns]
- [API quality assessment]

**Remediation (if FAIL or PARTIAL):**
- [Integration improvements required]

---

### Data Architecture

**Criterion:** Does the solution follow data governance standards? Is data ownership clear? Master data managed correctly?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Data ownership clarity | PASS / FAIL / PARTIAL | [Example: "Order Service owns order data. Customer Service owns customer master data. Clear ownership defined."] |
| Master data management | PASS / FAIL / PARTIAL | [Example: "Customer master (MDM) owned by Customer Service. Order Service references customer ID only, never duplicates customer data."] |
| Data governance compliance | PASS / FAIL / PARTIAL | [Example: "Data classification defined. Sensitivity levels applied. Data dictionary updated. Lineage tracked in data lake."] |
| Database per service approach | PASS / FAIL / PARTIAL | [Example: "Each service has own database (PostgreSQL). No cross-service direct DB access. Cross-service queries via APIs and denormalised read models."] |
| Data redundancy evaluation | PASS / FAIL / PARTIAL | [Example: "Some customer data denormalised in Order Service for performance (read-only cache). Justified; eventual consistency acceptable."] |
| PII handling | PASS / FAIL / PARTIAL | [Example: "PII (name, email, address) identified. Encrypted at rest, masked in logs. GDPR data subject rights implemented (export, deletion, portability)."] |
| Data retention and deletion | PASS / FAIL / PARTIAL | [Example: "Active orders retained indefinitely. Cancelled orders retained 7 years (compliance). Audit logs retained 7 years. Deletion process defined and tested."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Data governance posture]
- [Any data duplication or ownership issues]
- [Master data management assessment]

**Remediation (if FAIL or PARTIAL):**
- [Data architecture improvements required; e.g., "Implement MDM for customer data by Q2 2024"]

---

### Operational Readiness

**Criterion:** Can operations support this system in production? Are runbooks, monitoring, and incident response defined?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Observability implementation | PASS / FAIL / PARTIAL | [Example: "Metrics: Prometheus. Logs: ELK. Traces: Jaeger. Key metrics dashboards built. Alerting thresholds defined."] |
| Monitoring and alerting | PASS / FAIL / PARTIAL | [Example: "Alerts for P99 latency > 500ms, error rate > 5%, pod restart count. PagerDuty integration for on-call routing."] |
| Runbook documentation | PASS / FAIL / PARTIAL | [Example: "Runbooks written for: service restart, database failover, Kafka topic recovery. All stored in wiki; accessible to ops team."] |
| SLA definition | PASS / FAIL / PARTIAL | [Example: "API availability SLA: 99.9%. P99 latency SLA: < 200ms. Monitored and reported monthly."] |
| Incident response plan | PASS / FAIL / PARTIAL | [Example: "Incident response playbook defined. On-call rotation (PagerDuty). Escalation paths clear. Post-mortem process defined."] |
| Disaster recovery plan | PASS / FAIL / PARTIAL | [Example: "RTO: 15 minutes. RPO: < 1 hour. Daily incremental backups, weekly full backups. Replicated to secondary region. Tested quarterly."] |
| Capacity planning | PASS / FAIL / PARTIAL | [Example: "Load testing completed; autoscaling configured. CPU threshold 70% for scale-out. Baseline capacity supports 10x current load."] |
| Infrastructure automation | PASS / FAIL / PARTIAL | [Example: "Infrastructure as Code (Terraform). Deployments automated (GitOps). No manual infrastructure changes."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Operational readiness assessment]
- [Any gaps in monitoring, runbooks, or disaster recovery]

**Remediation (if FAIL or PARTIAL):**
- [Operational improvements; include due dates]

---

### Cost and Efficiency

**Criterion:** Is the solution cost-effective? Are licensing, cloud costs, and infrastructure expenses reasonable?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Licensing strategy | PASS / FAIL / PARTIAL | [Example: "Open source technologies selected where viable (PostgreSQL, Kafka). SaaS tools licensed on per-unit basis (Zendesk $50/agent). No unused licenses."] |
| Cloud cost estimate | PASS / FAIL / PARTIAL | [Example: "AWS estimated cost: $15K/month. Includes: compute (EC2), storage (S3), database (RDS). Cost breakdown aligned with spend controls."] |
| Infrastructure efficiency | PASS / FAIL / PARTIAL | [Example: "Kubernetes resource requests/limits configured. Right-sizing analysis done. No over-provisioning. Cost optimisation recommendations in place."] |
| Redundancy cost-benefit | PASS / FAIL / PARTIAL | [Example: "Multi-region deployment adds 40% cost but provides disaster recovery and regional latency reduction. Cost justified by business requirements."] |
| Build vs. buy analysis | PASS / FAIL / PARTIAL | [Example: "Build core order service (proprietary logic). Buy help desk and CRM (standard, no differentiation). Appropriate decisions."] |
| License reuse | PASS / FAIL / PARTIAL | [Example: "Reuses PostgreSQL licenses (already approved for enterprise). No new license agreements needed for core dependencies."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Cost-effectiveness assessment]
- [Budget implications]
- [Opportunities for cost optimisation]

**Remediation (if FAIL or PARTIAL):**
- [Cost reductions or efficiency improvements required]

---

### Scalability and Resilience

**Criterion:** Can the system handle growth? Are failure modes identified and mitigated? Is the system resilient?

**Evaluation:**

| Aspect | Rating | Comments |
|--------|--------|----------|
| Load testing | PASS / FAIL / PARTIAL | [Example: "Load tested to 500 RPS (5x expected peak). P99 latency 150ms; within SLA. System scaled horizontally; added 3 more pods under load."] |
| Failure mode analysis | PASS / FAIL / PARTIAL | [Example: "FMEA completed. Single points of failure identified: database primary (mitigated by replication), API Gateway (mitigated by multi-zone). No unmitigated critical failures."] |
| Circuit breaker / timeout | PASS / FAIL / PARTIAL | [Example: "Circuit breakers on all external service calls. Timeouts configured: 2s for fast calls, 5s for async. Prevents cascading failures."] |
| Horizontal scalability | PASS / FAIL / PARTIAL | [Example: "All services stateless. Kubernetes HPA autoscales based on CPU/memory. Min 2 replicas, max 20. Tested and working."] |
| Database scalability | PASS / FAIL / PARTIAL | [Example: "PostgreSQL with read replicas. Sharding plan for future (not yet needed). Master-slave replication, 100ms lag acceptable."] |
| Message queue resilience | PASS / FAIL / PARTIAL | [Example: "Kafka cluster with 3 brokers (high availability). Replication factor 3 for topics. Consumer group offsets persisted. No message loss tested."] |
| Caching strategy | PASS / FAIL / PARTIAL | [Example: "Redis cache for hot data (customer profiles, product info). Cache-aside pattern. TTL 1 hour. Handles cache misses gracefully (queries DB)."] |

**Overall rating:** PASS / FAIL / PARTIAL

**Key findings:**
- [Scalability and resilience posture]
- [Any unmitigated risks]
- [Load test results summary]

**Remediation (if FAIL or PARTIAL):**
- [Resilience improvements required]

---

## Findings Summary

**Total findings:** [Number]
**Critical findings:** [Number]
**Major findings:** [Number]
**Minor findings:** [Number]

### Findings Table

| Finding ID | Category | Severity | Description | Recommendation | Owner | Due Date | Status |
|---|---|---|---|---|---|---|---|
| F-001 | Security | Critical | TLS 1.2 used; enterprise standard requires TLS 1.3 on all inter-service communication | Upgrade to TLS 1.3 immediately; add TLS policy enforcement | Tech Lead | Before go-live | Open |
| F-002 | Data | Major | PII not identified in service design; need to tag sensitive fields (name, email, address) | Complete data classification; implement field-level encryption for PII | Data Architect | Within 1 week | Open |
| F-003 | Operational | Major | No disaster recovery runbook; RTO/RPO undefined | Define RTO/RPO; implement backup/restore process; test quarterly | Ops Lead | Within 2 weeks | Open |
| F-004 | Compliance | Minor | Audit logging incomplete; user actions not logged | Add audit logging for all state-changing operations | Dev Lead | Within 1 month | Open |
| F-005 | Performance | Minor | No load testing completed; expect scalability issues under peak | Conduct load test to 500 RPS; document scaling limits | QA Lead | Before staging | Open |

---

## Conditions for Approval

(Complete this section only if recommendation is "APPROVE WITH CONDITIONS")

**The following conditions must be satisfied before proceeding to go-live:**

1. **Condition 1: Security – TLS 1.3 Enforcement**
   - Requirement: All service-to-service communication must use TLS 1.3 (Finding F-001).
   - Evidence of completion: Kubernetes Network Policy enforcing TLS version; captured in test results.
   - Owner: Tech Lead
   - Due date: Before code merge to main branch (before go-live)
   - Verification: Architect review of configuration; security scan confirmation.

2. **Condition 2: Data – PII Classification and Encryption**
   - Requirement: All PII fields identified and classified. Encrypted at rest (AES-256) and in transit (TLS 1.3). Masked in logs.
   - Evidence of completion: Data classification document signed by Data Architect. Field-level encryption implementation. Log sample showing masked data.
   - Owner: Data Architect
   - Due date: Within 1 week
   - Verification: Code review of encryption implementation; audit of log outputs.

3. **Condition 3: Operational – Disaster Recovery Plan**
   - Requirement: RTO/RPO defined. Backup/restore procedures documented. Tested and confirmed working.
   - Evidence of completion: Documented RTO/RPO targets. Runbook signed off by Ops. Test results showing successful restore.
   - Owner: Ops Lead
   - Due date: Within 2 weeks
   - Verification: Disaster recovery test executed; results validated by Architect.

---

## Risks Accepted

(Document risks that the business has explicitly accepted and signed off on)

| Risk | Impact | Probability | Mitigation | Accepted By | Date |
|------|--------|-------------|-----------|-----------|------|
| Replication lag (100-500ms) between primary and read replicas may cause stale reads in reporting | Users see slightly out-of-date data; <1% of queries affected | High | Document eventual consistency in reporting UX; cache warmed with latest data on critical queries | VP Operations | YYYY-MM-DD |
| If Kafka broker fails, event processing delayed until failover completes (~30 seconds) | Orders delayed 30 seconds in fulfillment; 0.01% of orders affected | Low | 3-broker cluster with replication factor 3; automatic failover; documented SLA degradation | VP Operations | YYYY-MM-DD |
| External payment service integration timeout (5s) may fail legitimate payments; requires retry | ~0.5% of payment attempts fail on first try; recovered by retry | Medium | Payment idempotency key prevents duplicate charges. Retry with exponential backoff. Customer sees "retry" button | VP Finance | YYYY-MM-DD |

---

## Architecture Review Decision

### Overall Assessment

**Recommendation:** [APPROVE | APPROVE WITH CONDITIONS | REJECT]

**Justification:**
[Detailed rationale for recommendation. Reference the findings, criteria scores, and risk acceptance.]

Example for APPROVE:
"The Order Fulfillment Platform solution is well-architected and ready for development. It aligns with enterprise strategy, follows approved technology standards, and demonstrates strong resilience patterns. All 8 review criteria rated PASS. No critical findings. The team is experienced and ready to execute."

Example for APPROVE WITH CONDITIONS:
"The solution is fundamentally sound and strategically aligned. Three conditions must be remediated before go-live: (1) TLS 1.3 enforcement for service-to-service communication (security), (2) PII identification and encryption (data governance), (3) Disaster recovery plan validation (operational readiness). Once these conditions are met, the solution is approved for go-live."

Example for REJECT:
"Cannot approve this solution. It violates two critical enterprise architecture standards: (1) proposes shared database across multiple services, contradicting database-per-service governance, (2) relies on two separate data warehouses (ETL from Order System and separate analytics database), violating single source of truth principle. Recommend redesign to align with enterprise data architecture."

---

## Sign-Off and Approval

| Role | Name | Approval | Date | Signature |
|------|------|----------|------|-----------|
| Lead Architect (Decision Authority) | | APPROVE / CONDITIONS / REJECT | | |
| Security Architect | | Approved / Concerns | | |
| Data Architect | | Approved / Concerns | | |
| Operations Architect | | Approved / Concerns | | |
| Infrastructure Lead | | Approved / Concerns | | |
| Business Sponsor | | Approved / Concerns | | |

**Approval decision recorded:** [Date]
**Approval valid through:** [Date] (typically until next major change or design update)

---

## Next Steps

(Complete after approval decision)

| Step | Owner | Due Date | Status |
|------|-------|----------|--------|
| Resolve conditions (if conditional approval) | Tech Lead, Data Architect, Ops Lead | [Dates] | Pending |
| Move to development phase | Program Manager | [Date] | Pending |
| Schedule next architecture review (pre-go-live) | Architect | 2 weeks before go-live | Scheduled |
| Document approved architecture in central repo | Solution Architect | Within 1 week | Pending |
| Communicate decision to stakeholders | Program Manager | Within 2 days | Pending |

---

## Appendix: Review Process and Criteria Definitions

**Review scope:**
This review evaluates the solution against enterprise architecture standards across 8 key dimensions: strategic alignment, standards compliance, security/compliance, integration, data, operational readiness, cost/efficiency, and scalability/resilience. The review is conducted before development begins (or before major design changes).

**Evaluation scale:**
- **PASS:** Criterion fully satisfied; no concerns.
- **FAIL:** Criterion not satisfied; must be remediated (if conditional approval) or solution rejected.
- **PARTIAL:** Criterion partially satisfied; some concerns that must be addressed (added to conditions if approved).
- **N/A:** Criterion not applicable to this solution.

**Decision matrix:**
- All criteria PASS → APPROVE
- Some PARTIAL, others PASS, no FAIL → APPROVE WITH CONDITIONS (for PARTIAL items)
- Any FAIL → REJECT (unless business accepts risk)

