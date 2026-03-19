# Bug Report Template

## Bug Metadata

| Field | Value |
|-------|-------|
| **Bug ID** | BUG-XXX (Auto-generated) |
| **Title** | Concise summary of the bug |
| **Reporter** | QA Engineer name |
| **Date Found** | YYYY-MM-DD HH:MM UTC |
| **Environment** | Development / Staging / Production |
| **Application Version** | X.Y.Z (exact version/commit hash) |
| **Platform/Browser** | Chrome 120 / Firefox 121 / Safari 17 / iOS Safari 17 / Android Chrome |
| **Browser Version** | Exact version number |
| **Operating System** | macOS 14.2 / Windows 11 / Ubuntu 22.04 / iOS 17 / Android 13 |
| **Screen Resolution** | 1920x1080 (or mobile: 390x844) |
| **URL/Feature Area** | /dashboard/payments or Payment Processing module |

---

## Severity Classification

| Severity Level | Definition | Impact | Example |
|---|---|---|---|
| **P0 - Critical** | Complete application failure or total feature loss. Data loss risk. Security vulnerability. User cannot perform essential workflow. | Application unusable, revenue impact, customer data at risk | Login completely broken for all users, payment processing fails for all transactions |
| **P1 - High** | Major feature degradation. Workaround exists but cumbersome. Affects significant user population or critical business function. | Feature partially broken, workaround required, affects 10%+ of users | Checkout button appears but doesn't respond, requires page refresh to proceed |
| **P2 - Medium** | Feature works but has issues. Workaround easy to implement. Affects small user segment or non-critical path. | Minor feature issue, easy workaround, affects <10% of users | Sort order in table incorrect, can click directly instead |
| **P3 - Low** | Cosmetic or edge case issue. No functional impact. User experience is slightly degraded. | Typo, color slightly off, animation timing, niche scenario | Spacing between buttons inconsistent, help text says "eMail" instead of "Email" |

**Severity Selected:** P0 / P1 / P2 / P3

---

## Priority Level

- **Must Fix Now** — Ship is blocked, critical path broken, security issue
- **This Sprint** — Should be fixed before next release, affects core feature
- **Next Sprint** — Good to fix but not blocking, nice-to-have improvement
- **Backlog** — Nice to have, cosmetic, low impact, can wait indefinitely

**Priority Selected:** [Choose one above]

---

## Component and Feature Affected

| Field | Value |
|-------|-------|
| **Product Area** | Authentication / Payments / Inventory / Reports / Admin Panel |
| **Feature** | User Login / Credit Card Processing / Product Search |
| **Sub-feature** | Session Management / PCI Compliance / Advanced Filters |
| **Service/API** | auth-service / payments-api / inventory-service |
| **Database** | users table / payments table |
| **Third-party Integration** | Stripe / Auth0 / SendGrid / AWS S3 |

---

## Summary

**One-sentence bug description:**

Examples:
- "Login button does not respond to clicks on mobile devices"
- "Search results show products from deleted inventory"
- "Payment confirmation email is sent even when transaction fails"
- "Sorting column by 'Date' in descending order shows oldest items first"

---

## Description

**Detailed explanation of the issue**

Provide context and background. What should happen vs. what is happening? What makes this bug unusual or noteworthy?

Example:
```
When a user attempts to log in using the mobile app on iOS 17, clicking the "Sign In" button
produces no visible feedback or response. The button does not show a loading state, the page
does not navigate, and no error message appears. However, checking the browser network tab
reveals that the login API request is sent successfully and returns a 200 status code with a
valid session token.

This suggests the issue is on the frontend UI layer, not the backend API. It appears to only
affect iOS devices, as the same flow works correctly on Android and desktop browsers.

The user must manually refresh the page to see the dashboard, indicating the session was
established server-side but the frontend did not process the response correctly.

This is blocking for mobile users and is a critical path feature.
```

---

## Steps to Reproduce

**Numbered, precise, reproducible steps**

Follow this format exactly. Each step should be unambiguous and executable by anyone.

1. Navigate to https://app.example.com/login on an iPhone running iOS 17
2. Wait for the login page to fully load (should see email and password fields)
3. Tap the email input field
4. Type "test.user@example.com" using the device keyboard
5. Tap the password input field
6. Type "SecurePass123!" using the device keyboard
7. Tap the "Sign In" button (blue button, located at bottom of form)
8. **Observe behavior:** Button shows no loading state. Page does not navigate. No error message displays. Wait 10 seconds with no action.
9. Open browser developer tools (Safari → Develop → iPhone → app.example.com)
10. Check Network tab and confirm the login API request was sent to POST /api/auth/login
11. Verify the response status is 200 and contains a session token in the response JSON
12. Close developer tools and manually refresh the page (pull down to refresh gesture on iOS)
13. **Observe behavior:** Dashboard now displays, indicating session was established but frontend did not process response

---

## Expected Behaviour

**What should happen**

The expected behavior is clear and testable:

When a user clicks the "Sign In" button with valid credentials:

1. Button immediately shows a loading state (spinner animation or disabled appearance)
2. An API request POST /api/auth/login is sent with email and password
3. API returns 200 status code with session token in response body
4. Frontend receives response and stores session token in secure HttpOnly cookie
5. Page automatically navigates to /dashboard within 2 seconds
6. Dashboard loads and displays user-specific content (name, profile picture, etc.)
7. User is authenticated and can access protected pages

**Success criteria:**
- Load time from click to dashboard visible: <3 seconds
- No console errors (F12 developer tools shows clean console)
- No 5xx or 4xx errors in network tab
- Session token is set in cookies as HttpOnly flag (cannot be accessed by JavaScript)

---

## Actual Behaviour

**What actually happened**

When user clicks "Sign In" button with valid credentials on iOS:

1. Button shows no loading state (no spinner, no visual feedback)
2. API request IS sent (visible in Network tab), returns 200 with valid token
3. Frontend does NOT navigate to dashboard
4. Page remains on login screen indefinitely
5. User cannot access dashboard without manual page refresh
6. Console shows no error messages (debugging is difficult)

**Workaround:** Refresh the page manually after clicking Sign In. This causes the frontend to reload, and it detects the existing session token, allowing the user to access the dashboard. The session was established correctly by the backend.

---

## Screenshots and Video Attachments

**Visual evidence of the bug**

Provide clear, annotated screenshots showing:
- The state before the bug occurs
- The state where the bug is visible
- Any relevant UI elements, error messages, or data

### Screenshot 1: Initial Login Screen
- **Description:** Login page on iPhone, email and password entered
- **File:** `bug-001-login-screen-initial.png`
- **Size:** 1125 x 2436 px (iPhone 15 Pro Max)
- **Annotations:** Red circle around "Sign In" button that doesn't respond

### Screenshot 2: After Clicking Sign In (No Response)
- **Description:** Same screen after 10 seconds of clicking button. Button shows no feedback.
- **File:** `bug-001-login-screen-no-response.png`
- **Size:** 1125 x 2436 px
- **Annotations:** Red X over "Sign In" button, question mark indicating expected navigation didn't happen

### Video 1: Reproduction Video
- **Description:** Screen recording showing full reproduction: entering credentials, clicking button, waiting, observing no response, then manual refresh showing dashboard loads
- **File:** `bug-001-reproduction.mp4`
- **Duration:** 1 min 15 sec
- **Device:** iPhone 15 Pro Max, iOS 17.3
- **Network:** WiFi, 50 Mbps connection

---

## Relevant Logs and Stack Trace

**Error messages, application logs, and debugging output**

### Browser Console Output
```
[No errors visible in Safari console - clean output]
Console is empty, no JavaScript errors thrown
```

### Network Tab Analysis
```
POST /api/auth/login - 200 OK - 245 ms
Request Headers:
  Content-Type: application/json
  Authorization: (none, pre-auth request)
Request Body:
  {"email":"test.user@example.com","password":"SecurePass123!"}

Response Headers:
  Content-Type: application/json
  Set-Cookie: sessionId=abc123def456; HttpOnly; Secure; SameSite=Strict
Response Body:
  {"status":"success","sessionId":"abc123def456","user":{"id":"usr-001","name":"Test User"}}
```

### Application Logs (from server)
```
[2024-03-10T14:32:15.234Z] INFO auth-service: Login attempt from IP 192.168.1.100
[2024-03-10T14:32:15.456Z] INFO auth-service: User test.user@example.com authenticated successfully
[2024-03-10T14:32:15.457Z] INFO auth-service: Session created with ID abc123def456
[2024-03-10T14:32:15.500Z] INFO auth-service: Response sent to client (200 OK)
```

### Device Logs (Safari - iOS)
```
[Opening Safari Console shows no errors]
No JavaScript exceptions in console
No network errors reported by browser
```

---

## Test Data Used

**What data was used to reproduce (anonymised)**

| Field | Test Value | Note |
|-------|-----------|------|
| Email | test.user@example.com | Registered test user, anonymised |
| Password | SecurePass123! | Test account password (test environment only) |
| User ID | user-test-001 | Internal ID for test account |
| Session ID | abc123def456... | Session created by backend (visible in Set-Cookie header) |

Note: All data is from test environment and is anonymised. No real user data included.

---

## Workaround

**Is there a way to work around this bug?**

**Yes, there is a temporary workaround:**

1. After clicking "Sign In", wait 5 seconds (even though page doesn't navigate)
2. Manually refresh the page using the Safari pull-down-to-refresh gesture
3. The page will reload and the session token will be detected from the HttpOnly cookie
4. Dashboard will load normally and the user can proceed

**Limitation of workaround:**
- User-unfriendly and not discoverable (why would they refresh?)
- Does not work if session token was not set in cookie (edge case)
- Workaround is specific to iOS (desktop and Android work normally)

---

## Root Cause Hypothesis

**Tester's best guess at the root cause (developer will confirm)**

Based on the symptoms, my hypothesis:

The backend is correctly handling the login request and setting the session token in an HttpOnly cookie. The frontend JavaScript is also sending the request successfully.

However, the frontend response handler (likely in the login component or the HTTP client interceptor) appears to have a platform-specific bug on iOS Safari:

1. **Hypothesis 1:** iOS Safari may be handling the Promise/async-await differently, causing the navigation logic to not execute on success
2. **Hypothesis 2:** The `window.location.href` redirect might be blocked by Safari's security model in certain contexts on iOS
3. **Hypothesis 3:** There may be a race condition where the cookie is being set but the JavaScript check for session existence is executing before the cookie is available
4. **Hypothesis 4:** iOS Safari's tracking prevention or privacy settings might be interfering with the session cookie storage

**Suggested investigation approach for developer:**
- Add console.log statements in the login response handler to confirm the success callback is being executed on iOS
- Check if the session cookie is actually being stored on iOS (may require checking Device Inspector in Xcode)
- Test with Xcode simulator to isolate whether this is a real iOS issue or specific to iOS 17.3
- Review recent changes to the login flow, especially any HTTP client or routing logic updates

---

## Impact Assessment

**Who is affected and what is the business impact?**

### Users Affected
- **Total Users:** ~2.3M monthly active users
- **Affected Segment:** iOS mobile app users (~650K users, ~28% of user base)
- **Severity to Users:** Cannot log in on iOS mobile app, must use desktop/web or Android app as workaround
- **Geographic Distribution:** Distributed globally, no specific region affected more than others

### Business Impact
- **Revenue Impact:** Medium-High. Mobile app is 35% of monthly revenue. Assuming 50% of mobile users attempt to log in each day and 20% are iOS users: ~65K users per day affected
- **Customer Impact:** Support team will receive login failure tickets from iOS users
- **Data Impact:** No data loss, but session management is broken for iOS users
- **Reputation Impact:** Negative reviews on App Store from iOS users ("app doesn't work")
- **Competitive Impact:** Users may switch to competitor if workaround is not obvious

### Time Sensitivity
- **Frequency:** Every user who tries to log in on iOS is affected
- **Recurrence:** Bug is reproducible 100% of the time on iOS
- **Duration:** Bug exists since at least 2 days ago (date found 2024-03-10, likely existed for multiple days)
- **Escalation:** This should be treated as a P0 Critical bug and fixed before next release

---

## Related Issues

| Issue ID | Title | Relationship | Status |
|----------|-------|--------------|--------|
| BUG-002 | Android login sometimes times out | Related error condition | Open |
| BUG-003 | Login button text disappears on small screens | Related UI bug | Resolved |
| FEATURE-045 | Redesign login flow with biometric auth | Future enhancement | In Backlog |
| INCIDENT-2024-001 | iOS app login failures reported by support | Root cause of incident | Ongoing |

---

## Resolution Notes (To be filled by Developer)

### Root Cause (Developer Analysis)
```
[To be completed by assigned developer]

Investigation found that the issue is in the AuthService response handler on iOS.
When the backend sets the Set-Cookie header, iOS Safari is not immediately reflecting
this change in the JavaScript-accessible cookie store due to timing of async operations.

The fix involved adding a small delay (50ms) before attempting to navigate, allowing
the cookie to be properly registered in Safari's cookie jar.

Root cause: Race condition between cookie being set by Set-Cookie header and JavaScript
attempting to read the cookie value to determine navigation.
```

### Fix Description
```
[To be completed by assigned developer]

Modified: /src/services/AuthService.ts (lines 145-152)
Changed the response handler to use Promise.all() with a small promise-based delay
to ensure cookie is registered before checking auth state.

Before:
  const response = await loginAPI(email, password);
  if (response.success) {
    navigate('/dashboard');  // Immediate navigation, cookie not ready on iOS
  }

After:
  const response = await loginAPI(email, password);
  if (response.success) {
    await new Promise(resolve => setTimeout(resolve, 50));  // Wait for cookie
    navigate('/dashboard');  // Now cookie is ready
  }

Also added iPhone user agent detection to apply this delay only on iOS Safari.
```

### Fix Commit
```
Commit: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
Author: developer@example.com
Date: 2024-03-11 09:45:23 UTC
Message: "Fix iOS Safari login navigation race condition"

Files modified:
- src/services/AuthService.ts
- src/hooks/useAuth.ts
- test/auth.spec.ts (added iOS regression test)

Test coverage: Added test case TC-USR-IOS-001 to regression suite
```

---

## Verification Notes (To be filled by QE after Fix)

### Retesting Information
- **Tested Date:** YYYY-MM-DD
- **Tested By:** QA Engineer name
- **Tested Environment:** Staging
- **Tested Devices:** iPhone 15 Pro Max (iOS 17.3), iPhone 12 (iOS 17.2)
- **Tested Browsers:** Safari (native)

### Retesting Results
```
Test Case: Login with valid credentials on iPhone 15 Pro Max (iOS 17.3)
Result: PASS - Button shows loading state, page navigates to dashboard within 2 seconds

Test Case: Login on iOS 17.2 (older version)
Result: PASS - Works correctly

Test Case: Login on Android devices (regression)
Result: PASS - No regression, still working as before

Test Case: Login on desktop (regression)
Result: PASS - No regression, still working as before

Edge case: Rapid double-click on Sign In button
Result: PASS - Only one session created, no duplicate requests

Edge case: Slow network (throttled to 2G)
Result: PASS - Loading state shows for extended period, eventually completes
```

### Verification Status: APPROVED
- Bug is fixed and verified on iOS
- No regressions detected on other platforms
- Meets all pass/fail criteria from original test case
- Ready for production release

---

## Additional Notes

### Known Limitations
- Fix is specific to iOS Safari; Android and desktop were not affected
- Solution adds a small 50ms delay to iOS logins (imperceptible to users)
- Considered alternative solutions but this was least invasive to existing codebase

### Future Prevention
- Add iOS Safari-specific tests to automated test suite (currently missing)
- Monitor for similar race conditions in other async flows
- Consider using a more robust session validation method that doesn't rely on timing

### QA Sign-off
- Test case updated: TC-USR-001 (added iOS variant as TC-USR-IOS-001)
- Regression test suite passed: All 147 tests green
- Ready for production deployment
