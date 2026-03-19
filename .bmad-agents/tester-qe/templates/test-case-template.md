# Test Case Template

## Test Case Metadata

| Field | Value |
|-------|-------|
| **Test Case ID** | TC-XXX (e.g., TC-USR-001) |
| **Title** | Clear, concise description of what is being tested |
| **Feature/Epic** | Name of feature or epic this test covers |
| **Related Story/Issue ID** | JIRA ticket (e.g., USER-123) |
| **Test Type** | Unit / Integration / E2E / Performance / Security / Accessibility |
| **Priority** | P0 (Critical) / P1 (High) / P2 (Medium) / P3 (Low) |
| **Status** | Draft / Ready / In Review / Approved / Obsolete |
| **Author** | QE Engineer name |
| **Created Date** | YYYY-MM-DD |
| **Last Modified Date** | YYYY-MM-DD |
| **Automation Status** | Not Automated / Automatable / Automated |
| **Automation Framework** | Selenium / Playwright / Cypress / JUnit / pytest / Custom |

---

## Test Objective

**What is this test case verifying?**

Provide a clear, single-sentence statement of the goal. Example:
- "Verify that a user can successfully complete a purchase with a valid credit card"
- "Verify that the API returns a 400 Bad Request when email format is invalid"
- "Verify that the system enforces a maximum of 10 concurrent sessions per user"

---

## Prerequisites

**Environment and Setup Requirements**

### Environment State
- Environment: Development / Staging / Production
- Application Version: X.Y.Z
- API Endpoint: https://api.example.com/v1
- Database State: Existing records required / Specific test data needed

### User Permissions and Roles
- Required Role: Admin / Manager / User / Guest
- Required Permissions: List specific permissions needed

### Test Data and Account Setup
- Test User Account: test-user-1@example.com / credentials stored in vault
- Test User Role: [Specific role with specific permissions]
- Required Database Records: List any pre-existing data needed

### External System Dependencies
- Payment Gateway: Mock active (Stripe Sandbox)
- Email Service: Mock active
- Third-party APIs: Status and version

### Browser/Device Requirements (if applicable)
- Browser: Chrome 120+, Firefox 121+, Safari 17+
- Mobile Device: iOS 16+ / Android 12+
- Screen Resolution: 1920x1080 (desktop) / 375x812 (mobile)

---

## Test Data

| Field Name | Value | Data Type | Description |
|------------|-------|-----------|-------------|
| Email | test.user@example.com | String | Valid email format, registered user |
| Password | SecurePass123! | String | Must be 12+ chars, alphanumeric + special |
| First Name | John | String | Alphanumeric, max 50 chars |
| Last Name | Doe | String | Alphanumeric, max 50 chars |
| Phone Number | +1-555-0123 | String | E.164 format |
| Date of Birth | 1990-05-15 | Date | User must be 18+ years old |
| Country | United States | String | Must be valid country code |
| Quantity | 5 | Integer | Must be between 1 and 100 |
| Discount Code | SAVE2024 | String | Valid promotional code |
| Invalid Email | test@invalid | String | Missing domain to test validation |
| Null/Empty | (blank) | N/A | To test required field handling |
| Boundary Value | 999999999 | Integer | Maximum allowed value test |

---

## Test Steps

| Step # | Action | Expected Result | Actual Result | Pass/Fail | Notes |
|--------|--------|-----------------|---------------|-----------|-------|
| 1 | Navigate to login page at https://app.example.com/login | Login page loads successfully within 3 seconds, all elements visible and correctly positioned | | | Wait time not to exceed 3s |
| 2 | Enter email 'test.user@example.com' in email field | Email input field accepts text, no validation errors display | | | Test with valid email format |
| 3 | Enter password 'SecurePass123!' in password field | Password field masks characters with dots/asterisks, no text visible | | | Verify field security |
| 4 | Click 'Sign In' button | Page redirects to dashboard within 2 seconds, user is authenticated | | | Check session token creation |
| 5 | Verify dashboard header displays 'Welcome, John Doe' | Header text matches user's full name from profile | | | Text should be dynamic |
| 6 | Verify 'Logout' button is present and clickable | Logout button is visible in top-right corner, cursor changes to pointer on hover | | | Accessibility: keyboard accessible |
| 7 | Click 'Logout' button | User is redirected to login page, session is invalidated | | | Session token should be cleared |
| 8 | Attempt to navigate directly to dashboard URL via browser back button | User is redirected to login page (not dashboard) | | | Verify auth state protection |

---

## Postconditions

**Cleanup and Final State**

- User session should be completely terminated (session token removed from client and server)
- User should be logged out and unable to access protected pages without re-authentication
- Test user account should be returned to original state (reset any modified fields)
- Cleanup Steps:
  1. If test created new records, delete them from database: `DELETE FROM users WHERE email = 'test.user@example.com'`
  2. Clear browser cache and cookies (or use incognito mode for automation)
  3. Reset any feature flags used during test
  4. Restore any modified configuration files to production defaults
- Database State: Any test data should be rolled back
- No Side Effects: No test data should remain in system after execution

---

## Pass/Fail Criteria

**Clear Definition of Success and Failure**

### Pass Criteria (All must be true)
1. User successfully authenticates with valid credentials within the SLA (2 second response time)
2. Dashboard displays with all user-specific information correctly personalized
3. Session is properly established (token created, cookies set with HttpOnly flag)
4. Logout functionality completely terminates the session
5. Cannot re-access protected pages after logout without re-authentication
6. No JavaScript errors appear in browser console (check F12 developer tools)
7. No sensitive data (passwords, tokens) is visible in network traffic or logs

### Fail Criteria (Any single failure means test fails)
1. Login fails or takes longer than 5 seconds
2. Dashboard displays generic or another user's information
3. Session token is exposed in URL, local storage, or unencrypted cookies
4. After logout, back button allows access to previously authenticated pages
5. Any HTTP 5xx errors appear in response
6. Sensitive authentication data appears in browser console or network tab
7. Any accessibility warnings (WCAG 2.1 AA violations) in automated audit

---

## Automation Notes

### Automatable: Yes / No

**Reasoning:** This test is automatable because it involves deterministic UI interactions with predictable results. No random elements, no external manual verification needed.

### Automation Framework
- **Framework:** Playwright / Cypress / Selenium / JUnit / pytest / Other
- **Language:** Python / JavaScript / Java / Go
- **Test Script Location:** `/tests/e2e/auth/login.spec.js`
- **Supported Environments:** Chrome, Firefox, Safari (headless compatible)
- **Execution Time:** ~15 seconds per run
- **Flakiness Risk:** Low (no network dependencies, deterministic timing)
- **Maintenance Effort:** Low (UI elements are stable, unlikely to change)

### Automation Code Sample
```javascript
// Pseudo-code example (Playwright)
test('User login with valid credentials', async ({ page }) => {
  await page.goto('https://app.example.com/login');
  await page.fill('input[name="email"]', 'test.user@example.com');
  await page.fill('input[name="password"]', 'SecurePass123!');
  await page.click('button:has-text("Sign In")');
  await page.waitForNavigation();
  await expect(page.locator('text=Welcome, John Doe')).toBeVisible();
});
```

---

## Related Test Cases

| Related Test Case ID | Title | Relationship | Dependency |
|----------------------|-------|--------------|-----------|
| TC-USR-002 | Login with invalid email format | Negative case | None (independent) |
| TC-USR-003 | Login with incorrect password | Negative case | None (independent) |
| TC-USR-004 | Account lockout after 5 failed attempts | Related scenario | Depends on TC-USR-003 |
| TC-USR-005 | Password reset flow | Related feature | None (separate flow) |
| TC-USR-006 | Remember me functionality | Enhancement | None (independent) |
| TC-SEC-001 | XSS prevention in login form | Security variant | Should run with security test suite |

---

## Defects Found

| Defect ID | Title | Description | Severity | Status | Date Found |
|-----------|-------|-------------|----------|--------|-----------|
| DEF-001 | Password field visible in autocomplete | Password autocomplete suggestion shows masked password in plain text | P2-Medium | Open | 2024-03-10 |
| DEF-002 | Missing error message on login timeout | After 10 minutes of inactivity, login button shows no feedback | P1-High | In Progress | 2024-03-10 |
| DEF-003 | Logout not clearing session cookies | Session cookie persists after logout, can be replayed | P0-Critical | Escalated | 2024-03-10 |

---

## Additional Notes

### Known Limitations
- This test does not cover OAuth/SSO flows (separate test suite)
- This test assumes email verification is already completed
- Mobile responsiveness testing requires separate test cases

### Dependencies
- Test data service must be running to seed user account
- Auth service must be deployed (version matches production)
- Database connectivity required for session validation

### Test Execution Context
- Best run in staging environment to avoid affecting real users
- Runs as part of nightly regression suite (every 23:00 UTC)
- Critical path test — must pass before release to production
- Part of Login & Authentication test suite (18 total test cases)
