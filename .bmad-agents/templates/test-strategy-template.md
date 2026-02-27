# Test Strategy: [Project Name]

> **Status:** Draft | Approved
> **Author:** Tester & QE Agent
> **Date:** YYYY-MM-DD
> **PRD Reference:** `docs/prd.md` v[X]
> **Architecture Reference:** `docs/architecture/solution-architecture.md` v[X]

## 1. Test Scope

### In Scope
| Component/Service | Test Types | Priority |
|-------------------|-----------|----------|
|                   | Unit, Integration, E2E | High |

### Out of Scope
- [Items explicitly not tested in this phase and why]

## 2. Test Levels

### Unit Testing
- **Coverage Target:** >80% line coverage for new code
- **Framework:** [Jest / pytest / Go testing / etc.]
- **Responsibility:** Engineering agents (Backend, Frontend, Mobile)
- **Focus:** Business logic, data transformations, edge cases

### Integration Testing
- **Scope:** Service-to-service communication, database interactions, external API contracts
- **Framework:** [Testcontainers / WireMock / etc.]
- **Responsibility:** Engineering agents + QE agent
- **Focus:** Contract adherence, error propagation, timeout handling

### End-to-End Testing
- **Scope:** Critical user journeys through the full stack
- **Framework:** [Playwright / Cypress / Detox / etc.]
- **Responsibility:** QE agent
- **Focus:** Happy paths, key error paths, cross-service flows

### Performance Testing
- **Tool:** [k6 / Locust / Gatling / etc.]
- **Targets:**
  - Response time: < [X]ms at p95
  - Throughput: > [X] RPS
  - Error rate: < [X]%
- **Types:** Load, Stress, Soak, Spike

### Security Testing
- **OWASP Top 10 coverage**
- **Auth/AuthZ boundary testing**
- **Input validation and injection testing**
- **Dependency vulnerability scanning**

### Contract Testing
- **Tool:** [Pact / Specmatic / etc.]
- **Scope:** All service-to-service APIs
- **Consumer-driven contracts** between frontend ↔ backend, service ↔ service

## 3. Test Environments

| Environment | Purpose | Data | Infra |
|-------------|---------|------|-------|
| Local       | Unit + integration | Mocked/seeded | Docker Compose |
| CI          | All automated tests | Synthetic | Ephemeral containers |
| Staging     | E2E + performance | Production-like | Cloud mirror |

## 4. Test Data Strategy

- **Approach:** [Factory-based / Fixtures / Seeded / Production snapshot]
- **PII Handling:** [Anonymization approach]
- **Refresh Cadence:** [How often test data is reset]

## 5. Quality Gates

### Pre-Merge (CI)
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Code coverage >= threshold
- [ ] No critical/high security vulnerabilities
- [ ] Linting and formatting pass

### Pre-Release (Staging)
- [ ] All E2E tests pass
- [ ] Performance targets met
- [ ] Security scan clean
- [ ] Contract tests pass
- [ ] Accessibility audit pass (frontend/mobile)

## 6. Defect Management

| Severity | Definition | Response Time | Resolution Time |
|----------|-----------|---------------|----------------|
| Critical | System down, data loss | Immediate | 4 hours |
| High     | Major feature broken, no workaround | 2 hours | 1 day |
| Medium   | Feature impaired, workaround exists | 1 day | 1 sprint |
| Low      | Cosmetic, minor UX issue | Next sprint | Best effort |

## 7. Traceability Matrix

| PRD Requirement | Story | Test Cases | Test Type | Status |
|----------------|-------|-----------|-----------|--------|
| US-1.1         |       | TC-001, TC-002 | Unit, Integration | |

## 8. Risk-Based Testing Priorities

| Risk Area | Impact | Likelihood | Testing Emphasis |
|-----------|--------|-----------|-----------------|
| Payment processing | Critical | Medium | Extensive E2E + security |
| Data migration | High | High | Integration + data validation |
