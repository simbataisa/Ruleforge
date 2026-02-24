# Acceptance Criteria: HTTP Callable Actions Enhancement v2

**Feature Owner**: Product Owner
**Target Task**: #3 / Callable Actions v2 - HTTP Enhancement
**Status**: Ready for Development
**Blocks**: Task #5 (HTTP UI Implementation)

---

## 1. Custom Headers on All HTTP Methods

### AC 1.1: Headers available for GET method
**Given** a callable action is configured with HTTP protocol (GET)
**When** the user opens the HTTP configuration panel
**Then** a headers section displays with ability to add custom headers (Authorization, X-Custom, etc.)
**And** headers can be removed individually

### AC 1.2: Headers available for POST method
**Given** a callable action is configured with HTTP protocol (POST)
**When** the user opens the HTTP configuration panel
**Then** a headers section displays identically to GET
**And** headers persist independently per HTTP method type

### AC 1.3: Headers available for PUT method
**Given** a callable action is configured with HTTP protocol (PUT)
**When** the user opens the HTTP configuration panel
**Then** a headers section displays identically to GET
**And** Content-Type and other standard headers can be overridden

### AC 1.4: Headers available for DELETE method
**Given** a callable action is configured with HTTP protocol (DELETE)
**When** the user opens the HTTP configuration panel
**Then** a headers section displays identically to GET
**And** headers can include authentication and custom tracking headers

### AC 1.5: Header values support ${factPath} substitution
**Given** a callable action has headers configured
**When** a header value contains `${factPath}` (e.g., `Authorization: Bearer ${token}`)
**Then** at execution time, the ${factPath} is resolved from WorkingMemory facts
**And** if the fact resolves to a string, it is substituted into the header value
**Example**: Headers `{ "X-User-ID": "${userId}", "Authorization": "Bearer ${sessionToken}" }` with facts `{ userId: "user123", sessionToken: "abc456" }` → sends headers `{ "X-User-ID": "user123", "Authorization": "Bearer abc456" }`

### AC 1.6: Edge case — null factPath in header value
**Given** a header value references `${factPath}` that does not exist in facts
**When** the callable action is executed
**Then** the header value remains as the literal string `${factPath}` (not substituted)
**Or** alternatively, the header is omitted entirely (backend to decide)
**Status**: Backend behavior already defined; frontend must handle both cases gracefully

### AC 1.7: Edge case — empty headers object
**Given** a callable action has an empty headers map
**When** the HTTP request is made
**Then** no custom headers are added (only default Content-Type if applicable)
**And** the request succeeds with standard HTTP defaults

---

## 2. Query Parameters for GET (and optionally other methods)

### AC 2.1: Query parameters on GET method
**Given** a callable action is configured with HTTP protocol (GET)
**When** the user opens the HTTP configuration panel
**Then** a query parameters section displays (key-value pairs)
**And** the user can add, edit, and remove query parameters
**Example**: Adding `{ status: "active", limit: "10" }` → URL appended with `?status=active&limit=10`

### AC 2.2: Query parameters with ${factPath} substitution
**Given** a callable action has query parameters configured
**When** a parameter value contains `${factPath}` (e.g., `customerId: "${customerId}"`)
**Then** at execution time, the ${factPath} is resolved from WorkingMemory facts
**And** the resolved value is URL-encoded and appended to the query string
**Example**: URL `https://api.example.com/orders` + params `{ customerId: "${customerId}", status: "shipped" }` with fact `customerId: "CUST-789"` → request to `https://api.example.com/orders?customerId=CUST-789&status=shipped`

### AC 2.3: Edge case — null factPath in query parameter
**Given** a query parameter value references `${factPath}` that does not exist in facts
**When** the callable action is executed
**Then** the parameter is either:
  - Omitted entirely from the query string (preferred), or
  - Included as the literal string `${factPath}` (backend to decide)
**Status**: Backend behavior already defined; frontend must handle both cases

### AC 2.4: Edge case — URL already contains query string
**Given** the base URL is `https://api.example.com/orders?existing=param`
**And** query parameters are configured as `{ status: "active" }`
**When** the callable action is executed
**Then** the final URL is `https://api.example.com/orders?existing=param&status=active` (ampersand separator)
**And** no double question marks appear

### AC 2.5: Edge case — empty query parameters
**Given** the query parameters map is empty
**When** the callable action is executed
**Then** no query string is appended to the URL
**And** the request is made to the base URL unchanged

### AC 2.6: Edge case — special characters in parameter values
**Given** a query parameter value contains special characters (spaces, &, =, %, #)
**When** the callable action is executed
**Then** the value is URL-encoded per RFC 3986
**Example**: `search: "hello world"` → `?search=hello%20world`
**Example**: `filter: "price>100&type=book"` → `?filter=price%3E100%26type%3Dbook`

### AC 2.7: Query parameters on POST/PUT/DELETE (optional)
**Given** a user configures query parameters on POST, PUT, or DELETE methods
**When** the callable action is executed
**Then** the query parameters are appended to the URL (same behavior as GET)
**And** the request body (if any) is independent of query parameters

---

## 3. Path Parameters (Visual Detection from URL)

### AC 3.1: Path parameters detected in URL field
**Given** the URL field contains `${...}` expressions (e.g., `https://api.example.com/users/${userId}/orders`)
**When** the user views the URL field
**Then** the frontend displays a visual indicator or tooltip showing detected path parameters
**Example**: Tooltip or highlight showing "Detected path parameters: userId"

### AC 3.2: No functional change from backend
**Given** path parameters are already substituted by the backend at execution time
**When** the frontend displays the URL
**Then** no additional configuration UI is needed for path parameters
**And** the substitution is mentioned in documentation/help text

### AC 3.3: Path parameter ${factPath} substitution (backend verified)
**Given** a URL is `https://api.example.com/users/${userId}/orders/${orderId}`
**And** facts contain `{ userId: "user123", orderId: "order456" }`
**When** the callable action is executed
**Then** the backend substitutes path parameters and makes a request to `https://api.example.com/users/user123/orders/order456`
**Status**: Backend already implements this; frontend just displays and validates syntax

---

## 4. Protocol Switch: Script → HTTP Clears Target Field

### AC 4.1: Switching from Script to HTTP clears target
**Given** a callable action is currently configured with Script protocol
**And** the target field contains JavaScript code (e.g., `return context.evaluate(rule);`)
**When** the user selects HTTP protocol from the dropdown
**Then** the target field is cleared (emptied)
**And** a new HTTP-specific configuration panel appears

### AC 4.2: Switching from HTTP to Script clears target
**Given** a callable action is currently configured with HTTP protocol
**And** the target field contains a URL
**When** the user selects Script protocol from the dropdown
**Then** the target field is cleared (emptied)
**And** a new Script-specific code editor appears

### AC 4.3: No stale data in target field
**Given** a user switches protocol from Script → HTTP → Script
**When** the user views the target field in Script mode
**Then** the field is empty (not containing the old HTTP URL)
**And** the user is prompted to enter new Script code

### AC 4.4: Switching protocol preserves other fields
**Given** a callable action has name, description, and other metadata
**When** the user switches protocol
**Then** name, description, and metadata fields remain unchanged
**And** only protocol-specific configuration (target, headers, query params, etc.) is cleared

---

## 5. Editable Header Keys

### AC 5.1: Header keys are editable
**Given** a callable action has headers configured
**When** the user clicks on a header key field
**Then** the key becomes editable (input field or in-line edit)
**And** the user can change the header key (e.g., "X-Custom-Header" → "X-New-Header")

### AC 5.2: Header value remains intact during key rename
**Given** a header exists as `{ "Authorization": "Bearer token123" }`
**When** the user renames the key to "X-Auth-Token"
**Then** the header becomes `{ "X-Auth-Token": "Bearer token123" }`
**And** the value is not modified

### AC 5.3: Duplicate header keys prevented
**Given** a callable action already has a header with key "X-Custom"
**When** the user attempts to rename another header key to "X-Custom"
**Then** a validation error is displayed (e.g., "Header key already exists")
**And** the rename is prevented
**Note**: HTTP allows duplicate headers; decision to enforce uniqueness is product-level

### AC 5.4: Header keys are case-sensitive
**Given** a header key is "Content-Type"
**When** the user renames it to "content-type"
**Then** the system treats it as a different header key
**And** both headers are stored if both are added (or a warning is shown per product decision)

### AC 5.5: Special characters in header keys
**Given** a user attempts to enter a header key with invalid characters (spaces, =, :)
**When** the user tries to save
**Then** a validation error is displayed (header keys must be RFC 7230 compliant)
**And** valid characters are alphanumeric, hyphen, underscore

### AC 5.6: Empty header key validation
**Given** a user attempts to clear the header key field
**When** the user tries to save
**Then** a validation error is displayed ("Header key cannot be empty")
**And** the header is not saved until a valid key is provided

---

## Edge Cases Summary

| Edge Case | Requirement |
|-----------|-------------|
| null ${factPath} in header value | Omit or keep literal (backend decides; frontend handles both) |
| null ${factPath} in query param | Omit or keep literal (backend decides; frontend handles both) |
| URL already has ? | Append with & separator, no double ?? |
| Empty headers object | No custom headers added, request proceeds |
| Empty query params object | No query string appended, request proceeds |
| Special characters in query values | URL-encode per RFC 3986 |
| Duplicate header keys | Validation warning (product decision: allow or prevent) |
| Invalid header key characters | Validation error, reject non-RFC 7230 chars |
| Empty header key | Validation error |
| Protocol switch | Clear target field only, preserve metadata |
| Path params in URL | Visual indicator (no separate config needed) |

---

## Acceptance Checklist

- [ ] Custom headers available on GET, POST, PUT, DELETE
- [ ] Header values support ${factPath} substitution
- [ ] Query parameters available on GET (and optionally POST/PUT/DELETE)
- [ ] Query parameter values support ${factPath} substitution
- [ ] Path parameters detected and displayed in URL field
- [ ] Protocol switch (Script ↔ HTTP) clears target field
- [ ] Header keys are editable inline
- [ ] Duplicate header keys handled (validation)
- [ ] Invalid header key characters rejected (validation)
- [ ] Empty query params don't break URL
- [ ] URL with existing ? appends with & correctly
- [ ] Special characters in query values are URL-encoded
- [ ] Null ${factPath} references handled gracefully
- [ ] Edge cases documented and tested

---

**Next Steps**:
- [ ] Task #5 (HTTP UI Implementation) — Implement Accept Criteria
- [ ] Backend verification of query param encoding and null handling
- [ ] Integration test for all HTTP methods with substitution
