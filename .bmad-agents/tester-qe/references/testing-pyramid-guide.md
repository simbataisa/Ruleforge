# Testing Pyramid Guide - Comprehensive Reference

## The Testing Pyramid Overview

The testing pyramid is a framework for organizing tests by scope, speed, and cost. It defines the ideal distribution of test types across your test suite and explains the trade-offs between different testing layers.

```
          /\
         /  \
        /    \  E2E Tests (10%)
       /------\
      /        \
     /          \ Integration Tests (20%)
    /----------\
   /            \
  /              \ Unit Tests (70%)
 /________________\
```

### Key Principles

1. **Volume:** Write many unit tests (base), fewer integration tests (middle), very few E2E tests (top)
2. **Speed:** Unit tests execute in <1ms, integration in <5s, E2E in 10-30s per test
3. **Cost:** Unit tests are cheapest to write and maintain. E2E tests are most expensive.
4. **Reliability:** Unit tests are most reliable (deterministic). E2E tests are flaky (external dependencies).
5. **Feedback:** Unit tests give fastest feedback (instant). E2E tests take longest (5-30 min suite).
6. **Coverage:** Unit tests provide code coverage metrics. E2E tests provide feature/workflow coverage.

### Ideal Distribution

| Layer | Percentage | Quantity | Rationale |
|-------|-----------|----------|-----------|
| Unit Tests | 70% | ~700 tests | Fast feedback, cheap to write, catch logic errors early |
| Integration Tests | 20% | ~200 tests | Verify component interactions, database queries, API calls |
| E2E Tests | 10% | ~100 tests | Verify critical user journeys work end-to-end |
| **Total** | **100%** | **~1000 tests** | Balanced coverage, reasonable execution time (30-45 min total) |

**Why this ratio?**
- If you write too many E2E tests (e.g., 50% of suite), your test suite takes 2+ hours to run, slowing CI/CD pipeline
- If you write only unit tests (e.g., 95% of suite), you miss integration bugs and real-world issues
- The 70/20/10 ratio balances fast feedback, coverage, and maintainability

---

## Unit Tests

### Definition
Unit tests verify the behavior of isolated code units (functions, methods, classes) in isolation. They test a single unit of work with all dependencies mocked.

### What to Test with Unit Tests
- **Business logic:** Calculation engines, state machines, validation rules, data transformations
- **Error handling:** Exception handling, edge cases, boundary values, null safety
- **Algorithm correctness:** Sorting, filtering, searching, complex conditional logic
- **Utility functions:** String parsing, date formatting, encryption/hashing

Example:
```python
def test_calculate_discount_price():
    """Unit test for pricing logic"""
    original_price = 100.00
    discount_percent = 10
    expected = 90.00
    assert calculate_discount(original_price, discount_percent) == expected
```

### What NOT to Test with Unit Tests
- **External service calls:** Don't make real API calls. Mock them.
- **Database operations:** Don't hit real DB. Use in-memory database or mocks.
- **File I/O:** Don't write actual files. Mock the file system.
- **System calls:** Don't spawn real processes. Mock system interactions.

### Mocking Rules: Mock Boundaries, Not Internals

**Correct (Mock at boundary):**
```java
// Mock the external dependency (payment gateway)
@Mock
PaymentGateway paymentGateway;

@Test
void shouldProcessPaymentCorrectly() {
    // Arrange
    when(paymentGateway.charge(100, "card-123"))
        .thenReturn(new PaymentResult(true, "success"));

    // Act
    boolean result = paymentProcessor.processPayment(100, "card-123");

    // Assert
    assertTrue(result);
}
```

**Incorrect (Over-mocking internals):**
```java
// Don't mock internal helper methods
@Mock
PaymentProcessor processor; // Mocking the class we're testing!

@Test
void shouldProcessPaymentCorrectly() {
    // This defeats the purpose of unit testing internal logic
    when(processor.validateCard("card-123")).thenReturn(true);
    // Bad: We're not testing real validation logic
}
```

### Naming Convention: `should_doX_when_Y`

Clear, descriptive test names that express the behavior being tested:

```python
# Good
def test_should_return_discounted_price_when_customer_has_premium_membership():
def test_should_throw_validation_error_when_email_format_invalid():
def test_should_increment_counter_when_event_fired():
def test_should_handle_null_input_gracefully():

# Poor
def test_calculate():  # What does it test? Unclear.
def test_payment():  # Too vague.
def test_1():  # Meaningless.
```

### Coverage Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| **Line Coverage** | 80% | Catch obvious bugs, not over-optimize |
| **Branch Coverage** | 70% | Cover main code paths, not every edge case |
| **Method Coverage** | 90% | Most methods should be tested |
| **Exception Paths** | 100% | All error conditions must be tested |

**Why not 100% line coverage?**
- Diminishing returns: Last 20% of coverage is 80% of effort
- Some code is trivial (getters/setters, rarely tested)
- Focus on meaningful tests, not coverage chasing

### Execution Speed Target: <1ms per test

Unit tests should run almost instantly. Anything slower likely indicates external dependency (DB, API call, file I/O).

```javascript
// Good: ~0.5ms execution
test('should calculate tax correctly', () => {
  expect(calculateTax(100, 0.08)).toBe(8);
});

// Bad: ~100ms execution (file I/O)
test('should load config from file', () => {
  const config = require('./config.json');  // Blocking I/O!
  expect(config.apiUrl).toBeDefined();
});
```

### Frameworks by Stack

| Language | Primary Framework | Alternative | Notes |
|----------|------------------|-------------|-------|
| **Java** | JUnit 5 (Jupiter) | TestNG | Modern, support for parameterized tests, extensible |
| **Kotlin** | Kotest | JUnit 5 | Kotest provides more DSL flexibility |
| **Python** | pytest | unittest | pytest has better fixtures, cleaner syntax |
| **JavaScript/Node** | Jest | Vitest | Jest is standard in React/Node ecosystem |
| **Go** | testing (standard lib) | Testify | Go's built-in testing is lightweight, use Testify for assertions |
| **C#** | xUnit | NUnit | xUnit is modern, follows best practices |
| **Rust** | cargo test (built-in) | criterion (benchmarks) | Rust has testing built into language |

### Unit Test Example

```javascript
// Jest example: Authentication service unit test
describe('AuthService', () => {
  let authService;
  let mockHttpClient;
  let mockTokenStorage;

  beforeEach(() => {
    // Setup
    mockHttpClient = { post: jest.fn() };
    mockTokenStorage = { save: jest.fn(), get: jest.fn() };
    authService = new AuthService(mockHttpClient, mockTokenStorage);
  });

  describe('login', () => {
    test('should_return_user_object_when_credentials_valid', async () => {
      // Arrange
      const mockResponse = { token: 'abc123', user: { id: '1', name: 'John' } };
      mockHttpClient.post.mockResolvedValue(mockResponse);

      // Act
      const result = await authService.login('john@example.com', 'password123');

      // Assert
      expect(result).toEqual({ id: '1', name: 'John' });
      expect(mockTokenStorage.save).toHaveBeenCalledWith('abc123');
    });

    test('should_throw_error_when_credentials_invalid', async () => {
      // Arrange
      mockHttpClient.post.mockRejectedValue(new Error('Unauthorized'));

      // Act & Assert
      await expect(
        authService.login('john@example.com', 'wrongpassword')
      ).rejects.toThrow('Unauthorized');
    });
  });
});
```

---

## Integration Tests

### Definition
Integration tests verify that different components work together correctly. They test a unit plus its real (or containerized) dependencies: database, cache, message queues, external service adapters.

### Scope: Service Plus Real Dependencies

```
┌─────────────────────────────────┐
│    Integration Test Scope        │
├─────────────────────────────────┤
│  ✓ Service logic (UserService)  │
│  ✓ Real database (PostgreSQL)    │
│  ✓ Real cache (Redis)            │
│  ✓ Real message queue (Kafka)    │
│  ✗ External APIs (Stripe, Auth0) │ Still mocked
└─────────────────────────────────┘
```

### What to Test with Integration Tests

1. **Repository layer:** Database queries, transactions, data persistence
2. **Message consumers:** Event handlers, queue processing, delivery guarantees
3. **External API adapters:** API client logic, error handling, retry logic (with mocked endpoints)
4. **Cross-service interactions:** When service calls another service

Example:
```python
# Integration test: User repository with real database
@pytest.mark.integration
def test_should_save_and_retrieve_user_from_database(db_session):
    """Tests actual database operation"""
    # Arrange
    user = User(email='john@example.com', name='John Doe')

    # Act
    db_session.add(user)
    db_session.commit()
    retrieved_user = db_session.query(User).filter_by(email='john@example.com').first()

    # Assert
    assert retrieved_user.name == 'John Doe'
    assert retrieved_user.id is not None  # ID generated by DB
```

### What NOT to Test with Integration Tests

- **UI interactions:** Don't test UI in integration tests. Use E2E tests instead.
- **External cloud services:** Don't hit Stripe, AWS, or Auth0 in integration tests. Use test credentials or mocks.
- **All possible combinations:** You can't test every interaction. Focus on critical paths.

### Test Containers for Consistent Environments

Use TestContainers to spin up isolated containers (Docker) for each test:

```java
// Java example: Using Testcontainers for PostgreSQL
@Testcontainers
class UserRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @Test
    void should_save_user_to_database() {
        // Each test gets fresh container with clean DB
        User user = new User("john@example.com", "John");
        userRepository.save(user);

        User retrieved = userRepository.findByEmail("john@example.com");
        assertEquals("John", retrieved.getName());
    }
}
```

### Naming Convention

```python
# Integration test naming pattern
def test_should_save_user_to_database_when_all_fields_valid():
def test_should_query_users_by_email_and_return_matching_records():
def test_should_publish_user_created_event_to_kafka_after_save():
```

### Execution Speed Target: <5 seconds per test

Integration tests are slower because they touch real (containerized) dependencies. But they should still complete quickly.

```
Good:   ~2 seconds (database query + validation)
Bad:    ~30 seconds (external API call without timeout)
Ugly:   >60 seconds (performance/stress test in integration suite)
```

### Coverage Target: 60%

Integration tests should cover the critical integration points, not all code paths. Unit tests handle code paths; integration tests handle interactions.

### Frameworks and Tools

| Language | Framework | Container Tool |
|----------|-----------|-----------------|
| Java | JUnit 5 + Testcontainers | Docker (via Testcontainers) |
| Python | pytest + pytest-docker | Docker (via Docker SDK for Python) |
| Go | testing + testcontainers-go | Docker (via testcontainers-go) |
| Node.js | Jest + testcontainers | Docker (via testcontainers-node) |

### Integration Test Example

```python
# Python pytest example: Payment service integration test
import pytest
from testcontainers.postgres import PostgresContainer

@pytest.fixture(scope="module")
def postgres_container():
    with PostgresContainer("postgres:15") as container:
        yield container

@pytest.fixture
def payment_service(postgres_container):
    # Service uses real database container
    db_url = postgres_container.get_connection_url()
    return PaymentService(db_url=db_url)

@pytest.mark.integration
def test_should_process_payment_and_save_to_database(payment_service):
    # Arrange
    payment = Payment(amount=100.00, currency="USD", user_id="usr-123")

    # Act (This hits the real containerized database!)
    result = payment_service.process_payment(payment)
    saved_payment = payment_service.get_payment_by_id(result.payment_id)

    # Assert
    assert saved_payment.status == "completed"
    assert saved_payment.amount == 100.00
```

---

## Contract Tests (Consumer-Driven Contracts)

### Definition
Contract tests verify that services communicate correctly at their boundaries. They use Pact to define contracts between consumer and provider, catching integration bugs early without running full integration tests.

### When to Use
- **Microservices architecture:** When services owned by different teams communicate
- **Critical integration points:** When a bug would have high impact
- **CI pipeline:** Run contract tests before E2E tests to fail fast

### Example: Payment Service Contract

```javascript
// Consumer (Orders service) defines what it expects from Payment service
const { Pact } = require('@pact-foundation/pact');

describe('Orders service expects Payment service to', () => {
  const provider = new Pact({ consumer: 'OrdersService', provider: 'PaymentService' });

  it('return payment status when transaction is queried', async () => {
    // Define contract
    await provider.addInteraction({
      state: 'payment exists',
      uponReceiving: 'a request for payment status',
      withRequest: {
        method: 'GET',
        path: '/payments/pay-123',
      },
      willRespondWith: {
        status: 200,
        body: {
          id: 'pay-123',
          status: 'completed',
          amount: 100.00,
        },
      },
    });

    // Act: Orders service calls Payment service
    const payment = await paymentClient.getPayment('pay-123');

    // Assert
    expect(payment.status).toBe('completed');

    // Verify the contract
    await provider.verify();
  });
});
```

---

## End-to-End (E2E) Tests

### Definition
E2E tests verify that complete user journeys work correctly from the user's perspective. They exercise the entire application stack: frontend, backend, database, external integrations.

### What to Automate with E2E Tests

Focus E2E tests on **critical user journeys only** (not all possible flows):

1. **User registration and login:** User creates account, logs in, accesses dashboard
2. **Core business flow:** For ecommerce: browse → add to cart → checkout → payment → confirmation
3. **Critical integrations:** Payment processing end-to-end, notification delivery
4. **Role-based access:** Admin can manage users, regular users cannot

Example critical journeys for an ecommerce platform:
- Guest user buys a product (no account)
- Registered user updates profile
- Admin user creates a discount code
- User recovers forgotten password
- User cancels order and gets refund

**Total:** 5-10 critical journeys, not 50+

### What NOT to Test with E2E Tests

- **Every form field:** Use unit tests for input validation
- **All error conditions:** Use unit tests for error handling
- **UI edge cases:** Use visual regression tests
- **All navigation paths:** Use unit tests for routing logic
- **Performance:** Use load testing tools, not E2E tests

### Test Data Management

For E2E tests, you need realistic data that persists across test steps:

```javascript
// Pattern: Create data in beforeEach, clean up in afterEach
describe('Checkout flow', () => {
  let testUserId;
  let testProductId;

  beforeEach(async () => {
    // Set up test data in database (via API or direct DB)
    testUserId = await createTestUser({ email: 'test@example.com' });
    testProductId = await createTestProduct({ name: 'Test Product', price: 99.99 });
  });

  afterEach(async () => {
    // Clean up after test completes
    await deleteTestUser(testUserId);
    await deleteTestProduct(testProductId);
  });

  test('should_complete_checkout_successfully', async () => {
    // Test uses pre-created data
    await page.goto(`/products/${testProductId}`);
    await page.click('button:has-text("Add to Cart")');
    // ... rest of flow
  });
});
```

### Environment Requirements

E2E tests require:
- **Full application stack:** Frontend + Backend + Database
- **Test database:** Isolated from production (usually staging environment)
- **External service mocks:** For payment gateways, email services, etc. (use test credentials)
- **Stable URLs:** DNS and network must be consistent

### Flakiness Prevention Strategies

E2E tests are inherently flaky (external dependencies, timing issues, network latency). Minimize flakiness:

1. **Explicit waits, not sleeps:**
   ```javascript
   // Good: Wait for specific element
   await page.waitForSelector('button:has-text("Success")');

   // Bad: Hope 2 seconds is enough
   await page.waitForTimeout(2000);
   ```

2. **Idempotent test data:** Tests should work whether they're run once or 10 times
   ```javascript
   // Good: Idempotent (deletes before creating)
   await deleteUserIfExists('test@example.com');
   await createTestUser('test@example.com');

   // Bad: Non-idempotent (assumes user doesn't exist)
   await createTestUser('test@example.com');  // Fails if run twice
   ```

3. **Retry failed assertions:**
   ```javascript
   // Good: Playwright retries automatically
   await expect(page.locator('text=Success')).toBeVisible();

   // Bad: Single check, fails if timing is slightly off
   expect(await page.isVisible('text=Success')).toBe(true);
   ```

4. **Isolate tests:** Each test should be independent, not rely on previous test's state

5. **Mock slow/flaky services:** Mock payment gateway, email service. Don't depend on real services.

### Execution Time Budget

The entire E2E suite should complete in <10 minutes:

```
Total tests: 10 critical journeys
Average per test: 45 seconds (including waits and page loads)
Total with overhead: 10 min 30 seconds

If tests take >10 minutes:
  - Remove non-critical tests
  - Run tests in parallel
  - Optimize test setup (reduce database waits)
```

### Frameworks

| Framework | Best For | Language |
|-----------|----------|----------|
| **Playwright** | Modern web apps, cross-browser | JavaScript/Python/Java |
| **Cypress** | React/Vue apps, developer friendly | JavaScript |
| **Selenium** | Legacy browsers, cross-platform | Java/Python/JavaScript |
| **Appium** | Mobile apps (iOS/Android) | Java/Python/JavaScript |
| **Detox** | React Native apps | JavaScript |

### E2E Test Example

```javascript
// Playwright example: Complete user checkout flow
describe('Checkout user journey', () => {
  let page;
  let browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    // Create fresh test data for each test
    await seedTestDatabase();
  });

  afterEach(async () => {
    await page.close();
    await cleanupTestDatabase();
  });

  test('should_complete_checkout_with_credit_card', async () => {
    // 1. User navigates to home page
    await page.goto('https://app.example.com');
    await expect(page.locator('h1:has-text("Shop")')).toBeVisible();

    // 2. User searches for product
    await page.fill('input[placeholder="Search..."]', 'Laptop');
    await page.click('button:has-text("Search")');
    await page.waitForLoadState('networkidle');

    // 3. User adds product to cart
    const firstProduct = await page.locator('div[data-test="product-item"]').first();
    await firstProduct.click();
    await page.click('button:has-text("Add to Cart")');
    await expect(page.locator('text=Added to cart')).toBeVisible();

    // 4. User navigates to checkout
    await page.click('a:has-text("View Cart")');
    await page.click('button:has-text("Checkout")');

    // 5. User enters shipping info
    await page.fill('input[name="firstName"]', 'John');
    await page.fill('input[name="lastName"]', 'Doe');
    await page.fill('input[name="address"]', '123 Main St');
    await page.click('button:has-text("Continue to Payment")');

    // 6. User enters payment info
    const frameHandle = await page.$('[title="Stripe payment iframe"]');
    const frame = await frameHandle.contentFrame();
    await frame.fill('[data-testid="card-number-field"]', '4242424242424242');
    await frame.fill('[data-testid="expiry-date"]', '12/25');
    await frame.fill('[data-testid="cvc"]', '123');

    // 7. User completes purchase
    await page.click('button:has-text("Place Order")');
    await page.waitForNavigation();

    // 8. Verify success
    await expect(page.locator('text=Order Confirmed')).toBeVisible();
    await expect(page.locator('text=Order #')).toBeVisible();
  });
});
```

---

## Performance Tests

### Test Types and When to Run Each

| Type | Definition | When to Run | Duration | Goal |
|------|-----------|------------|----------|------|
| **Load Test** | Realistic user load over time | Before each release | 10-30 min | Verify SLO met under expected load |
| **Stress Test** | Increasing load until failure | Monthly/quarterly | 30-60 min | Find breaking point |
| **Soak Test** | Moderate load for extended time | Weekly | 2-8 hours | Find memory leaks, connection pool issues |
| **Spike Test** | Sudden traffic spike | After deployment | 5-10 min | Verify auto-scaling works |

### Performance Tools Comparison

| Tool | Best For | Language | Learning Curve |
|------|----------|----------|-----------------|
| **k6** | Cloud-native, modern | JavaScript/Go | Low, developer friendly |
| **Gatling** | Web applications | Scala/Java | Medium |
| **JMeter** | Legacy systems, protocols | Java/GUI | High |
| **Locust** | Python teams | Python | Low |
| **Artillery** | Node.js teams | YAML/JavaScript | Low |

### Baseline Metrics to Capture

```
Response Time:
  - Mean (average) latency
  - P50 (50th percentile / median)
  - P95 (95th percentile)
  - P99 (99th percentile) — "the worst 1% of users"
  - P99.9 (99.9th percentile)
  - Max (maximum observed)

Error Rate:
  - HTTP errors (4xx, 5xx)
  - Timeouts
  - Connection failures

Throughput:
  - Requests per second (RPS)
  - Transactions per second (TPS)

Resource Utilization:
  - CPU usage
  - Memory usage
  - Database connection pool
  - Network bandwidth
```

### Pass/Fail Criteria (SLO)

```yaml
Performance SLO:
  response_time_p99: 500ms      # 99% of requests < 500ms
  response_time_p99_9: 1000ms   # 99.9% of requests < 1 second
  error_rate: 0.1%              # Fewer than 0.1% errors
  throughput: 1000_rps          # Handle at least 1000 RPS

  Failure Criteria:
    - P99 latency > 500ms = FAIL
    - Error rate > 0.1% = FAIL
    - Throughput < 1000 RPS = FAIL
```

### Performance Test Example

```javascript
// k6 load test example
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users over 2 min
    { duration: '5m', target: 100 },   // Stay at 100 users for 5 min
    { duration: '2m', target: 200 },   // Ramp up to 200 users over 2 min
    { duration: '5m', target: 200 },   // Stay at 200 users for 5 min
    { duration: '2m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'],  // P99 latency must be < 500ms
    http_req_failed: ['rate<0.001'],   // Error rate < 0.1%
  },
};

export default function () {
  // Simulate a user browsing products
  let response = http.get('https://api.example.com/products');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // Simulate user viewing product detail
  response = http.get(`https://api.example.com/products/product-123`);
  check(response, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
```

---

## Security Tests

### SAST (Static Analysis) in CI Pipeline

Run code scanners on every commit to catch vulnerabilities before deployment:

```yaml
# GitHub Actions example
name: Security Scan
on: [push, pull_request]

jobs:
  sast:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run SonarQube scan
        uses: SonarSource/sonarqube-scan-action@master

      - name: Run Snyk dependency scan
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

      - name: Run Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
```

### DAST (Dynamic Analysis) Testing

Test the running application for vulnerabilities using OWASP ZAP:

```bash
# Run OWASP ZAP scan on staging environment
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://staging.example.com \
  -r report.html \
  -J report.json
```

### Dependency Scanning

Check for known vulnerabilities in dependencies:

```bash
# Using npm audit
npm audit

# Using Snyk
snyk test

# Using Dependabot (GitHub)
# Automatically checks dependencies, creates PRs for updates
```

### OWASP Top 10 Test Checklist

| Vulnerability | Test | How |
|---|---|---|
| **SQL Injection** | Inject SQL keywords into form fields | Test: `'); DROP TABLE users; --` in login field |
| **Authentication/Authorization** | Test role-based access | Admin can access admin panel, user cannot |
| **Sensitive Data Exposure** | Check for PII in logs/responses | Verify passwords not in error messages |
| **XML External Entities (XXE)** | Upload malicious XML file | Verify XML parser is not vulnerable |
| **Broken Access Control** | Test authorization at API level | User A cannot access User B's data |
| **Security Misconfiguration** | Check for default credentials | Try admin/admin, admin/password, etc. |
| **XSS (Cross-Site Scripting)** | Inject JavaScript into form fields | Test: `<script>alert('xss')</script>` |
| **CSRF (Cross-Site Request Forgery)** | Verify CSRF tokens present | Check form includes anti-CSRF token |
| **Using Known Vulnerable Components** | Scan dependencies | Run `npm audit`, `snyk test` |
| **Insufficient Logging & Monitoring** | Verify security events are logged | Check logs for failed login attempts |

---

## Accessibility Tests

### Automated Testing Tools

Tools that catch many accessibility issues automatically:

```javascript
// jest-axe example: Automated accessibility testing
import { render } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import LoginForm from './LoginForm';

expect.extend(toHaveNoViolations);

test('LoginForm should not have accessibility violations', async () => {
  const { container } = render(<LoginForm />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

Tools:
- **axe DevTools:** Browser extension for developers
- **jest-axe:** Automated testing in unit/integration tests
- **Lighthouse:** Built into Chrome DevTools, accessibility audit
- **Pa11y:** CLI tool for accessibility testing

### What Automated Tools Catch
- Color contrast violations (text on background)
- Missing alt text on images
- Missing form labels
- Improper heading hierarchy (h1 → h3 skips h2)
- Missing ARIA labels
- Missing keyboard focus indicators

### What Automated Tools Miss
- Actual screen reader behavior (e.g., Jaws, NVDA reads content in weird order)
- Keyboard navigation completeness (can you tab through all interactive elements?)
- Mobile touch target sizes
- Timing of animations (strobing/flashing content)
- Real user tasks (can blind user actually complete a purchase?)

### Manual Testing Requirements

1. **Screen reader testing:**
   ```
   Use NVDA (Windows free) or JAWS (Mac)
   Navigate form using only keyboard
   Verify form labels are read correctly
   Verify focus order makes sense
   ```

2. **Keyboard navigation:**
   ```
   Unplug mouse, use only keyboard
   Tab through all interactive elements
   Verify focus indicator is visible
   Verify logical tab order (left to right, top to bottom)
   Verify Escape closes modals
   Verify Enter submits forms
   ```

3. **Color contrast:**
   ```
   Use WebAIM contrast checker
   All text must have WCAG AA contrast (4.5:1 for normal text)
   All text must have WCAG AAA contrast (7:1) for critical features
   ```

### Accessibility Test Checklist

- [ ] All images have descriptive alt text
- [ ] Form inputs have associated labels
- [ ] Headings are in logical order (h1, h2, h3...)
- [ ] Color alone doesn't convey meaning (use icons, text labels too)
- [ ] Color contrast meets WCAG AA (4.5:1) minimum
- [ ] Interactive elements are keyboard accessible
- [ ] Focus indicator is visible
- [ ] Modal dialogs trap focus (can't tab out)
- [ ] Skip links present for keyboard users
- [ ] Touch targets are at least 44x44 pixels (mobile)
- [ ] No content hidden from screen readers without reason
- [ ] Animated content can be paused
- [ ] Videos have captions and transcripts

---

## Test Environment Strategy

### Environments and What Runs Where

| Environment | Purpose | Test Types | Data | Teardown | Notes |
|---|---|---|---|---|---|
| **Local** | Developer machine | Unit only | N/A | N/A | Fast feedback, instant |
| **Dev** | Shared dev server | Unit + Integration | Transient | After each test | Minimal infra cost |
| **Staging** | Pre-production replica | All types | Realistic copy | After nightly run | Closest to production |
| **Perf** | Performance testing | Performance tests | Realistic volume | Daily snapshot reset | Dedicated hardware |
| **Production** | Live application | Smoke tests only (read-only) | Real users | None | Never destructive |

### Test Environment Checklist

For each environment, verify:
- [ ] Appropriate isolation (dev doesn't affect production)
- [ ] Sufficient resource allocation (CPU, memory, disk, network)
- [ ] Clean state before each test run (databases reset)
- [ ] Test data seeding consistent
- [ ] External service mocks working correctly
- [ ] Monitoring and logging enabled
- [ ] Secrets and API keys properly configured
- [ ] Database backups before running tests
- [ ] Rollback procedure documented if needed

---

## Quality Gates

### Gate Definitions

Quality gates are automated checks that block code from progressing if standards aren't met.

| Gate | Metric | Threshold | Blocks | Purpose |
|------|--------|-----------|--------|---------|
| **Code Quality** | SonarQube rating | A or better | PR merge | Catch obvious bugs, complexity issues |
| **Unit Test Coverage** | Line coverage | ≥80% | PR merge | Ensure critical logic is tested |
| **Unit Test Pass Rate** | All tests pass | 100% | PR merge, Release | No broken code in main branch |
| **Security Scan** | Vulnerability severity | No P0/P1 | PR merge, Release | Catch known vulnerabilities |
| **Dependency Vulns** | Snyk score | A or better | PR merge | Prevent vulnerable libraries |
| **Integration Tests** | All P0/P1 tests pass | 100% | Release | Verify components work together |
| **E2E Tests** | All critical journeys pass | 100% | Release | Verify user workflows work |
| **Performance** | P99 latency | <500ms | Release | Verify SLO met |
| **Accessibility** | WCAG violations | 0 critical | Release | Ensure app is accessible |

---

## CI Pipeline Test Stage Design

### Pipeline Structure

```yaml
name: Test Pipeline
on: [push, pull_request]

jobs:
  fast-tests:          # Run immediately on PR
    runs-on: ubuntu-latest
    steps:
      - run: npm run test:unit
      - run: npm run test:integration
      - run: npm run lint
      - run: npm run type-check
    # Takes ~5 minutes, fails fast

  approval-stage:      # Manual approval before long tests
    needs: fast-tests
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Require approval for release tests
        run: echo "Ready for E2E and performance testing"

  slow-tests:          # Run after approval (parallel)
    needs: approval-stage
    strategy:
      matrix:
        browser: [chrome, firefox, safari]
    runs-on: ubuntu-latest
    steps:
      - run: npm run test:e2e -- --browser=${{ matrix.browser }}
    # Takes ~10 minutes total

  performance-tests:   # Long-running, separate job
    needs: approval-stage
    runs-on: ubuntu-latest-xl
    steps:
      - run: npm run test:performance
    # Takes ~30 minutes
```

### Parallel Execution Strategy

```
Sequential (SLOW):
  Unit Tests (5m) → Integration (10m) → E2E (15m) → Performance (30m)
  Total: 60 minutes

Parallel (FAST):
  Unit + Integration + Lint (10m parallel)
  ↓ (manual approval on main branch)
  E2E Chrome + Firefox + Safari (15m parallel)
  Performance (30m parallel)
  Total: ~45 minutes, much better feedback
```

### Fail-Fast Strategy

Stop the pipeline as early as possible when tests fail:

1. **Lint/type-check first:** Fast, catches obvious issues
2. **Unit tests next:** Fast, catch logic errors
3. **Integration tests:** Slower, catch integration issues
4. **E2E and performance:** Only run if all above pass

If lint fails, don't run unit tests. If unit tests fail, don't run integration tests. This minimizes time wasted.

---

## Summary: Testing Pyramid Best Practices

1. **Aim for 70/20/10 ratio:** Most tests at bottom (unit), few at top (E2E)
2. **Run fast tests on PR:** Unit and integration in <10 minutes
3. **Run slow tests before merge:** E2E and performance before production
4. **Mock at boundaries, not internals:** Isolate what you're testing
5. **One assertion per test when possible:** Clear pass/fail
6. **Descriptive test names:** `should_doX_when_Y` tells you what it tests
7. **Idempotent tests:** Run 10 times, get same result every time
8. **Clean up after tests:** No test data left behind
9. **Fail fast in CI:** Stop pipeline early when tests fail
10. **Monitor test flakiness:** Track and fix flaky tests immediately

The testing pyramid is a framework, not dogma. Adjust ratios based on your team's needs, but maintain the core principle: fast, isolated unit tests form the base.
