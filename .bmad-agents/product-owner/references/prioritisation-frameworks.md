# Prioritization Frameworks Reference

A comprehensive guide to frameworks and methodologies for prioritizing features, epics, and initiatives in product management. Choose the framework based on your decision context.

---

## RICE Framework (Reach × Impact × Confidence / Effort)

**Best for:** Quick, quantitative prioritization of many features. Works across teams (marketing, product, engineering). Most popular in tech startups.

### Formula

```
RICE Score = (Reach × Impact × Confidence) / Effort

Interpretation:
- Score 100+: Top priority, pursue immediately
- Score 50-99: High priority, plan for next quarter
- Score 10-49: Medium priority, consider for backlog
- Score < 10: Low priority, deprioritize or pass
```

### Component Scoring Guide

**Reach: How many people will this affect? (over time period, e.g., 1 quarter)**

| Score | Definition | Examples |
|-------|-----------|----------|
| **100** | Affects 100% of users (~50,000+ DAU) | Product-wide feature, core bug fix, platform stability |
| **50** | Affects 50% of users (~25,000 DAU) | Feature for common user segment (mobile users, premium tier) |
| **25** | Affects 25% of users (~12,500 DAU) | Feature for specific user segment (power users, new market) |
| **10** | Affects 10% of users (~5,000 DAU) | Niche feature, edge case, specific persona |
| **5** | Affects < 5% of users (~2,500 DAU) | Very niche; experimental; small feature |
| **1** | Affects < 1% of users (<500 DAU) | One-off, special case, not worth doing |

---

**Impact: How much will it improve the metric we care about? (qualitative scale)**

| Score | Definition | Example |
|-------|-----------|---------|
| **3x** | Massive impact; moves the needle significantly | Feature that increases conversion 20% OR reduces churn 30% |
| **2x** | Major impact; clear wins | Feature that increases engagement 10% OR improves retention 15% |
| **1x** | Moderate impact; meaningful improvement | Feature that improves NPS 5 points OR reduces support load 20% |
| **0.5x** | Small impact; incremental improvement | Feature that improves UX slightly; minor bug fix |
| **0.25x** | Minimal impact; barely noticeable | Polish; minor aesthetic improvements; edge case fix |

---

**Confidence: How confident are we in the reach and impact estimates? (0-100%)**

| Score | Definition | Evidence |
|-------|-----------|----------|
| **100%** | Very confident; strong data/research | User research with 20+ interviews; validated analytics; A/B test results |
| **80%** | Confident; some validation | Stakeholder consensus; market research; 5-10 customer conversations |
| **50%** | Moderate confidence; reasonable assumption | Anecdotal feedback; logical reasoning; competitor analysis |
| **25%** | Low confidence; educated guess | Single user request; hypothetical scenario; no validation |
| **10%** | Very low confidence; shot in the dark | Gut feeling; untested assumption; speculation |

**Rule of Thumb:** If confidence < 25%, do user research first before prioritizing.

---

**Effort: How many person-weeks to complete? (1 = easy, 1 week of work)**

| Score | Definition | Examples |
|-------|-----------|----------|
| **0.25** | < 1 week; trivial | Small bug fix, copy change, simple UI tweak |
| **0.5** | 1-2 weeks | Simple feature, one engineer |
| **1** | 2-4 weeks | Standard story, one engineer, straightforward |
| **2** | 1 month (4 weeks) | Medium feature, cross-team (design + eng) |
| **4** | 1 month, multi-person OR 2 months solo | Complex feature, requires backend + frontend |
| **8** | 2 months, small team | Epic, 2-3 engineers, 8 weeks |
| **16+** | 1+ quarter | Major initiative, platform change, requires planning |

**Be Realistic:** Engineer estimates tend toward optimism. Add 20-30% buffer.

---

### Worked Example

**Feature A: Dark Mode**

```
Reach:        50 (50% of users want it; growing demand)
Impact:       0.5x (Nice feature; doesn't move core metrics)
Confidence:   75% (Community requests + feature requests; weak data on conversion impact)
Effort:       4 (Design all colors, test cross-platform, accessibility)

RICE = (50 × 0.5 × 0.75) / 4
     = 18.75 / 4
     = 4.69

Score: 4.69 (Low priority; nice-to-have)
```

**Feature B: One-Click Checkout**

```
Reach:        80 (80% of users do checkout; all users benefit)
Impact:       2x (Could reduce checkout time 50%; increase conversion 10%)
Confidence:   80% (User research + competitor benchmarking validates need)
Effort:       8 (Complex feature; design, frontend, backend, payments)

RICE = (80 × 2 × 0.80) / 8
     = 128 / 8
     = 16

Score: 16 (Medium-High priority; good ROI)
```

**Feature C: Remove Obsolete Feature**

```
Reach:        25 (25% of code uses this; cleanup)
Impact:       1x (Maintenance benefit; cleaner codebase)
Confidence:   90% (Clear that it's unused; refactor benefit known)
Effort:       2 (Code cleanup, testing, migrations)

RICE = (25 × 1 × 0.90) / 2
     = 22.5 / 2
     = 11.25

Score: 11.25 (Medium priority; technical debt worth addressing)
```

**Ranking (from worked examples above):**
1. Feature B: 16 (highest RICE)
2. Feature C: 11.25 (medium)
3. Feature A: 4.69 (lowest RICE)

---

### RICE Strengths & Weaknesses

**Strengths:**
- Simple formula; doesn't require complex spreadsheets
- Quantitative; reduces subjective debate
- Easy to compare across features
- Works across teams (marketing, sales, product)
- Factors in risk (confidence) explicitly

**Weaknesses:**
- Estimates can be wrong (garbage in, garbage out)
- Doesn't account for strategic importance (long-term vision)
- Biased toward features with measurable reach (misses harder-to-measure impacts)
- Reach estimates often inflated (people say "affects everyone" when it doesn't)
- Effort estimates notoriously optimistic (add 20-30% buffer)

**When to Use:** Quarterly planning when you have many competing features and need a tie-breaker.

---

## MoSCoW Prioritization (Must / Should / Could / Won't)

**Best for:** Scoping a release or project. Establishing baselines and clear boundaries. Defining MVP.

### Definitions

| Category | Definition | % of Total | Consequence |
|----------|-----------|---|---|
| **Must** | Critical for this release. Non-negotiable. Project fails without it. | 20-30% | Missing a Must = feature incomplete or fails |
| **Should** | Very important. Significant value. Include if time/resources allow. | 40-50% | Missing a Should = reduced value, but still useful |
| **Could** | Nice-to-have. Differentiators. Only if time remains after Must/Should. | 15-20% | Missing a Could = acceptable; Plan for next release |
| **Won't** | Out of scope for this release. Future consideration. Explicitly deprioritized. | 5-10% | Won't items don't distract team; clear expectation |

### Example: E-Commerce Checkout Release

**Must (MVP - can't ship without):**
- [ ] Add items to cart
- [ ] Specify shipping address
- [ ] Enter payment info
- [ ] Process payment
- [ ] Order confirmation
- [ ] Security: SSL/HTTPS
- [ ] PCI compliance

**Should (adds value; plan to include):**
- [ ] Save addresses for reuse
- [ ] Guest checkout (no account)
- [ ] Apply coupon codes
- [ ] Multiple payment methods (Visa, Mastercard, Apple Pay)
- [ ] Real-time address validation
- [ ] Estimated delivery date

**Could (nice-to-have; include if time):**
- [ ] International shipping
- [ ] Gift wrapping options
- [ ] Live chat support
- [ ] Insurance/protection options
- [ ] Referral code input

**Won't (deferred; not in this release):**
- [ ] Installment payments (BNPL/Affirm)
- [ ] Cryptocurrency payments
- [ ] White-label checkout
- [ ] Multi-vendor checkout
- [ ] Subscription management

### Rules of Thumb

**The 25-40-20-15 Rule:**
- 25% of features = Must (critical path)
- 40% = Should (main value)
- 20% = Could (nice-to-have)
- 15% = Won't (future)

**If You Have:**
- All Must: Too strict; likely won't deliver enough value
- No Clear Musts: Feature is not MVP; break it into smaller scope
- Everything as Must: Scope creep; discipline yourself to say "Should" or "Could"

### Anti-Pattern: "Feature Creep"

**Problem:** Every stakeholder says "my feature is Must."

**Symptoms:**
- 50% of backlog labeled "Must"
- Release never finishes (always more Must items)
- Team frustrated; can't please everyone

**Solution:**
- Force hard decisions: "What if we skip X?"
- Use RICE as tiebreaker when disagreement on Must
- Revisit Must items every week; move struggling items to Should/Could
- Show impact of decisions: "Shipping Must A means delaying Feature B by 2 weeks"

---

## ICE Framework (Impact × Confidence × Ease)

**Best for:** Quick prioritization when effort estimation is hard. Bias toward delivering fast wins and momentum.

### Formula

```
ICE Score = Impact × Confidence × Ease

Interpretation:
- Score 8+: High priority; pursue
- Score 4-7: Medium priority; consider
- Score < 4: Low priority; deprioritize
```

### Component Scoring (1-10 scale)

**Impact: How much will this affect the key metric?**
- 10: Massive impact (expected to move metric 20%+)
- 7-9: Major impact (move metric 5-20%)
- 4-6: Moderate impact (move metric 1-5%)
- 1-3: Minor impact (move metric < 1%)

**Confidence: How sure are you about the impact?**
- 10: Very confident (validated with users; A/B tested similar features)
- 7-9: Confident (user research; market data)
- 4-6: Moderate confidence (some validation; logical reasoning)
- 1-3: Low confidence (untested; educated guess)

**Ease: How easy is it to implement? (1-10, where 10 = easiest)**
- 10: Trivial (config change, copy update, enable flag)
- 7-9: Easy (simple feature, one engineer, < 1 week)
- 4-6: Moderate (standard story, cross-team, 1-2 weeks)
- 1-3: Hard (complex, 1+ month, many dependencies)

### Worked Example

**Feature A: Add "Recommended for You" section to homepage**

```
Impact:       8 (Could increase engagement 10%; users like personalization)
Confidence:   6 (Good user feedback; but no data on conversion impact)
Ease:         7 (Design approved; data pipeline exists; one engineer, 5 days)

ICE = 8 × 6 × 7 = 336
```

**Feature B: Migrate database to new schema**

```
Impact:       4 (Improves performance slightly; mostly internal)
Confidence:   9 (Clear that migration needed; technical benefit known)
Ease:         2 (Complex, risky; 4 engineers, 6 weeks, potential downtime)

ICE = 4 × 9 × 2 = 72
```

**Ranking:**
1. Feature A: 336 (high ICE; fast win)
2. Feature B: 72 (lower ICE; necessary but slow)

### ICE vs. RICE

| Factor | ICE | RICE | When to Use |
|--------|-----|------|---|
| **Emphasis** | Biases toward quick wins | Balanced; considers scale | ICE if you want momentum; RICE if scale matters |
| **Effort variable** | Ease (1-10 scale) | Effort (weeks) | Ease is subjective; Effort is more precise |
| **Best for** | Startups, rapid iteration | Larger teams, complex products | Teams < 10 people: ICE. Teams > 30: RICE. |
| **Bias** | Toward shipping fast | Toward impact | Use ICE for "get something out fast"; RICE for long-term strategy |

---

## WSJF (Weighted Shortest Job First)

**Best for:** SAFe environments, large-scale programs, managing dependencies. Used in agile portfolio management.

### Formula

```
WSJF = Cost of Delay / Job Duration

Cost of Delay = User/Business Value + Time Criticality + Risk Reduction

Interpretation:
- Higher score = higher priority (do first)
- Captures both importance and urgency
- Accounts for dependencies and risk
```

### Component Scoring (1-10 scale)

**User/Business Value:** How much value does this deliver to users or business?
- 10: Strategic value; aligns with OKRs; major revenue impact
- 7-9: High value; important feature; clear benefits
- 4-6: Moderate value; nice-to-have; incremental benefit
- 1-3: Low value; niche; edge case

**Time Criticality:** How urgent is this? Does timing matter?
- 10: Critical; window of opportunity closing (seasonal, competitive threat)
- 7-9: Time-sensitive; better sooner than later (compliance deadline, market window)
- 4-6: Moderate urgency; timing somewhat important
- 1-3: Not time-sensitive; can wait (technical debt, nice-to-have)

**Risk Reduction:** How much does this reduce risk?
- 10: Eliminates major risk (security vulnerability, compliance issue)
- 7-9: Reduces significant risk (scaling bottleneck, data loss)
- 4-6: Reduces moderate risk (performance issue, UX friction)
- 1-3: Minimal risk reduction (edge case, minor issue)

**Job Duration:** How long to complete? (in days/weeks)
- 1: < 1 day (config change, quick bug fix)
- 2-3: 1-5 days (small feature, quick story)
- 5: 1-2 weeks (standard story)
- 10: 2-4 weeks (complex feature)
- 20: 1-2 months (epic)

### Worked Example

**Feature A: Add Two-Factor Authentication (2FA)**

```
User/Business Value:   9 (Security feature; competitive necessity; customers ask)
Time Criticality:      8 (Security trend; competitors have it; compliance concern)
Risk Reduction:        10 (Eliminates account takeover risk)

Cost of Delay = 9 + 8 + 10 = 27
Job Duration = 10 days
WSJF = 27 / 10 = 2.7
```

**Feature B: Dark Mode**

```
User/Business Value:   7 (Popular request; improves UX)
Time Criticality:      3 (Not time-sensitive; can ship anytime)
Risk Reduction:        1 (Doesn't reduce risk)

Cost of Delay = 7 + 3 + 1 = 11
Job Duration = 20 days
WSJF = 11 / 20 = 0.55
```

**Ranking:**
1. Feature A: 2.7 WSJF (Security; do first)
2. Feature B: 0.55 WSJF (Nice-to-have; do later)

---

## Kano Model (Basic vs. Performance vs. Delighters)

**Best for:** Understanding how features satisfy customers. Positioning features strategically.

### Three Dimensions

**Basic Needs (Hygiene Factors)**
- Definition: Expected to exist. Their absence causes dissatisfaction; their presence is expected.
- If Missing: Customer unhappy ("Why doesn't this basic feature work?")
- If Present: Customer not impressed ("Of course it works; that's the minimum")
- Example: App doesn't crash, login works, can view products
- Strategy: Table stakes; must get right, but won't differentiate

**Performance Needs (Satisfiers)**
- Definition: More is better. Correlation between presence and satisfaction.
- If Better: Customer happier ("This is faster/easier than competitors")
- If Worse: Customer unhappy ("Why is this so slow?")
- Example: Faster load times, easier checkout, clearer search results
- Strategy: Competitive ground; match or beat competitors

**Delighters (Exciters)**
- Definition: Unexpected. Their presence surprises and delights; their absence goes unnoticed.
- If Present: Customer impressed ("Wow, I didn't expect this!")
- If Absent: No dissatisfaction ("Didn't know I needed it")
- Example: Personalized product recommendations, surprise discounts, Easter eggs
- Strategy: Differentiation; create competitive advantage

### Example: E-Commerce App

| Feature | Category | Why | Priority |
|---------|----------|-----|----------|
| **Can browse products** | Basic | Expected; no competitive value | Must-have |
| **Can add to cart** | Basic | Assumed feature | Must-have |
| **Can checkout** | Basic | Table stakes | Must-have |
| **Fast load times** | Performance | Affects user experience; competitors vary | Should-have; competitive |
| **Mobile-responsive design** | Performance | Users expect; competitors offer | Should-have |
| **Product recommendations** | Delighter | "For you" section; surprise value | Nice-to-have; differentiator |
| **One-click checkout** | Delighter (becomes Performance) | Initially unexpected; now table stakes | Delighter → Performance over time |

### Implications for Prioritization

**Backlog Composition:**
- 50%: Basic needs (must-have; quality gate)
- 30%: Performance needs (competitive; delivers value)
- 20%: Delighters (differentiators; competitive advantage)

**When to Prioritize:**
- Basic Needs: Always; before features. If not done, everything else is moot.
- Performance Needs: When competitive gap exists; when customers expect it
- Delighters: When basic + performance are solid; when you need to stand out

**Note:** Delighters become expectations over time.
- Examples: Undo button (was delighter; now expected)
- One-click checkout (was delighter; competitors now offer)
- Dark mode (was delighter; now expected)

---

## Opportunity Scoring / Opportunity Solution Tree

**Best for:** Discovery; understanding user needs vs. satisfaction. Identifies hidden opportunities.

### Framework

```
Opportunity Score = Importance × (1 - Satisfaction)

Importance: How much does this matter to users? (1-10)
Satisfaction: How satisfied are users with current state? (0-1)

Interpretation:
- Score 9-10: High opportunity (important; low satisfaction)
- Score 5-8: Medium opportunity
- Score 1-4: Low opportunity (either not important or already satisfied)
```

### Example: Fitness App Features

| Feature | Importance (1-10) | Current Satisfaction (0-1) | Opportunity Score |
|---------|---|---|---|
| **Track workouts** | 10 | 0.9 | 10 × (1-0.9) = 1.0 (Low; well-satisfied) |
| **Social features** | 7 | 0.4 | 7 × (1-0.4) = 4.2 (Medium opportunity) |
| **Personalized recommendations** | 8 | 0.3 | 8 × (1-0.3) = 5.6 (High opportunity) |
| **Integration with wearables** | 9 | 0.2 | 9 × (1-0.2) = 7.2 (Highest opportunity) |
| **Community challenges** | 6 | 0.8 | 6 × (1-0.8) = 1.2 (Low; well-satisfied) |

**Ranking by Opportunity (highest first):**
1. Wearable integration: 7.2
2. Personalized recommendations: 5.6
3. Social features: 4.2
4. Community challenges: 1.2
5. Track workouts: 1.0

### Opportunity Solution Tree (OST)

**Visual tool to map customer outcomes → opportunities → solutions.**

```
Customer Outcome (goal a customer wants to achieve)
  ↓
Opportunities (ways to support this outcome; unmet needs)
  ├─ Opportunity 1
  ├─ Opportunity 2
  └─ Opportunity 3
       ↓
Solutions (features that enable the opportunity)
  ├─ Solution A
  ├─ Solution B
  └─ Solution C (e.g., "Add Fitbit integration")
```

**Example for Fitness App:**

```
Outcome: "I want to get healthier"
  ├─ Opportunity 1: "I don't know what workout to do"
  │  ├─ Solution A: Suggest workouts based on goals + ability
  │  ├─ Solution B: Video library with guided workouts
  │  └─ Solution C: Create custom plans with AI coach
  │
  ├─ Opportunity 2: "I lose motivation without external support"
  │  ├─ Solution A: Social leaderboards
  │  ├─ Solution B: Friend challenges
  │  └─ Solution C: Streaks + badges
  │
  └─ Opportunity 3: "I can't track progress across devices"
     ├─ Solution A: Sync data across devices
     ├─ Solution B: Integrate Fitbit/Garmin/Apple Watch
     └─ Solution C: Auto-sync from health apps
```

---

## Value vs. Effort Matrix (2x2)

**Best for:** Quick visual prioritization. Identify quick wins vs. major projects.

### The Matrix

```
                HIGH VALUE
                   │
                   │  Major Projects (Do last)
                   │  ████████████████
    Quick Wins     │  ██    (High effort, high value)
    ████████       │  ██
    ██            │
    ██            │  Fill-ins    │  Thankless Tasks
    ██ (Low effort,     ██████    │  ████████████
    HIGH value)    ██ (Low value)│  ██ (High effort,
                   │              LOW value)
        ────────────────────────────────────
            LOW EFFORT      HIGH EFFORT
```

### Quadrants

| Quadrant | Effort | Value | Strategy | Examples |
|----------|--------|-------|----------|----------|
| **Quick Wins** | Low | High | Do immediately; build momentum | Bug fix, copy change, small feature with big impact |
| **Major Projects** | High | High | Plan for future; break into sprints | Rewrite system, new platform, major feature |
| **Fill-ins** | Low | Low | Do in spare time; don't overinvest | Polish, minor improvements, technical debt reduction |
| **Thankless Tasks** | High | Low | Avoid or delegate; don't prioritize | Complex feature nobody wants, over-engineered solution |

### How to Run the Exercise

1. **List all features/initiatives** on sticky notes
2. **Have team place each on the matrix** (debate placement; consensus)
3. **Identify quadrants:**
   - Quick wins: Front of queue (do first)
   - Major projects: Plan carefully; sequence
   - Fill-ins: Backlog; low priority
   - Thankless: Question whether worth doing
4. **Create roadmap from the matrix**

**Caution:** What seems low-effort might not be. Estimate with engineers before committing.

---

## Decision Criteria Checklist

**Use this when deciding between competing features. Score each feature against these criteria.**

| Criterion | Importance | Feature A | Feature B | Feature C | Notes |
|-----------|-----------|----------|----------|----------|-------|
| **Strategic Alignment** | Critical | Aligns with OKR | Aligns with OKR | Tangential | Weight: 25% |
| **User Impact** | Critical | 10% conversion lift | 5% engagement lift | 2% NPS | Weight: 25% |
| **Revenue Impact** | High | $2M incremental | $500k incremental | $100k incremental | Weight: 20% |
| **Technical Risk** | High | Low risk | Medium risk | High risk | Weight: 15% |
| **Effort** | High | 4 weeks | 8 weeks | 12 weeks | Weight: 15% |
| **Time Sensitivity** | Medium | Urgent (Q2 launch) | Normal | Can wait | Weight: 10% |

**Scoring:** 1-10 scale; weight by importance; calculate average weighted score.

---

## Anti-Patterns in Prioritization

### Anti-Pattern 1: HiPPO (Highest Paid Person's Opinion)

**Problem:** CEO says "I want dark mode" → dark mode is top priority, regardless of data.

**Fix:**
- Data over opinions; back decisions with user research
- Use frameworks (RICE, ICE) that remove opinion bias
- Question assumptions; ask "why?" before accepting as truth

### Anti-Pattern 2: Recency Bias

**Problem:** Last feature approved becomes top priority; roadmap always shifts.

**Fix:**
- Establish prioritization process; stick to it
- Review prioritization monthly; don't change weekly
- Force trade-offs: "If we add Feature X, what do we drop?"

### Anti-Pattern 3: The Squeaky Wheel

**Problem:** Loudest customer/stakeholder gets prioritized, even if small segment.

**Fix:**
- Use reach metric (how many users affected?)
- Distinguish between vocal minority and actual user needs
- Track feature requests by volume; don't weight one person's request as 50 votes

### Anti-Pattern 4: Gold Plating

**Problem:** Scope bloat. Every feature gets "nice-to-have" additions; nothing ships.

**Fix:**
- Define MVP clearly; cut nice-to-haves
- Use MoSCoW; say "Won't" explicitly
- Ship MVP; gather feedback; build Phase 2 based on data

### Anti-Pattern 5: Everything is "Must"

**Problem:** When everything is critical, nothing is.

**Fix:**
- Force discipline; only 20-30% can be "Must"
- Ask hard question: "What if we skip this?"
- Use RICE/ICE to tiebreak

---

## Choosing a Framework

**Decision tree: Which framework should you use?**

```
Do you need to compare ~10+ features quickly?
  ├─ Yes → Use RICE (quantitative, scales)
  └─ No → Continue below

Is this a sprint planning exercise (MVP scope)?
  ├─ Yes → Use MoSCoW (clear boundaries)
  └─ No → Continue below

Are you in a SAFe environment with dependencies?
  ├─ Yes → Use WSJF (accounts for urgency + risk)
  └─ No → Continue below

Do you want to emphasize fast wins?
  ├─ Yes → Use ICE (biases toward ease)
  └─ No → Continue below

Do you need a visual prioritization?
  ├─ Yes → Use Value vs. Effort Matrix (quick, visual)
  └─ No → Continue below

Are you in discovery mode (understanding user needs)?
  ├─ Yes → Use Opportunity Scoring / OST
  └─ No → Use RICE (best general-purpose framework)
```

---

## Summary Recommendations

**By Team Size:**
- **Small teams (< 10):** ICE or Value vs. Effort (simple, quick)
- **Medium teams (10-30):** RICE (balanced, quantitative)
- **Large organizations (30+):** RICE + WSJF (scaled framework)

**By Product Stage:**
- **Early stage (MVP):** MoSCoW (define scope) + Opportunity Scoring (find opportunities)
- **Growth stage:** RICE (optimize for impact + scale)
- **Mature stage:** WSJF (manage dependencies, strategic alignment)

**Best Practice:** Combine frameworks. Use MoSCoW to define MVP, then RICE to prioritize within each category (Must items ranked by RICE within Must scope).

