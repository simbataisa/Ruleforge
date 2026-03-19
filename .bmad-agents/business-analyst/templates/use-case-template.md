# Use Case Template

A structured template for documenting a use case during the Analysis phase of BMAD (Business, Mobile Engineer, Analyst, Designer). This document describes how an actor (user, system, or external service) interacts with the system to achieve a specific goal.

---

## Use Case Header

| Field | Value |
|-------|-------|
| **Use Case ID** | UC-XXX (e.g., UC-001) |
| **Title** | [Clear, goal-oriented title, e.g., "User Searches for Products"] |
| **Actor(s)** | Primary: [User/System/External], Secondary: [List other actors] |
| **Related Epic/Feature** | [Link to epic ID or feature name] |
| **Author** | [BA name] |
| **Date Created** | [YYYY-MM-DD] |
| **Last Updated** | [YYYY-MM-DD] |
| **Status** | Draft / Review / Approved / Implemented |
| **Priority** | Critical / High / Medium / Low |
| **Complexity** | Simple / Moderate / Complex |

---

## Brief Description

[1-2 sentence summary of what this use case covers. State the goal clearly.]

Example: "A registered user searches for products by keyword, filters results by category and price, and views detailed product information. The system queries its database and returns matching results in real-time."

---

## Stakeholders and Interests

| Stakeholder | Role/Department | Interest/Concern | Priority |
|-------------|-----------------|------------------|----------|
| **End User** | Customer | Fast search, accurate results, easy filtering | High |
| **Product Manager** | Product Team | User engagement, search volume metrics, feature adoption | High |
| **Marketing Team** | Marketing | Search visibility, promotional content placement | Medium |
| **Search Engineer** | Engineering | Indexing performance, latency < 500ms, scale | High |
| **Analytics Team** | Data | Search abandonment rates, zero-results rate | Medium |
| **Legal** | Compliance | GDPR: search queries logged responsibly | Low |

---

## Preconditions

**What must be true before this use case begins. System and user state assumptions.**

- [ ] User is registered and authenticated (session active)
- [ ] User is on the Product Search screen
- [ ] Product catalog is loaded and indexed in search engine
- [ ] User has valid network connection
- [ ] Session timeout not exceeded (no re-login needed)
- [ ] At least 10 products exist in the catalog

---

## Postconditions — Success and Failure

### Success Postcondition
**When main success scenario completes:**
- Search results are displayed to user
- Results are ranked by relevance
- User can proceed to product detail or refine search
- Search query logged for analytics
- User's search history updated

### Failure Postcondition
**When the use case fails:**
- Error message displayed (not technical jargon)
- User offered option to retry or modify search
- Search query logged for analysis (why did search fail?)
- Support contact info shown if persistent errors

---

## Main Success Scenario

**Step-by-step: actor action → system response. Numbered, sequential.**

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | User | Taps search bar on home screen | Focus placed on search input; keyboard shows; search suggestions appear (trending searches, recent searches) |
| 2 | User | Types search keyword "running shoes" | As user types, search field updates; system filters suggestions by query |
| 3 | User | Taps Search button (or presses Enter) | System initiates search; loading spinner appears; "Searching..." message |
| 4 | System | [Backend] Queries product index by keyword | Database returns matching products: IDs, names, prices, ratings (top 50 by relevance) |
| 5 | System | [Backend] Ranks results by relevance score | Results ordered: exact match → partial match → fuzzy match; popular items ranked higher |
| 6 | System | [UI] Returns results to mobile app | Search results displayed in grid/list view; shows: product image, name, price, star rating, # of reviews |
| 7 | User | Sees results; optionally scrolls or filters | User can scroll to see more results, or tap filters (category, price range, rating) |
| 8 | User | [Optional] Taps product to view details | Navigates to Product Detail screen (UC-002) |
| 9 | System | Logs search event | Analytics: logged query, result count, time elapsed, user ID |

---

## Alternative Flows

**Variations on the main scenario. Numbered sub-flows branching at specific steps.**

### Alternative Flow 1: User Applies Filters Before/After Search

**Branch at Step 3 (before search initiated):**

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 3a | User | Taps "Filters" button before submitting search | Filter panel opens; user sees: category (checkboxes), price range (slider), rating (stars) |
| 3b | User | Selects: Category = Running, Price = $50-$150, Rating >= 4 stars | Filter selections reflected on UI |
| 3c | User | Taps "Apply Filters" | Filter criteria added to search query; proceeds to step 4 |
| 4 | System | Queries index with keyword AND filter constraints | Returns products matching both keyword and filter criteria |
| 5-9 | [Continue as main scenario] | | |

---

### Alternative Flow 2: User Searches Using Voice

**Branch at Step 2 (instead of typing):**

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 2a | User | Taps microphone icon in search bar | Microphone permissions requested (if not already granted); recording begins; visual cue shown |
| 2b | User | Speaks search query: "I want cheap running shoes" | System records audio; shows waveform |
| 2c | User | Stops speaking; taps "Done" | System converts speech to text using speech-to-text API (Google, Apple, custom) |
| 2d | System | [Backend] Processes voice input | Returns recognized text: "I want cheap running shoes"; confidence score: 92% |
| 2e | System | [UI] Displays recognized text in search field | Allows user to edit if recognition incorrect, or proceed |
| 2f | User | [If satisfied] Taps Search or auto-submits | Proceeds to step 4 |

**Possible error:** Voice recognition confidence < 80%; system asks user to repeat or type instead.

---

### Alternative Flow 3: Empty Results

**Branch at Step 4 (no matching products):**

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 4a | System | Queries product index | No products match search criteria |
| 4b | System | [Backend] Returns empty result set | Result count = 0 |
| 4c | System | [UI] Shows empty state screen | Displays: Icon, "No results found for 'xyz'", suggestions to refine search, link to browse all categories |
| 4d | User | [Option 1] Refines search (e.g., removes filter) | User taps "Remove filters" or edits search query; returns to step 2 |
| 4e | User | [Option 2] Browses categories | User taps "Browse All" or "Shop by Category"; navigates away from search |
| 4f | System | Logs "zero results" event | Analytics flag for team; helps identify gaps in catalog |

---

## Exception Flows

**Error conditions and system recovery. Numbered, describing error and response.**

### Exception 1: Network Timeout

**Trigger:** Backend search query takes > 10 seconds; request times out.

| Step | Condition | System Response | User Impact |
|------|-----------|-----------------|-------------|
| E1-1 | Request timeout | Cancel request; return error to UI | Search stops; spinner disappears |
| E1-2 | System sends error response | Error message: "Search took too long. Please try again." | User sees error toast/alert |
| E1-3 | User's action | Taps "Retry" button | Search re-submitted (new request) |
| E1-4 | If retry succeeds | Results displayed normally | Proceed to step 6 |
| E1-5 | If retry fails again | Show "Search unavailable. Please try later." with support contact | User leaves search screen; returns later |

**Logging:** Log timeout with query, timestamp, error details for engineering analysis.

---

### Exception 2: Database Connection Failure

**Trigger:** Search service cannot connect to product database.

| Step | Condition | System Response | User Impact |
|------|-----------|-----------------|-------------|
| E2-1 | DB connection error | Search service returns 500 error | UI receives server error response |
| E2-2 | System sends error to UI | Error message: "Our servers are busy. Try again soon." | User sees error; cannot search |
| E2-3 | User's action | Taps "Retry" | Request re-queued |
| E2-4 | If DB recovers | Search succeeds on retry | Proceed to step 6 |
| E2-5 | If DB down > 30 minutes | Analytics alerts on-call engineer; incident logged | Engineering investigates; user message updated to "Maintenance in progress" |

---

### Exception 3: Invalid Input / Injection Attempt

**Trigger:** User enters special characters or SQL-like strings in search query.

| Step | Condition | System Response | User Impact |
|------|-----------|-----------------|-------------|
| E3-1 | User enters: `search = "running; DROP TABLE products--"` | Input sanitization applied (remove/escape special chars) | Query becomes "running DROP TABLE products" |
| E3-2 | System searches for literal string "running DROP TABLE products" | No products match (obviously) | Empty results displayed (not a database error) |
| E3-3 | User's action | Refines search to "running" | Normal results shown |

**Security:** All inputs parameterized (never concatenate user input into SQL). Use prepared statements.

---

### Exception 4: Search Index Stale

**Trigger:** New products added to catalog but search index not updated.

| Step | Condition | System Response | User Impact |
|------|-----------|-----------------|-------------|
| E4-1 | Product uploaded; DB updated | Search index has not yet re-indexed new product | User cannot find newly added product by search |
| E4-2 | System detects sync delay | Background job queued to re-index | Takes 1-5 minutes (depends on index size) |
| E4-3 | Index rebuilds | New product now searchable | User who searches again will find it |

**Monitoring:** Track "index freshness"; alert if delay > 30 minutes. Log to data team.

---

## Business Rules

Specific policies or constraints that govern this use case. Linked to business or regulatory requirements.

| Rule ID | Description | Source / Owner | Impact |
|---------|-------------|-----------------|--------|
| **BR-1** | Search is available only to authenticated users | Product Policy | Unauthenticated users cannot access search; redirected to login |
| **BR-2** | Free tier users see up to 50 results; Premium users see 500 | Pricing Model | Limit displayed in UI; API enforces server-side |
| **BR-3** | Products with zero inventory not shown in search results | Catalog Policy | Prevents out-of-stock disappointment |
| **BR-4** | Search queries older than 90 days are auto-deleted | Privacy/GDPR | Reduces data retention risk; user history cleared |
| **BR-5** | Search results ranked by relevance first, then by rating | Ranking Algorithm | Popular/highly-rated results prioritized |
| **BR-6** | User can see results only for products in their region | Geo-fencing / Regulation | Prevents shipping to unavailable areas |

---

## Data Requirements

### Inputs

**Data provided by actor or fetched from system before use case starts:**

- **Search Query:** String, required, min 1 char, max 100 chars (e.g., "running shoes")
- **Filters (optional):**
  - Category: List of category IDs (e.g., ["shoes", "apparel"])
  - Price Range: Min/Max decimal (e.g., $50-$150)
  - Rating: Decimal 0-5.0 (e.g., >= 4.0 stars)
  - In Stock: Boolean (true/false)
- **Pagination (optional):** Page number (1, 2, 3, ...), limit (default 20, max 100)
- **Sort Order (optional):** relevance, price_asc, price_desc, rating, newest

### Outputs

**Data returned to actor after use case completes:**

- **Search Results:** Array of product objects
  - Product ID (string)
  - Name (string)
  - Description (string, first 100 chars)
  - Price (decimal, USD)
  - Currency (string, e.g., "USD")
  - Image URL (string, thumbnail)
  - Star Rating (decimal, 0-5.0)
  - Review Count (integer)
  - In Stock (boolean)
  - Relevance Score (0-100, internal, not shown to user)

- **Metadata:**
  - Total Result Count (integer)
  - Page Number (integer)
  - Showing Results X-Y of Z
  - Search Time (milliseconds, displayed as "Found in 0.3s")

### Data Entities Touched

| Entity | CRUD | Reason |
|--------|------|--------|
| **Product** | Read | Fetch product data for results |
| **ProductInventory** | Read | Check if in stock |
| **ProductRating** | Read | Retrieve average rating, review count |
| **SearchQuery** | Create | Log search for analytics |
| **UserSearchHistory** | Update | Add query to user's search history |

---

## Non-Functional Requirements

### Performance

- **Search Response Time:** < 500ms (P95) from user pressing Search to results visible
- **Index Freshness:** Product index updated within 5 minutes of catalog change
- **Concurrent Users:** Support 10,000 concurrent searches at peak (e.g., during sale)
- **Latency SLA:** 99th percentile < 1.5 seconds

### Security

- **Input Validation:** All search queries sanitized; no SQL injection possible
- **Authentication:** User session required; token must be valid
- **Rate Limiting:** Max 10 searches per user per minute (prevent abuse)
- **Data Masking:** User search queries logged but not exposed in third-party analytics
- **Encryption:** Search queries in transit (HTTPS); stored queries encrypted at rest

### Compliance

- **Data Retention:** Search queries deleted after 90 days (GDPR Article 5)
- **Audit Logging:** All searches logged with user ID, timestamp, query, result count
- **Accessibility:** Search bar keyboard accessible; results screen compatible with screen readers (VoiceOver, TalkBack)

### Usability

- **Response Feedback:** Loading spinner shown if search > 1 second
- **Error Messages:** Non-technical, actionable (not "500 Internal Server Error")
- **Empty State:** Helpful guidance if no results (suggestions, link to browse)
- **Mobile-First:** Search optimized for mobile; not just desktop redesigned

---

## Open Questions Log

Questions raised during analysis that block implementation or acceptance. Tracked to closure.

| Question | Raised By | Date | Answer | Resolved Date |
|----------|-----------|------|--------|---------------|
| How are results ranked? Relevance only, or with business ranking (promotions)? | Engineering | 2024-03-01 | Relevance primary; promotions shown separately ("Sponsored" badge) | 2024-03-05 |
| Should we support fuzzy search (typos)? "runing" → "running"? | Product | 2024-03-01 | Yes; fuzzy with 2-char max edit distance | 2024-03-10 |
| What's max search query length? | Engineering | 2024-03-02 | 100 characters; longer queries truncated | 2024-03-05 |
| Do free users have daily search limits? | Product | 2024-03-03 | No hard limit; rate limited at 10/minute per user | 2024-03-08 |
| Should search suggest spell corrections? | UX | 2024-03-04 | Yes; "Did you mean: ..." shown if no results | TBD |
| How to handle seasonal categories (e.g., "winter coats" in summer)? | Product | 2024-03-05 | Categories always searchable; "out of season" flag in results | TBD |

---

## Related Use Cases

**Dependencies and cross-references. Shows how this use case connects to others.**

| Related Use Case | Type | Description |
|------------------|------|-------------|
| **UC-002: View Product Detail** | Extends | User taps product from search results; navigates to detail view |
| **UC-003: Add Product to Cart** | Extends | From product detail (reached via search), user adds to cart |
| **UC-004: User Registration** | Includes | User must be registered before accessing search |
| **UC-005: Refine Search Filters** | Includes | Applied mid-search to narrow results |
| **UC-006: View Search History** | Related | System stores user's past searches; retrievable in separate flow |
| **UC-007: Admin: Update Product Catalog** | Related (async) | When admin uploads products, search index async-updated; affects this UC's availability |

---

## Acceptance Criteria

**Conditions that must be met for implementation to be accepted.**

- [ ] Search results display within 500ms (P95) from query submission
- [ ] Empty state shown when no results match query
- [ ] Network timeout handled gracefully; user offered retry
- [ ] Search query logged to analytics with timestamp, user ID, result count
- [ ] All user inputs sanitized (no SQL injection possible)
- [ ] Results display up to 100 products per page (max configurable)
- [ ] Filters (category, price, rating) working and applying correctly
- [ ] Search bar automatically focused on screen load
- [ ] Keyboard appears on iOS/Android when search bar tapped
- [ ] Voice search converts speech to text (if supported)
- [ ] Zero-result state includes link to browse categories or show suggestions
- [ ] Accessibility: Screen reader (VoiceOver/TalkBack) announces results correctly
- [ ] Results ranked by relevance; "Sponsored" products labeled separately
- [ ] Search works offline (displays cached results from last session)
- [ ] Rate limiting enforced (10 searches/minute per user)

---

## Notes for Implementation Team

- Search uses Elasticsearch backend (or Algolia service); ensure indexing pipeline updated when new products added
- Design: Search screen is light/minimal; avoid clutter to not distract from results
- API endpoint: `GET /api/v2/products/search?q={query}&filters={json}&page={n}`
- Response format: JSON; include pagination metadata
- Caching: Cache results for same query for 1 hour (stale data acceptable)
- Mobile-first: Test on slow networks (throttle to 3G in DevTools)
- Metrics to track in Datadog: search latency, zero-result rate, top 100 queries, filter usage

