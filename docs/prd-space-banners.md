# PRD — Space Context Banners & Rules 500 Fix

**Date:** 2026-03-15
**Product Owner:** PO (BMAD)
**Epic:** Space Context Awareness & Stability

---

## Prioritization (RICE)

| Story | Reach | Impact | Confidence | Effort | RICE Score | Priority |
|---|---|---|---|---|---|---|
| US-1: Sandbox banner | 500 | 2x | 90% | 0.5d | 1800 | P0 |
| US-2: Production banner | 500 | 2x | 90% | 0.5d | 1800 | P0 |
| US-3: Fix 500 proxy error | 500 | 3x | 100% | 0.25d | 6000 | P0 |

---

## User Stories

### US-1: Sandbox Space Banner
**As a** rule author,  
**I want** a banner when I open the Sandbox space  
**So that** I know this is a shared experimental environment and not a production space.

**Acceptance Criteria:**
```gherkin
Feature: Sandbox space banner

  Scenario: Opening the Sandbox space shows Sandbox banner
    Given I am logged in as any user
    And I navigate to the SpaceDetailPage for the "Sandbox" space
    Then I see a banner with text "This is the Sandbox space. Any rules created here will be visible to all spaces."
    And the banner is visually distinct (amber/yellow styling)

  Scenario: Other spaces do not show Sandbox banner
    Given I navigate to any space that is NOT named "Sandbox"
    Then I do NOT see the Sandbox banner
```

### US-2: Production Space Banner
**As a** rule author,  
**I want** a banner when I open any non-Sandbox space  
**So that** I know I am working in a production environment.

**Acceptance Criteria:**
```gherkin
Feature: Production space banner

  Scenario: Opening a production space shows production banner
    Given I navigate to any space NOT named "Sandbox"
    Then I see a banner with text "This is a production space. Any rules created here will be visible to all spaces."
    And the banner is visually distinct (blue styling)

  Scenario: Sandbox space does not show production banner
    Given I navigate to the "Sandbox" space
    Then I do NOT see the production space banner
```

### US-3: Fix Rules 500 Error
**As a** user viewing a space's rules,  
**I want** the rules list to load without errors  
**So that** I can manage rules within a space.

**Acceptance Criteria:**
```gherkin
Feature: Space rules API

  Scenario: GET /v1/spaces/{id}/rules returns 200
    Given the management service is running
    And I have a valid auth token
    When I GET /v1/spaces/{spaceId}/rules
    Then the response status is 200
    And the response body contains a paginated list of rules

  Scenario: SpaceDetailPage loads rules without errors
    Given I am logged in
    When I navigate to any SpaceDetailPage
    Then the rules tab shows a list (or empty state)
    And there is no "Server error" message
```

---

## Alignment Check Matrix

| Requirement | Story | Architecture | Implementation |
|---|---|---|---|
| FR-001 Sandbox banner | US-1 | SpaceDetailPage.tsx | Banner component using space.name === "Sandbox" |
| FR-002 Production banner | US-2 | SpaceDetailPage.tsx | Banner component using space.name !== "Sandbox" |
| FR-003 Fix 500 proxy | US-3 | vite.config.ts | Regex proxy rule before generic /v1/spaces |

---

## Definition of Done

- [ ] TypeScript build passes (0 errors)
- [ ] Banners render correctly for both space types
- [ ] `/v1/spaces/{id}/rules` returns 200 in local dev
- [ ] Smoke test section updated and passing
- [ ] No new 🔴 issues in code review

