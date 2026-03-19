# Requirements Analysis Frameworks Reference

A comprehensive guide to frameworks, methodologies, and best practices for analyzing and documenting requirements in enterprise software development.

---

## Requirements Classification

Understanding different types of requirements and how they differ.

### Functional Requirements

**Definition:** Describe *what* the system must do. Specific behaviors, features, business logic.

**Characteristics:**
- Specific and measurable (can test: does it work or not?)
- Testable (can write test case)
- Related to system behavior
- From user perspective: "I need to..."

**Examples:**

| Good | Bad |
|------|-----|
| "System must calculate tax on order total using ZIP code lookup" | "System should be smart about taxes" |
| "User can add up to 5 products to cart before checkout" | "Cart functionality must work well" |
| "Invoices must be generated within 5 seconds of order completion" | "System should generate invoices" |
| "Password must be 8+ chars, 1 uppercase, 1 number, 1 special char" | "Passwords should be secure" |

**How to Document:**
- State as action: "System shall [verb] [object]..."
- Example: "System shall validate email format before signup"

---

### Non-Functional Requirements

**Definition:** Describe *how well* the system must perform. Quality attributes, performance, reliability, security, usability.

**Categories:**

| Category | Examples |
|----------|----------|
| **Performance** | Response time < 500ms, 60 fps scroll, page load < 2s |
| **Scalability** | Support 10,000 concurrent users, 1M records |
| **Reliability** | 99.9% uptime, MTTR < 1 hour, data loss < 0.001% |
| **Security** | Encrypt at rest & in transit, no SQL injection, 2FA support |
| **Usability** | WCAG AA accessible, touch target 48dp min, i18n 5+ languages |
| **Maintainability** | Test coverage > 80%, deployment < 10 min, code reviews required |
| **Compliance** | GDPR, HIPAA, PCI-DSS, SOC2 |
| **Compatibility** | Support iOS 12+, Android 6+, Safari, Chrome, Firefox |

**Examples:**

| Good | Bad |
|------|-----|
| "System must respond to search query within 500ms (p95)" | "Search should be fast" |
| "App must launch in < 400ms (cold start)" | "App should launch quickly" |
| "All personally identifiable information encrypted with AES-256" | "Data should be secure" |
| "95% of users complete signup in < 3 minutes" | "Signup should be easy" |

---

### Constraints

**Definition:** Technical, organizational, or environmental limitations. Often not changeable.

**Examples:**

| Type | Example |
|------|---------|
| **Technical** | "Must use Java 11 or higher", "API must use REST (not GraphQL)", "Database must be PostgreSQL" |
| **Resource** | "Project budget: $500k", "Team size: 4 engineers", "Timeline: 6 months" |
| **Organizational** | "Code must follow company style guide", "All changes require PR review", "Deployment on Tuesdays only" |
| **Regulatory** | "Must comply with GDPR", "Data retention: 7 years", "Audit trail required" |
| **Environmental** | "Network bandwidth: 4G average", "Device memory: 2GB minimum", "No external dependencies (air-gapped)" |

---

## SMART Requirements Criteria

Ensure requirements are actionable, measurable, and testable.

### SMART Framework

| Criterion | Definition | Example |
|-----------|-----------|---------|
| **Specific** | Clear, focused, unambiguous | "User must be able to search products by keyword, category, or price range" (not "search functionality") |
| **Measurable** | Quantified; has acceptance criteria | "Search results return within 500ms" (not "search should be fast") |
| **Achievable** | Realistic given resources | "Support 10,000 concurrent users" (achievable with proper architecture) |
| **Relevant** | Tied to business goal; valuable | "Reduce checkout time by 50%" (aligned to conversion KPI) |
| **Time-bound** | Deadline or context | "By Q2 2024", "In MVP release", "Before Black Friday" |

### Examples Table: Good vs. Bad Requirements

| Bad Requirement | SMART Critique | Good Requirement |
|-----------------|---|---|
| "Users should be able to view their order history" | Not measurable; vague | "Users can view up to 12 months of order history; filter by date range; display 50 orders per page" |
| "App should be accessible" | Too vague; hard to test | "App must be WCAG 2.1 AA compliant; all images have alt text; touch targets minimum 48dp" |
| "System must be secure" | Too broad; no specifics | "All data encrypted in transit (HTTPS); at rest (AES-256); API keys rotated every 90 days; OWASP top 10 mitigations" |
| "Improve performance" | No baseline; unmeasurable | "Reduce API response time from avg 1.2s to < 500ms (p95); page load from 3.5s to < 2.0s" |
| "Better user experience" | Subjective; untestable | "95% of users complete registration in < 3 minutes; error messages are < 20 words; button labels are action verbs" |

---

## MoSCoW Prioritization

A simple framework for categorizing requirements by priority. Used to determine MVP scope and phased delivery.

### Definitions

| Category | Definition | % of Requirements | Decision Rule |
|----------|-----------|---|---|
| **Must** | Critical; project fails without it. Blocking. | 20-30% | Include in MVP; no trade-off |
| **Should** | Very important; add significant value. Include if possible. | 40-50% | Prioritize if time/budget allows |
| **Could** | Nice-to-have; differentiators. Only if time remains. | 15-20% | Lower priority; Phase 2+ |
| **Won't** | Out of scope for this release. Future consideration. | 5-10% | Document for next phase |

### Example Breakdown

**E-commerce Checkout Feature:**

| Category | Examples | # |
|----------|----------|---|
| **Must** | Add items to cart, set shipping address, process payment, confirm order | 4 |
| **Should** | Save addresses for reuse, apply coupon code, gift wrapping, insurance option | 4 |
| **Could** | Live chat support, AR try-on preview, personalized recommendations | 3 |
| **Won't** | Multi-vendor checkout, cryptocurrency payment, white-label options | 3 |

**Typical Split:** 25% Must (must do) / 45% Should (should do) / 20% Could (could do) / 10% Won't (won't do)

### Anti-Patterns (Common Mistakes)

| Anti-Pattern | Why It's Wrong | Fix |
|---|---|---|
| **Everything is "Must"** | Impossible to deliver; scope creep | Say "Should" or "Could"; be honest about constraints |
| **No "Must"** | No focus; low-value MVP | Identify true non-negotiables |
| **MoSCoW = Implementation Order** | Backlog order ≠ priority | Use RICE or other frameworks for sequencing; some "Should"s come before "Could"s |
| **User Priority = Business Priority** | Users want everything; some needs bigger impact | Balance user wants with business goals |

---

## INVEST Criteria for User Stories

Guidelines for writing user stories that are story-sized (can fit in a sprint), detailed enough to estimate, and independent.

### INVEST Acronym

| Letter | Criterion | Explanation |
|--------|-----------|-------------|
| **I** | **Independent** | Story doesn't depend on other stories; can be built in any order |
| **N** | **Negotiable** | Story is a starting point; details discussed with team, not a rigid contract |
| **V** | **Valuable** | Story delivers value to user or business; not technical debt |
| **E** | **Estimable** | Team can estimate effort; not vague ("make it better") |
| **S** | **Small** | Can be completed in 1-2 days; fits in one sprint |
| **T** | **Testable** | Clear acceptance criteria; team knows when it's done |

### Examples: Good vs. Bad Stories

**Bad Story (violates INVEST):**
```
As a customer,
I want a better checkout experience,
So that I can buy products more easily.

Problems:
- Not valuable (too vague)
- Not estimable (what's "better"?)
- Not testable (how do we know it's done?)
- Probably too big for one sprint
```

**Good Story (INVEST-compliant):**
```
As a customer,
I want to see a progress indicator during checkout (step 1 of 3),
So that I know how many steps remain.

Acceptance Criteria:
- [ ] Progress bar shows: Step 1 (Cart) / Step 2 (Shipping) / Step 3 (Payment)
- [ ] Current step highlighted in blue
- [ ] Progress updates as user navigates between steps
- [ ] Visible on both mobile (vertical) and desktop (horizontal)
- [ ] WCAG AA compliant (progress bar announced to screen readers)

Estimable: 3-5 story points
Testable: Can write automated tests for step transitions
Small: Designer + 1 engineer, 2 days
Independent: Doesn't depend on other checkout stories
```

---

## Requirements Traceability Matrix (RTM)

**Purpose:** Track relationships from business goals → requirements → implementation → tests. Ensures nothing is lost.

### Structure

```
Business Goal (from strategy)
  ↓
Epic (user-facing feature cluster)
  ↓
Feature (deliverable chunk)
  ↓
User Story (user perspective; implementable)
  ↓
Acceptance Criteria (how to test)
  ↓
Test Case (verifies acceptance criteria)
```

### Example RTM Table

| Business Goal | Epic ID | Epic | Feature | Story ID | Story | AC | Test Case |
|---|---|---|---|---|---|---|---|
| Increase conversion rate by 20% (from 2% to 2.4%) | EP-10 | Simplified Checkout | Reduce steps from 5 → 3 | US-101 | As buyer, I want one-click checkout with saved address | [ ] Checkout in 3 steps [ ] Saved address pre-fills | TC-101: Happy path (saved address) / TC-102: No saved address |
| | | | | US-102 | As buyer, I want express payment (Apple Pay, Google Pay) | [ ] Apple Pay works [ ] Google Pay works | TC-103: Apple Pay flow / TC-104: Google Pay flow |
| Reduce cart abandonment by 15% | EP-11 | Abandoned Cart Recovery | Email reminders | US-103 | As customer, I receive email 2h after cart abandonment | [ ] Email sent 2h after abandon [ ] Email includes cart items [ ] Link back to cart in email | TC-105: Abandon email sent / TC-106: Link works |

**Value:** If a feature isn't linked to a goal, it's low-priority. If test fails, trace back to story and goal.

---

## Business Rules vs. Requirements: Key Distinction

**Business Rules:** Policies, logic, constraints that govern the business. Separate from *how* they're implemented.

**Requirements:** How the system implements business rules. Technology-specific.

### Examples Table

| Business Rule | Requirement (How to Implement) |
|---|---|
| "Customers must be 18+ years old" | "System validates birthdate >= 18 years; rejects signup if younger" |
| "Free tier users see max 50 items; Premium users see 500" | "API returns limit in response header; UI enforces with pagination; backend enforces at query level" |
| "Orders with fraud risk score > 80 require manual review" | "System calculates fraud score using ML model; if > 80, order status = 'PENDING_REVIEW'; flag assigned to compliance team" |
| "Refunds take 5-7 business days" | "Refund API initiates bank transfer on day 1; notification sent on day 1 and day 5; dashboard shows status with expected date" |
| "Inventory decrements on shipment, not on order" | "Order status 'PENDING' doesn't affect inventory; status 'SHIPPED' decrements inventory via event listener" |

**Why This Matters:**
- Business rules are stable (rarely change)
- Requirements change based on technology choices
- Business analysts own rules; engineers own requirements
- Separating them prevents coupling; easier to adapt implementation

---

## Acceptance Criteria Formats

Two styles; choose based on context and team preference.

### Format 1: Gherkin / Given-When-Then (BDD Style)

**Best for:** Complex workflows, conditional logic, detailed scenarios. Executable by automation tools (Cucumber, Behave).

```gherkin
Feature: Checkout Process
  Scenario: User applies coupon code
    Given user has items in cart
    And user has valid coupon code "SAVE10"
    When user applies coupon
    Then coupon code is visible in cart
    And discount is calculated: (subtotal * 0.10)
    And subtotal is reduced by discount
    And confirmation message appears: "Coupon SAVE10 applied"

  Scenario: User applies invalid coupon
    Given user has items in cart
    And user enters invalid coupon code "INVALID123"
    When user applies coupon
    Then error message appears: "Coupon code not found"
    And cart total unchanged
    And input field highlighted in red
```

**Advantages:**
- Executable; can be automated
- Readable by non-technical stakeholders
- Covers happy path + edge cases
- Standard format (Gherkin)

**Disadvantages:**
- Verbose; can be overkill for simple features
- Requires tool setup (Cucumber, etc.)
- Overkill for straightforward features

---

### Format 2: Checklist Style (Simpler)

**Best for:** Straightforward features, simple acceptance, faster documentation.

```
Story: User can apply coupon code

Acceptance Criteria:
- [ ] User can enter coupon code in cart
- [ ] Valid coupon applies 10% discount
- [ ] Invalid coupon shows error message
- [ ] Discount reflected in cart subtotal
- [ ] Confirmation message displayed
- [ ] User can remove coupon; total reverts to original
- [ ] Works on mobile and desktop
- [ ] Error message is < 20 words
- [ ] Discount calculated server-side (not just UI)
- [ ] Coupon code case-insensitive ("SAVE10" = "save10")
```

**Advantages:**
- Simple; quick to write
- Easy to track completion (checkbox)
- Readable without special tools
- Flexible format

**Disadvantages:**
- Not executable
- Less structured for complex scenarios
- Harder to automate testing

---

## Non-Functional Requirements Catalogue

Questions to ask when defining NFRs. Use as a checklist for each epic/feature.

### Performance Requirements

- What is the target response time for this feature? (e.g., < 500ms)
- What is the target page load time? (e.g., < 2s)
- How many concurrent users must it support?
- What's the expected throughput? (e.g., 10,000 requests/second)
- Are there peak periods? (e.g., Black Friday 100x normal traffic)
- Measurement tool? (e.g., Datadog, New Relic)

### Scalability Requirements

- How much data will this feature handle initially? (e.g., 1M users)
- Expected data growth rate? (e.g., 10% per year)
- Should it scale horizontally (more servers) or vertically (bigger servers)?
- What are horizontal scaling limits? (e.g., up to 100 servers)

### Reliability & Availability

- What's the required uptime? (e.g., 99.9% = 8.76 hours/year downtime)
- What's acceptable data loss in a disaster? (e.g., < 1 hour of data)
- What's the recovery time objective (RTO)? (e.g., restore service within 4 hours)
- What's the recovery point objective (RPO)? (e.g., no more than 1 hour of lost data)
- How often should backups occur? (e.g., hourly, daily)

### Security Requirements

- What authentication method? (username/password, SSO, OAuth, 2FA)
- What encryption? (HTTPS in transit, AES-256 at rest)
- What PII is handled? (passwords, financial data, health records)
- What compliance? (GDPR, HIPAA, PCI-DSS)
- What audit logging? (what actions tracked, how long retained)
- Penetration testing required? (annual, before launch)

### Usability & Accessibility

- Target audience technical level? (expert, average, non-technical)
- Mobile vs. desktop primary? (mobile-first design)
- WCAG accessibility level? (A, AA, AAA)
- Keyboard navigation required?
- Screen reader support (VoiceOver, NVDA, JAWS)?
- Supported languages? (English only, i18n to 5+ languages)
- Supported browsers? (Safari, Chrome, Firefox, IE)

### Maintainability

- Code test coverage target? (e.g., > 80%)
- Documentation required? (README, API docs, architecture diagrams)
- Code review process? (all changes require review)
- Technical debt threshold? (e.g., max 10% of sprint velocity)
- Deployment frequency? (daily, weekly)
- Deployment time limit? (e.g., < 15 minutes, < 5 minutes zero-downtime)

### Compliance & Legal

- Regulatory requirements? (GDPR, HIPAA, SOC2, ISO 27001)
- Data retention policy? (e.g., 7 years for financial records, 90 days for logs)
- Data residency? (e.g., EU data must stay in EU)
- Audit requirements? (quarterly security audit, annual compliance review)
- Export controls? (can this feature be sold internationally?)

---

## Gap Analysis Framework

**Purpose:** Identify differences between current state and desired future state. Reveals what needs to change.

### Current vs. Desired State Matrix

| Capability | Current State | Desired State | Gap | Priority | Effort |
|---|---|---|---|---|---|
| **Product Search** | Keyword search only | Keyword + filters + facets | Add filters, facets, ranking algorithm | High | Medium |
| **Checkout** | 5-step process, 5 min avg | 3-step, 2 min avg | Reduce steps, improve UX, express payment | High | High |
| **Mobile App** | Web-responsive only | Native iOS/Android | Build mobile apps | Medium | High |
| **Analytics** | Basic page views | Detailed user journey, cohort analysis | Implement event tracking, data warehouse | Medium | Medium |
| **Customer Support** | Email only | Email + chat + phone | Add chat & phone channels | Medium | Medium |
| **Integrations** | None | 5 key integrations (accounting, shipping, etc.) | Build integration layer | Low | High |

### Using the Matrix

1. **Identify Gaps:** What's missing? What needs improvement?
2. **Prioritize:** Which gaps have highest business impact?
3. **Plan Effort:** How much work to close gap?
4. **Sequencing:** Close high-priority, medium-effort gaps first

---

## Root Cause Analysis: 5 Whys

**Purpose:** Move beyond surface problem to underlying cause. Prevents treating symptoms instead of causes.

### The Technique

Start with a problem statement; ask "Why?" 5 times. Each answer becomes the basis for the next question.

### Example

**Problem:** "Users abandon checkout at 60% rate"

Q1: Why do users abandon checkout?
A: They see a "Confirm Payment" button they're unsure about.

Q2: Why are they unsure about the button?
A: Payment processing takes 10-20 seconds with no feedback.

Q3: Why is there no feedback during processing?
A: The payment UI doesn't show a progress indicator or "processing" message.

Q4: Why wasn't a progress indicator implemented?
A: The payment team didn't know it was needed; no user research was done on the checkout flow.

Q5: Why didn't we do user research?
A: Checkout was built quickly (MVP); we focused on functionality, not UX.

**Root Cause:** Lack of user research and UX focus during MVP phase.

**Solution:** Not "add a button"; instead, "implement user-centered checkout design with progress feedback and user validation."

### Fishbone Diagram (Alternative Format)

Visualize 5 Whys in a fishbone structure:

```
                          ┌─ Payment API responds slowly (10-20s)
                          │
    Payment Processing ───┤─ No progress indication shown
                          │
                          └─ UI doesn't support async feedback
                                    ↓
User abandons checkout ─────────────────────→ ROOT CAUSE: Poor UX during payment
                                    ↑
User Testing ───────────────────────┘
                          ├─ Checkout built in MVP (speed > UX)
                          ├─ No user research on payment flow
                          └─ Engineering didn't consult UX team

```

---

## Risk Register Template

**Purpose:** Identify and track project risks. Enables proactive mitigation.

| Risk | Category | Likelihood (1-5) | Impact (1-5) | Score | Mitigation | Owner | Status |
|------|----------|---|---|---|---|---|---|
| **Technical debt from MVP delays release** | Technical | 4 | 3 | 12 | Code review process, refactor budget each sprint | Engineering Lead | Active |
| **Key engineer leaves mid-project** | People | 2 | 5 | 10 | Knowledge transfer docs, cross-training | HR + Engineering | Active |
| **Customer demand different than forecasted** | Market | 3 | 4 | 12 | Weekly user research, flexible roadmap | Product Manager | Active |
| **Payment processor changes API** | External | 2 | 4 | 8 | Monitor provider roadmap, maintain abstraction layer | Engineering | Active |
| **Compliance rejection on GDPR** | Compliance | 1 | 5 | 5 | Legal review before launch, audit in Q2 | Legal | Active |

**Score = Likelihood × Impact** (max 25)

**Prioritize:** Focus on scores > 10. Low-score risks are acceptable; note and monitor.

---

## Summary Checklist: Requirements Quality Gate

Before finalizing requirements, verify:

- [ ] All functional requirements are specific and measurable
- [ ] NFRs documented for: performance, security, scalability, compliance, accessibility
- [ ] Requirements are SMART (specific, measurable, achievable, relevant, time-bound)
- [ ] Requirements prioritized with MoSCoW or RICE
- [ ] User stories follow INVEST criteria
- [ ] Acceptance criteria clear and testable (Gherkin or checklist)
- [ ] Business rules documented separately from technical requirements
- [ ] Traceability matrix links goals → epics → stories → tests
- [ ] Gap analysis complete; current vs. desired documented
- [ ] Root causes identified (not just symptoms)
- [ ] Risks identified and mitigations planned
- [ ] Open questions captured; assigned owners, due dates
- [ ] Requirements validated with stakeholders
- [ ] Requirements documented in shared repository (Jira, Confluence, etc.)
- [ ] Requirements reviewed in quality gate meeting; sign-off obtained

