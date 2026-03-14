# Code Review Template - Structured Review Checklist

## Review Metadata

| Field | Value |
|-------|-------|
| **Pull Request Link** | https://github.com/org/repo/pull/12345 |
| **PR Title** | Feature: Add email notification system for user alerts |
| **Author** | developer@example.com |
| **Reviewer** | tech-lead@example.com |
| **Review Date** | 2024-03-14 |
| **Related Story/Ticket** | FEAT-2847 |
| **PR Type** | Feature / Bugfix / Refactor / Hotfix / Chore / Docs |
| **Lines Changed** | +487, -142 (629 total) |
| **Files Modified** | 8 files |
| **Branch** | feature/email-notifications |
| **Target Branch** | main (merge into production) |

---

## PR Summary (Filled by Author)

**What does this PR do?**

Implement email notification system for user alerts. When a user receives an important system notification, an email is automatically sent based on their notification preferences.

**Key changes:**
- New `NotificationService` handles email delivery via SendGrid
- User preference model added to store notification opt-in/opt-out settings
- Background job worker processes notification queue (uses Celery)
- Email templates for 5 notification types (new message, milestone reached, admin alert, etc.)
- Database migration to add `user_notification_preferences` table
- Integration tests for email delivery with mocked SendGrid

**Testing:**
- 34 new unit tests covering service logic, edge cases
- 8 integration tests with Testcontainers for database and mocked SendGrid
- Manual testing on staging environment
- E2E tests verify end-to-end notification flow

**Performance impact:**
- Email sending is async (background job), zero impact on request latency
- No additional database queries in request path
- Estimated 100ms average per email send (async, not blocking)

---

## Review Checklist

### Correctness & Logic (10 items)

- [ ] **Logic is correct:** All business logic is sound. Calculations are accurate. No off-by-one errors or incorrect conditionals. For this PR: notification conditions check user preferences correctly, email queue processing handles retries properly.

- [ ] **Edge cases are handled:** Null/empty values, boundary conditions, missing optional fields. Example: What happens if user has no email address? (Gracefully skip, log warning) What if SendGrid times out? (Retry with exponential backoff)

- [ ] **Error handling is comprehensive:** All error paths are handled. No silent failures. Exceptions are caught and logged appropriately. Example: If email send fails, exception is caught, logged to Sentry, message placed back in queue for retry.

- [ ] **No silent failures:** Errors are not swallowed. All operations have proper error propagation and logging. Bad: `try { email.send(); } catch(e) {}` Good: `try { email.send(); } catch(e) { logger.error('Email send failed', e); throw e; }`

- [ ] **Race conditions are prevented:** If code runs concurrently, there are no race conditions. Example: Two jobs processing same notification ID simultaneously - handled with database locks or unique constraint.

- [ ] **Null safety:** No NullPointerException or AttributeError from null values. All nullable fields are checked before use. Example: `notification.user.email` should check user exists and has email address.

- [ ] **Input validation:** User input is validated before processing. Malicious input cannot cause issues. Example: Email addresses are validated format, notification IDs are checked exist before processing.

- [ ] **Output sanitisation:** Output is safe. No XSS if output goes to frontend. No SQL injection if data goes to database. Example: Email template content is escaped properly.

- [ ] **Consistency:** Code is consistent with existing patterns in codebase. Example: Error handling patterns match other services, logging format is consistent.

- [ ] **Idempotency:** If operation is called multiple times, result is same. Critical for background jobs. Example: Sending same notification twice results in one email, not two.

---

### Design & Architecture (8 items)

- [ ] **Single Responsibility Principle:** Each class/function has one reason to change. `NotificationService` only handles notifications. `EmailTemplate` only manages template rendering. Not mixing concerns.

- [ ] **No tight coupling:** Components are loosely coupled. Easy to test in isolation. Easy to swap implementations. Example: `NotificationService` depends on abstract `EmailProvider` interface, not concrete SendGrid class. Can swap providers without changing service.

- [ ] **Follows established patterns:** Adheres to patterns already used in codebase. Doesn't introduce new paradigms inconsistently. Example: If codebase uses dependency injection, PR uses DI. If codebase uses service classes, PR uses service class.

- [ ] **Abstraction is appropriate:** Not over-engineered with unnecessary layers. Not under-engineered missing necessary abstraction. Example: `NotificationService` is abstract enough that could swap backends (email → SMS → push notifications).

- [ ] **No premature optimization:** Code is not optimized for hypothetical scenarios that aren't bottlenecks. Readability not sacrificed for micro-optimization. Example: Code doesn't pre-allocate arrays or use bit flags for no measurable performance gain.

- [ ] **Dependency direction is correct:** High-level modules don't depend on low-level modules. Both depend on abstractions. Example: `NotificationService` depends on `EmailProvider` interface, not on `SendGridEmailProvider` concrete class.

- [ ] **Separation of concerns:** Business logic separated from infrastructure. Example: Queue processing logic separated from email delivery logic. Notification rules separated from template rendering.

- [ ] **Framework/library use is appropriate:** Libraries are used correctly and for right reasons. Not doing manually what framework provides. Example: Using Celery for background jobs, not rolling custom job queue.

---

### Code Quality (8 items)

- [ ] **Naming is clear and consistent:** Variable, function, and class names are descriptive. Naming follows established conventions (camelCase/snake_case consistency). Example: `send_notification_email()` is clear. `sne()` would be cryptic.

- [ ] **No magic numbers/strings:** Hard-coded values are replaced with named constants. Example: Instead of `if retry_count > 3`, use `MAX_RETRY_ATTEMPTS = 3; if retry_count > MAX_RETRY_ATTEMPTS`

- [ ] **No dead code:** Removed unused variables, functions, commented-out code blocks. Bad: `# old_notification_logic()` (commented code) Good: Use git history to see old code.

- [ ] **No commented-out code:** Code is not left commented out. If it's not needed, delete it. If it might be needed, git has history. Exception: Legitimate comments explaining complex logic.

- [ ] **DRY without over-abstraction:** No duplicate code. But don't create unnecessary abstractions. Example: Three notification types share 80% logic → Extract to base class. Two classes share one line → Don't extract (DRY violation but premature abstraction).

- [ ] **Cyclomatic complexity is low:** Code is not overly nested. Cognitive load is manageable. Target: Cyclomatic complexity <10 per function. Too complex: 10 nested if statements. Better: Early return, extract methods, use decision tables.

- [ ] **Functions are reasonably sized:** Not too long (>50 lines), not too short (<5 lines). Sweet spot: 15-30 lines per function. Too long: Break into smaller functions. Too short: Might be extracting too aggressively.

- [ ] **Code is readable:** First-time readers can understand the code without external documentation. Code is self-explanatory through clear naming and structure.

---

### Testing (8 items)

- [ ] **Unit tests are present:** New logic has corresponding unit tests. Example: `NotificationService` has unit tests for `send_notification()`, `should_skip_notification()`, etc.

- [ ] **Edge cases are tested:** Not just happy path. Null inputs, boundary values, error conditions tested. Example: Test with empty recipient list, null notification, SendGrid timeout.

- [ ] **Tests are meaningful:** Tests verify actual behavior, not just that code runs. Bad: `assert notification_service is not None` (just verifies object creation). Good: `assert email_sent_to_correct_recipient()` and `assert email_content_matches_template()`

- [ ] **No skipped tests:** Tests are not skipped (`@skip`, `@pytest.mark.skip`, `xit` in Mocha) without good reason. If reason is documented, acceptable. Example: `@skip("TODO: mock third-party API")` is acceptable with clear plan.

- [ ] **Test names describe behavior:** Test names explain what is being tested. Pattern: `should_doX_when_Y` or `test_sending_notification_with_invalid_email()`. Not just: `test_notification()` or `test_1()`.

- [ ] **Mocks are appropriate:** External dependencies are mocked (SendGrid, database). Internal logic is not mocked (defeats purpose of unit test). Example: Mock the `EmailProvider.send()` method. Don't mock internal validation logic.

- [ ] **Test coverage is adequate:** New code paths are covered by tests. Coverage tool shows ≥80% line coverage for new code. Check `nyc`, `coverage.py`, `JaCoCo` reports.

- [ ] **Integration tests exist for critical paths:** Not just unit tests. Critical features like email sending have integration tests. Example: Integration test with Testcontainers verifies email saved to database and queued correctly.

---

### Security (6 items)

- [ ] **No secrets in code:** API keys, passwords, connection strings are not hardcoded. Stored in environment variables or secure vault. Example: `sendgrid_api_key = os.environ.get('SENDGRID_API_KEY')` not `sendgrid_api_key = 'SG_12345abc...'`

- [ ] **Input is validated:** User input checked for expected format/length before processing. Example: Email addresses validated, notification IDs checked exist.

- [ ] **SQL injection prevented:** If using SQL, parameterized queries used. No string concatenation of SQL. Good: `SELECT * FROM users WHERE id = %s`, params=[user_id]`. Bad: `SELECT * FROM users WHERE id = '` + user_id + `'`

- [ ] **Authentication checked:** User is authenticated and authorized for action. Example: User can only see their own notifications. Admin-only operations have permission check.

- [ ] **PII not logged:** Personally identifiable information is not logged. Email addresses, phone numbers, API keys not in logs. Bad: `logger.info(f'Sending email to {user.email}')` Good: `logger.info(f'Sending email to user {user.id}')`

- [ ] **Dependencies are not introducing vulnerabilities:** No dependencies with known critical vulnerabilities. Run `npm audit`, `snyk test`, `pip audit`. Check GitHub Dependabot alerts.

---

### Performance (5 items)

- [ ] **No N+1 queries:** If iterating over records, database is not queried in loop. Example: Bad: `for notification in notifications: send_email(notification.user.email)` (queries user for each notification). Good: Prefetch users with `select_related()` or `prefetch_related()`.

- [ ] **No unnecessary network calls in loops:** API calls not made in loops. Example: Bad: `for item in items: price = fetch_from_api(item.id)` Good: Batch request all at once.

- [ ] **Pagination used for large datasets:** Query results are paginated if potentially large. Not loading all 1 million records into memory. Example: `User.objects.all().paginate(page=1, per_page=100)` not `User.objects.all()`

- [ ] **Caching used appropriately:** Expensive operations are cached if result is stable. Example: Email templates cached, user preferences cached with TTL.

- [ ] **No blocking operations on main thread:** Long operations run asynchronously. Main request handler not blocked. Example: Email sending is async job, not sent in request handler.

---

### Documentation (4 items)

- [ ] **Public API is documented:** Classes, functions, modules have docstrings/comments explaining purpose and usage. Example: `NotificationService.send_notification(user_id, notification_type)` has docstring explaining parameters and return value.

- [ ] **Complex logic has inline comments:** Non-obvious algorithms or business logic is explained. Example: Complex retry logic with exponential backoff has comments explaining the math.

- [ ] **README updated if needed:** If feature changes user-facing behavior or deployment, README/docs updated. Example: New environment variables documented in README.

- [ ] **Architecture Decision Record (ADR) created if major decision:** If architectural decision made (new service, new database, new pattern), ADR is created documenting decision, alternatives considered, rationale.

---

## Review Notes

**Free text feedback from reviewer:**

### Strengths
- Clean separation between notification logic and email delivery
- Comprehensive error handling with proper logging
- Good test coverage (34 unit + 8 integration tests)
- Follows established service pattern used in other features
- Async email sending means zero impact on request latency

### Issues Found

**Critical (must fix before merge):**
1. **SQL Injection vulnerability in notification filtering:** Line 127 in `notification_service.py` uses string concatenation for SQL. Should use parameterized query.
   ```python
   # Current (vulnerable):
   query = f"SELECT * FROM notifications WHERE user_id = {user_id}"

   # Should be:
   query = "SELECT * FROM notifications WHERE user_id = %s"
   cursor.execute(query, [user_id])
   ```

2. **Missing null check on user email:** Line 45 sends email without verifying user has email address. Could send null value to SendGrid.
   ```python
   # Add check:
   if not user.email:
       logger.warning(f"User {user.id} has no email, skipping notification")
       return
   ```

**High Priority (should fix before merge):**
3. **N+1 query issue:** In `get_notification_recipients()`, user is loaded for each notification. Use prefetch_related().
4. **Missing @retry decorator on SendGrid call:** Email sends to SendGrid without retry logic. Add exponential backoff retry.

**Medium Priority (nice to fix):**
5. **Test naming inconsistency:** Some tests use `test_`, some use `should_`. Standardize on `test_` or `should_` format.
6. **Email template strings are not translatable:** Hard-coded English text in email templates. Consider i18n for future translations.

**Low Priority (suggestions):**
7. **Comment in `background_job_worker()` could be more specific:** "Process queue" is vague. Explain what processing entails.

### Questions for Author
1. How are failed email sends handled? Are they retried indefinitely or only N times?
2. What is the SLA for email delivery? How quickly should user see email after triggering notification?
3. How are bounced emails handled? If user email bounces repeatedly, do we disable their email notifications?
4. Are there any rate limits on SendGrid account? What happens if we hit rate limit?

---

## Action Items

| Item | Owner | Priority | Resolved |
|------|-------|----------|----------|
| Fix SQL injection on line 127 | developer@example.com | Critical | No |
| Add null check for user.email on line 45 | developer@example.com | Critical | No |
| Fix N+1 query in get_notification_recipients() | developer@example.com | High | No |
| Add retry decorator to SendGrid call | developer@example.com | High | No |
| Standardize test naming (test_ vs should_) | developer@example.com | Medium | No |
| Answer email failure SLA questions | developer@example.com | High | No |

---

## Review Decision

**Status: REQUEST CHANGES**

This PR has valuable functionality but needs fixes before merging:
- Critical security issue (SQL injection)
- Critical bug (null pointer on email)
- High priority performance issue (N+1 query)

Once these are fixed and CI passes, this will be ready to merge.

**Path to approval:**
1. Fix critical security/bug issues
2. Push fixes to branch (automated CI will re-run)
3. Reply to review comments confirming changes
4. Request re-review when ready

**Typical outcomes:**
- Approve: All checklist items pass, no blocking issues
- Request Changes: Blocking issues found, must fix before merge
- Comment: Minor suggestions, doesn't block merge

---

## Post-Merge Checklist (Tech Lead Responsibility)

- [ ] **Monitoring confirmed:** Dashboard shows email send metrics, error rates monitored
- [ ] **Feature flag configured:** Feature is behind feature flag in case rollback needed. Environment variable set in production.
- [ ] **Rollback plan documented:** If feature causes issues, rollback procedure is documented and tested
- [ ] **On-call briefed:** On-call engineers know about new feature, what to watch for, who to escalate to
- [ ] **Alerts configured:** SendGrid errors, email queue backlog, high latency alerts set up in monitoring system
- [ ] **Observability in place:** Email sending logs are centralized, query patterns are tracked, performance metrics collected
- [ ] **Performance baseline captured:** Before/after request latency, database query counts, SendGrid API call volume documented
- [ ] **Customer communication:** Release notes published if this is customer-facing feature
- [ ] **Documentation updated:** Team docs/runbook updated with new feature details, troubleshooting steps
- [ ] **Database migration safe:** Migration tested on staging, no production impact, rollback procedure confirmed

---

## Code Review Best Practices

### For Reviewers
1. **Be thorough but timely:** Spend 30-45 minutes on review. Deep reviews are good, infinite perfectionalism is not.
2. **Assume good intent:** Author did their best. Frame feedback constructively: "Consider using parameterized queries for security" not "This is a security disaster."
3. **Distinguish blocking from suggestions:** Critical issues must be fixed. Suggestions are optional improvements.
4. **Ask questions:** "How are edge cases handled?" is better feedback than "Fix it."
5. **Praise good work:** "Clean separation of concerns here" encourages good practices.

### For Authors
1. **Respond to feedback:** Don't ignore review comments. Engage in discussion.
2. **Keep PRs small:** Easier to review 200 line PR than 2000 line PR.
3. **Write clear PR description:** Make reviewer's job easier with context.
4. **Request re-review when done:** Don't assume reviewer will check again. Leave comment "Ready for re-review, fixed all issues."
5. **Learn from feedback:** Patterns will emerge. If reviewer mentions N+1 queries repeatedly, you're probably writing them incorrectly.

### Escalation Policy
- **Minor style nit:** Approve but comment
- **Smells like bug:** Request changes
- **Security issue:** Request changes, escalate to security team
- **Architectural decision:** Request changes, discuss with whole team if disagreement
- **Blocked >24 hours:** Escalate to manager, may approve with follow-up
