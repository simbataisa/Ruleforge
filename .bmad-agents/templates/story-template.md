# Story: [EPIC-ID]-[STORY-NUMBER] — [Title]

> **Status:** Draft | Ready | In Progress | Done
> **Epic:** [Epic Name]
> **Priority:** Must Have | Should Have | Could Have
> **Complexity:** S | M | L | XL
> **Assigned Agent:** [Backend/Frontend/Mobile Engineer]
> **PRD Reference:** US-[X.Y]
> **Architecture Reference:** [Component/Service name]

## User Story

**As a** [user persona],
**I want to** [action/capability],
**So that** [business value/benefit].

## Context

[Explain the business context and why this story matters. Reference the relevant PRD section and architectural decisions. This context helps the implementing agent make good judgment calls when facing ambiguity.]

### Related Artifacts
- PRD: `docs/prd.md` §[section]
- Architecture: `docs/architecture/solution-architecture.md` §[section]
- ADR: `docs/architecture/adr/ADR-[XXX].md` (if applicable)
- API Spec: `docs/tech-specs/api-spec.md` §[section] (if applicable)

## Acceptance Criteria

```gherkin
Scenario: [Happy path scenario name]
  Given [precondition]
  When [action]
  Then [expected result]
  And [additional verification]

Scenario: [Error/edge case scenario name]
  Given [precondition]
  When [action]
  Then [expected error handling]
```

## Technical Implementation Notes

### Approach
[High-level implementation approach — which services/components to modify, what patterns to use]

### API Changes
[New or modified endpoints, request/response schemas]

### Data Changes
[Database migrations, new tables/columns, data transformations]

### Integration Points
[Services to integrate with, contracts to follow]

### Security Considerations
[Auth requirements, data sensitivity, input validation needs]

## Definition of Done

- [ ] Code implemented following coding standards
- [ ] Unit tests written and passing (>80% coverage for new code)
- [ ] Integration tests written for API/service boundaries
- [ ] API documentation updated
- [ ] Error handling and logging implemented
- [ ] Security considerations addressed
- [ ] Code reviewed by Tech Lead agent
- [ ] QE agent test cases passing
- [ ] No new technical debt introduced (or tracked if unavoidable)
- [ ] Implementation notes documented

## Dependencies

| Dependency | Type | Status | Blocker? |
|-----------|------|--------|----------|
| [Story/Service/External] | Upstream/Downstream | Done/In Progress/Blocked | Yes/No |

## Test Cases

| TC ID | Scenario | Expected Result | Type |
|-------|----------|----------------|------|
| TC-001 | | | Unit/Integration/E2E |

## Handoff Notes

**From:** Tech Lead / Scrum Master
**To:** [Engineering Agent]
**Key Decisions:** [Pre-made decisions to reduce ambiguity]
**Open Questions:** [Anything the implementing agent needs to decide]
