# Epic Template

A comprehensive template for writing an epic in the BMAD method. Epics are large features aligned to business initiatives, broken into multiple user stories, and delivered across 1-3 sprints (or more for complex initiatives).

---

## Epic Header

| Field | Value |
|-------|-------|
| **Epic ID** | EP-XXX (e.g., EP-010) |
| **Title** | [Clear, business-oriented title, e.g., "Simplified Checkout Flow"] |
| **Product Area** | [e.g., "Commerce", "Payments", "Customer Experience"] |
| **Initiative / OKR Alignment** | [Link to company OKR or strategic initiative, e.g., "OKR: Increase conversion 20%"] |
| **Product Owner** | [Name] |
| **Target Release** | [Quarter and year, e.g., "Q2 2024"] |
| **Target Launch Date** | [YYYY-MM-DD, approximate] |
| **Epic Status** | Backlog / In Planning / Ready for Dev / In Progress / Testing / Launched |
| **Epic Type** | Feature / Enhancement / Platform / Experiment |

---

## Problem Statement

**Why are we building this? What problem does it solve?**

### Who Has the Problem?

[Describe the user persona or customer segment affected. Specific, not generic.]

Example: "Busy professionals aged 25-40 who use our app during commute (5-15 minutes). They abandon shopping carts when the process takes too long."

### What Is the Problem?

[Clear description of the pain point. Quantified if possible.]

Example: "Our checkout process currently has 5 steps and takes an average of 5 minutes to complete. This is significantly longer than competitors (2-3 minutes), leading to 60% cart abandonment rate during the purchase flow."

### Why Does It Matter?

[Business impact. Connect to company goals.]

Example: "Cart abandonment costs us $2M in lost annual revenue (20% of projected $10M e-commerce revenue). Each 1% reduction in abandonment = $200k revenue. This feature targets 15% reduction = $3M incremental revenue."

### Supporting Data

[Evidence backing the problem. Metrics, research findings, customer feedback.]

- [ ] 60% of users abandon checkout (from analytics, source: Google Analytics 2024-02)
- [ ] Average checkout time: 4m 47s (vs. competitor average: 2m 15s)
- [ ] Top user feedback: "Checkout is too many steps" (5/10 support tickets weekly)
- [ ] User session recording shows users drop off at Step 3 (Shipping Address)
- [ ] Interview with 10 users: 8 said checkout "unnecessarily complicated"

---

## Opportunity / Hypothesis

**If we build this, what do we believe will happen?**

### Opportunity Statement

[What's the bigger opportunity? What can we enable or unlock?]

Example: "By simplifying checkout, we can reduce friction for first-time and repeat buyers, decrease cart abandonment, and improve customer lifetime value through increased conversion."

### Hypothesis

[If-Then statement. Specific, testable, measurable.]

Structure: "If we [change], we believe [outcome] will [improve by X% or reach Y target]."

Example 1:
"If we reduce checkout steps from 5 to 3 (combining address + shipping selection), we believe checkout time will decrease by 50% (from 5 min to 2.5 min), and cart abandonment will decrease by 15% (from 60% to 45%)."

Example 2:
"If we add Express Checkout (Apple Pay, Google Pay, saved addresses), we believe 40% of users will use it, completing checkout in < 1 minute, and repeat customer conversion will increase by 25%."

### Success Outcome

[What does success look like? How will we know this worked?]

Example: "Success = cart abandonment rate drops from 60% to 45% (15% improvement) within 30 days of launch."

---

## User Persona(s) Affected

List the primary user personas this epic impacts.

| Persona | Impact | Motivation |
|---------|--------|-----------|
| **First-Time Buyer** | High | Long checkout discourages signup; express checkout reduces friction |
| **Busy Professional** | High | Time-sensitive; wants quick checkout during commute |
| **Repeat Customer** | Medium | Benefits from saved addresses; one-click checkout |
| **Mobile User** | Very High | Screens smaller; more navigation difficult; more likely to abandon |
| **International User** | Medium | Current process doesn't optimize for multi-currency/shipping complexity |

---

## Business Value & Success Metrics

### Primary Success Metrics

| Metric | Baseline | Target | Measurement Method | Timeframe |
|--------|----------|--------|---|---|
| **Cart Abandonment Rate** | 60% | 45% (15% reduction) | Google Analytics; Track user_id through checkout → purchase funnel | 30 days post-launch |
| **Average Checkout Time** | 4m 47s | 2m 30s (48% reduction) | Session recordings + analytics event timing | 30 days post-launch |
| **Repeat Customer Conversion** | 15% | 18% (20% lift) | Cohort analysis; repeat customers in 90-day window | 60 days post-launch |
| **Mobile Conversion Rate** | 2% | 3% (50% improvement) | Mobile-specific conversion funnel | 30 days post-launch |

### Secondary Metrics

| Metric | Target | Purpose |
|--------|--------|---------|
| **User Satisfaction (CSAT)** | >= 4.0/5.0 on checkout experience | Qualitative validation; NPS improvement |
| **Feature Adoption Rate** | >= 40% of users use express checkout | Adoption of new payment methods |
| **Support Ticket Reduction** | 50% fewer "checkout is confusing" tickets | Reduced support burden |
| **Checkout Error Rate** | < 1% | Technical quality; minimize failed orders |

### Business Impact

[Financial impact of success. ROI calculation.]

- Baseline: $10M annual e-commerce revenue, 60% abandonment
- Actual completed purchases: $4M (40% × $10M)
- With 15% abandonment reduction: 55% × $10M = $5.5M completed purchases
- Incremental revenue: $1.5M per year
- Estimated development cost: $100k (4 engineers × 6 weeks)
- ROI: 1500% in Year 1; pays for itself in ~1 month

---

## Scope Definition

### In Scope

**Features / Capabilities included in this epic.**

- [ ] Reduce checkout steps from 5 to 3 (Cart → Shipping → Payment/Confirmation)
- [ ] Auto-fill shipping address from user profile (if available)
- [ ] Add Express Checkout: Apple Pay (iOS), Google Pay (Android)
- [ ] Save multiple addresses for future checkouts
- [ ] Show estimated delivery date during shipping selection
- [ ] Progress indicator (Step 1 of 3) for user orientation
- [ ] Real-time error feedback (inline validation)
- [ ] Guest checkout option (no account required)
- [ ] Coupon code application during checkout
- [ ] Order confirmation screen with order number, tracking link

### Out of Scope

**Features explicitly NOT included in this epic (or deferred to future phases).**

- [ ] Cryptocurrency payment support (future payment methods epic)
- [ ] Subscribe & Save (recurring delivery feature; separate epic)
- [ ] International shipping address validation (Phase 2; regional expansion)
- [ ] Split payment (pay with multiple cards; Phase 2)
- [ ] Installment plans / BNPL (financing epic; Q3)
- [ ] Custom gift message editor (nice-to-have; deprioritized)
- [ ] Real-time inventory sync during checkout (technical debt; Phase 2)

---

## User Stories

### Story List

**All user stories required to complete this epic. Linked to backlog; prioritized by MVP phasing.**

#### MVP Phase 1 (Critical Path)

| Story ID | Title | Type | Priority | Estimate | Owner |
|----------|-------|------|----------|----------|-------|
| **US-1001** | Reduce checkout to 3 steps (consolidate address + shipping) | Story | P0 | 8 pts | Frontend |
| **US-1002** | Auto-fill shipping address from user profile | Story | P0 | 5 pts | Frontend |
| **US-1003** | Add progress indicator (Step X of 3) | Story | P0 | 3 pts | Frontend |
| **US-1004** | Validate address fields in real-time (inline errors) | Story | P0 | 5 pts | Frontend |
| **US-1005** | Create backend API endpoint for 3-step checkout | Story | P0 | 8 pts | Backend |
| **US-1006** | Integrate with payment processor (Stripe v3) | Story | P0 | 13 pts | Backend |
| **US-1007** | Order confirmation page with tracking | Story | P0 | 5 pts | Frontend |

#### MVP Phase 2 (Express Checkout)

| Story ID | Title | Type | Priority | Estimate | Owner |
|----------|-------|------|----------|----------|-------|
| **US-1008** | Implement Apple Pay integration (iOS) | Story | P1 | 13 pts | Mobile |
| **US-1009** | Implement Google Pay integration (Android) | Story | P1 | 13 pts | Mobile |
| **US-1010** | Save addresses for future checkouts | Story | P1 | 8 pts | Frontend + Backend |
| **US-1011** | Guest checkout flow (no account required) | Story | P1 | 8 pts | Frontend + Backend |

#### Phase 3 (Enhancements)

| Story ID | Title | Type | Priority | Estimate | Owner |
|----------|-------|------|----------|----------|-------|
| **US-1012** | Estimated delivery date based on shipping method | Story | P2 | 5 pts | Frontend |
| **US-1013** | Coupon code application during checkout | Story | P2 | 5 pts | Frontend + Backend |
| **US-1014** | A/B test: "Save for next time" vs. "Always remember" | Test Story | P2 | 3 pts | Analytics |

#### Technical Debt / Infrastructure

| Story ID | Title | Type | Priority | Estimate | Owner |
|----------|-------|------|----------|----------|-------|
| **US-1015** | Unit test coverage: checkout module (target > 85%) | Tech Story | P0 | 5 pts | Frontend |
| **US-1016** | Load testing: checkout under 10k concurrent users | Tech Story | P0 | 5 pts | QA + Backend |
| **US-1017** | Documentation: checkout API specification | Tech Story | P1 | 2 pts | Backend |

---

## Technical Dependencies

### External Dependencies

| System / Service | Dependency Type | Owner | Risk | Mitigation |
|------------------|---|---|---|---|
| **Stripe Payment API** | Integration | Payments Team | Stripe API changes; rate limits | Monitor API roadmap; fallback to Braintree if issues |
| **Apple Pay Framework** | iOS SDK | Apple | iOS version compatibility; deprecated APIs | Test on iOS 12+; update SDK quarterly |
| **Google Pay API** | Android SDK | Google | API changes; device compatibility | Test on Android 6+; subscribe to deprecation notices |
| **Address Validation Service** | Third-party | Smarty Streets | API downtime; validation accuracy | Fallback to regex validation; no single point of failure |
| **Analytics Platform** | Data | Analytics Team | Event tracking schema changes | Coordinate event naming; freeze schema during checkout changes |

### Internal Dependencies

| Team / System | Type | Owner | Impact |
|---|---|---|---|
| **Backend Infrastructure** | Backend API | Platform Team | Need new endpoints for 3-step checkout; need performance testing |
| **Design System** | Components | Design Team | Need button variants, form field states, error states |
| **Customer Support** | Runbooks | Support | Need training on new checkout flow; FAQ updates |
| **QA / Testing** | Test automation | QA | Need Selenium scripts for 3-step flow; cross-browser testing |
| **Analytics** | Event tracking | Analytics Team | Need event schema for new checkout steps |

---

## Non-Functional Requirements Summary

### Performance

- Checkout page load: < 2 seconds (p95)
- API response (complete order): < 1 second
- Address auto-fill: < 500ms
- Support 10,000 concurrent users during flash sale

### Security

- HTTPS for all checkout flows (no exceptions)
- PCI compliance: no credit card numbers stored locally (tokenized payment)
- CSRF protection on form submissions
- Rate limiting: max 5 submit attempts per user per minute (prevent abuse)
- Input validation server-side (never trust client)
- Two-factor authentication available (optional for users)

### Compliance & Accessibility

- WCAG 2.1 Level AA compliant
- Keyboard navigation fully supported (Tab through fields, Enter to submit)
- Screen reader labels: "Card Number Field", "Expiration Date", etc.
- Color contrast: 4.5:1 minimum (all text readable)
- Touch targets: 48dp minimum (Android), 44pt (iOS)
- Supports Spanish, French, German, Japanese (i18n)
- Compliant with GDPR (data deletion, consent management)

### Reliability

- Uptime: 99.95% (2.2 hours downtime/month max)
- Order success rate: > 99.5% (< 0.5% failed orders)
- Data loss: 0 orders lost (durability)
- Rollback plan: revert to previous checkout in < 5 minutes if issues

---

## Milestones / Phasing

**How is this epic broken into phases? What's the release plan?**

| Phase | Name | Stories Included | Target Date | Exit Criteria | Notes |
|-------|------|---|---|---|---|
| **Alpha** | Core 3-Step Checkout | US-1001 to US-1007 | 2024-04-30 | Internal testing complete; payment processing works | Engineering only; not external |
| **Beta** | External Testing | All Phase 1 stories + express checkout | 2024-05-15 | 50 beta users; 95% complete checkout flow; zero critical bugs | Invite select customers; gather feedback |
| **Soft Launch** | Gradual Rollout | Phase 1 + 2 stories | 2024-05-30 | 10% of users routed to new flow; abandonment <= 60% | Canary deployment; monitor metrics |
| **Full Launch** | General Availability | All stories complete | 2024-06-15 | 100% users on new flow; abandonment < 48%; CSAT >= 4.0 | Sunset old checkout; redirect all traffic |
| **Post-Launch** | Monitoring & Optimization | A/B tests (US-1014) | 2024-07-01 | Identify top friction points; plan Phase 3 improvements | Monitor metrics weekly; adjust targeting |

---

## Risks and Assumptions

### Risks

| Risk | Category | Likelihood (1-5) | Impact (1-5) | Score | Mitigation |
|------|----------|---|---|---|---|
| **Payment processor changes API/pricing** | External | 2 | 4 | 8 | Monitor provider roadmap; maintain abstraction layer; have backup processor |
| **Address validation fails for international addresses** | Technical | 3 | 3 | 9 | Use quality address validation service; fallback to manual entry |
| **Apple/Google Pay integration takes longer than estimated** | Technical | 3 | 3 | 9 | Start early; allocate buffer; hire iOS/Android specialists if needed |
| **Users resist new checkout flow (prefer familiar)** | User Behavior | 2 | 3 | 6 | Run A/B test; provide onboarding; communicate benefits |
| **Performance degrades under load (10k concurrent)** | Technical | 2 | 4 | 8 | Load test early; optimize database queries; consider caching strategy |
| **Compliance rejection on accessibility (WCAG)** | Compliance | 1 | 5 | 5 | Test with accessibility tools early; hire accessibility consultant; QA testing |

### Assumptions

| Assumption | Likelihood (1-5) | Impact (1-5) | Validation Plan |
|---|---|---|---|
| Users prefer 3-step checkout over 5-step | 5 (very likely) | 5 (critical) | User research shows 80% prefer shorter flows; A/B test post-launch |
| Express checkout (Apple/Google Pay) drives 40% adoption | 3 (medium) | 4 (high) | Research shows 30-50% adoption in e-commerce; monitor Week 1-4 |
| Server can handle 10k concurrent users | 4 (likely) | 4 (high) | Load testing; infrastructure team confirms capacity |
| Stripe API won't change during our 8-week dev | 5 (very likely) | 2 (low) | Stripe rarely changes APIs; monitor roadmap |
| Team has capacity (4 engineers, 8 weeks) | 3 (medium) | 3 (medium) | Confirm resource availability; plan ramp-up period |

---

## Stakeholder Sign-Off

**Record approval from decision-makers. Ensures alignment and accountability.**

| Stakeholder | Role | Approval Date | Status | Comments |
|---|---|---|---|---|
| **Sarah Chen** | VP Product | 2024-03-01 | ✓ Approved | Aligns with Q2 OKR; prioritize Phase 1 |
| **James Rodriguez** | VP Engineering | 2024-03-01 | ✓ Approved | Confirmed team capacity; 8-week estimate realistic |
| **Maria Lopez** | VP Sales | 2024-03-02 | ✓ Approved | Customers requesting faster checkout; high priority |
| **David Kim** | CFO | 2024-03-02 | ✓ Approved | $100k investment justified by $1.5M incremental revenue |
| **Customer Advisory Board** | Customers | 2024-03-05 | ✓ Feedback collected | 9/10 surveyed want faster checkout; Beta testing interest high |

---

## Open Questions

**Questions blocking epic planning or implementation. Tracked to resolution.**

| Question | Context | Asked By | Date | Answer | Resolved |
|---|---|---|---|---|---|
| **Should we support cryptocurrency payments in Phase 1?** | Stripe supports Crypto; customer interest increasing | Marketing | 2024-02-28 | No; out of scope for MVP. Phase 2 if demand justifies. | Yes |
| **What about subscription/recurring billing?** | Some customers interested in recurring orders | Product | 2024-02-28 | Separate epic. Checkout handles one-time orders only. | Yes |
| **Do we need international address support in Phase 1?** | UK, Canada, Australia users requesting | Support | 2024-03-01 | Phase 1 US/Canada only. International Phase 2. | TBD |
| **How do we handle payment failures gracefully?** | Failed card transactions happen; how to retry? | Engineering | 2024-03-01 | Show error + retry button. Log for analytics. | TBD |
| **Should we offer installment payments (BNPL)?** | Shopify offers Affirm/Klarna integration | Product | 2024-03-02 | Research Q2; separate epic if pursuing | TBD |

---

## Definition of Done

**Checklist for epic completion. All items must be satisfied before declaring epic "Done."**

- [ ] All user stories completed and accepted (criteria met)
- [ ] Code review completed; all PRs approved
- [ ] Unit test coverage > 85%; integration tests pass
- [ ] Load testing passed (10k concurrent users, < 2s response time)
- [ ] Security review completed (OWASP top 10, PCI compliance)
- [ ] Accessibility testing completed (WCAG 2.1 AA)
- [ ] Cross-browser testing: Chrome, Safari, Firefox, Edge (latest versions)
- [ ] Mobile testing: iOS 12+, Android 6+ (landscape + portrait)
- [ ] Performance benchmarked (page load < 2s; API < 1s)
- [ ] Documentation completed (API docs, runbooks, FAQ)
- [ ] Support team trained on new flow
- [ ] Analytics events implemented and validated
- [ ] Monitoring/alerts configured for checkout metrics
- [ ] Rollback plan tested (can revert in < 5 minutes)
- [ ] Customer success team briefed; customer comms drafted
- [ ] Launch date confirmed with all stakeholders
- [ ] Post-launch monitoring plan finalized

---

## Epic Summary

One-paragraph executive summary for leadership visibility.

"This epic simplifies the checkout process from 5 steps to 3, reducing average checkout time from 4m 47s to 2m 30s and decreasing cart abandonment from 60% to 45%. We'll implement Express Checkout (Apple/Google Pay), address auto-fill, and real-time validation. The $100k investment is justified by $1.5M incremental annual revenue. Delivered in two phases (MVP by end of Q2, full launch by mid-June) with 4 engineers, 8 weeks, targeting > 99.5% uptime and WCAG AA accessibility. Success measured by reduced abandonment rate, faster checkout time, and > 4.0 CSAT score."

