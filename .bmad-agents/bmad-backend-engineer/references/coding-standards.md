# Backend Coding Standards & Best Practices

## Core Design Principles

### SOLID Principles
- **Single Responsibility Principle (SRP):** Each class/module should have one reason to change. A service handles one business capability; a handler has one HTTP concern.
- **Open/Closed Principle (O/CP):** Open for extension, closed for modification. Use dependency injection, interfaces, and abstractions to allow behavior changes without modifying existing code.
- **Liskov Substitution Principle (LSP):** Subtypes must be substitutable for their base types. Derived classes should not break the contract of the base class.
- **Interface Segregation Principle (ISP):** Clients should not depend on interfaces they don't use. Create small, focused interfaces rather than large god-interfaces.
- **Dependency Inversion Principle (DIP):** Depend on abstractions, not concretions. Inject interfaces/abstractions, not concrete implementations.

### DRY (Don't Repeat Yourself)
- Extract reusable logic into shared functions/methods
- Create utility libraries for common operations
- Use code generation where appropriate (OpenAPI server stubs, protobuf)
- Establish shared domain models across services

### KISS (Keep It Simple, Stupid)
- Favor explicit, clear code over clever optimization prematurely
- Minimize cyclomatic complexity (target: < 10 per method)
- Use standard libraries and patterns before reaching for custom solutions
- Document complex logic with comments explaining the "why"

### YAGNI (You Aren't Gonna Need It)
- Don't add features or abstractions until they're actually needed
- Build for current requirements, refactor when new requirements emerge
- Avoid premature optimization; measure first, then optimize hot paths

---

## Project Structure Conventions

### Package Organization: Package-by-Feature (Preferred)

Organize code by business domain/feature rather than technical layer. This improves cohesion and makes it easier to understand a feature end-to-end.

**Example Structure (Java/Kotlin):**
```
src/main/java/com/bmad/service/
├── resources/
│   ├── domain/
│   │   ├── Resource.java              # Entity/model
│   │   ├── ResourceRepository.java    # Repository interface
│   │   └── ResourceStatus.java        # Enums/value objects
│   ├── application/
│   │   ├── CreateResourceCommand.java
│   │   ├── ResourceService.java       # Use cases
│   │   └── ResourceEventPublisher.java
│   ├── infrastructure/
│   │   ├── PostgresResourceRepository.java
│   │   └── ResourceMapper.java
│   └── api/
│       └── ResourceController.java
├── notifications/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── api/
├── shared/
│   ├── domain/
│   │   └── DomainEvent.java
│   ├── infrastructure/
│   │   ├── KafkaEventPublisher.java
│   │   └── PostgresUnitOfWork.java
│   └── api/
│       └── GlobalExceptionHandler.java
└── config/
    ├── AppConfiguration.java
    └── KafkaConfiguration.java
```

**Layer Responsibilities:**
- `domain/` - Business entities, repository interfaces, domain events, domain services
- `application/` - Use cases, application services, DTOs, mappers
- `infrastructure/` - Database/ORM implementations, external service clients, infrastructure adapters
- `api/` - REST controllers, request/response models, error handlers

### Alternative: Package-by-Layer (Legacy/Acceptable)

If organizational convention requires it:
```
src/main/java/com/bmad/service/
├── domain/
│   ├── Resource.java
│   ├── ResourceRepository.java
│   └── ResourceStatus.java
├── service/
│   ├── ResourceService.java
│   └── NotificationService.java
├── repository/
│   ├── ResourceRepository.java (impl)
│   └── UserRepository.java (impl)
├── controller/
│   ├── ResourceController.java
│   └── HealthController.java
├── dto/
│   ├── CreateResourceRequest.java
│   └── ResourceResponse.java
└── config/
```

---

## Naming Conventions

### Package Names
- Lowercase, no underscores: `com.bmad.service.resources`
- Hierarchical: `com.bmad.{company}.{domain}.{feature}.{layer}`
- Avoid single-letter packages

### Class & Interface Names
- **PascalCase:** `ResourceService`, `CreateResourceCommand`, `PostgresUserRepository`
- **Interfaces:** Use descriptive nouns, NOT `IUserRepository` or `UserRepositoryInterface`
  - Good: `UserRepository`, `NotificationSender`, `EventPublisher`
  - Bad: `IUserRepository`, `UserRepositoryImpl`, `UserRepositoryInterface`
- **Implementations:** Specific descriptors
  - Good: `PostgresUserRepository`, `KafkaEventPublisher`, `EmailNotificationSender`
  - Bad: `UserRepositoryImpl`, `EventPublisherImpl`
- **Exceptions:** End with `Exception`
  - Good: `UserNotFoundException`, `InvalidResourceException`
  - Bad: `UserNotFound`, `InvalidResourceError`

### Method & Function Names
- **PascalCase for commands (Java/Kotlin):** `getString()`, `findUserById()`, `publishEvent()`
- **camelCase for commands (Go):** `getString()`, `findUserById()`, `publishEvent()`
- **Imperative verb + noun:** `createUser()`, `deleteResource()`, `sendNotification()`
- **Queries (return boolean):** `isActive()`, `hasPermission()`, `canDelete()`
- **Avoid Hungarian notation:** Don't prefix with types like `strName`, `intCount`

### Variable & Constant Names
- **Local variables:** camelCase: `userName`, `maxRetries`, `requestId`
- **Constants:** UPPER_SNAKE_CASE: `MAX_RETRIES = 3`, `DEFAULT_TIMEOUT_MS = 5000`
- **Boolean variables:** Prefix with is/has/should: `isActive`, `hasPermission`, `shouldRetry`
- **Meaningful names:**
  - Good: `authenticatedUser`, `remainingRetries`, `responseTimeMs`
  - Bad: `u`, `r`, `t`, `temp`, `data`

### Database Naming
- **Table names:** snake_case, plural: `users`, `resources`, `audit_events`
- **Column names:** snake_case: `user_id`, `created_at`, `is_active`
- **Primary key:** `id` (bigint/uuid depending on pattern)
- **Foreign keys:** `{related_table_singular}_id`: `user_id`, `resource_id`
- **Timestamps:** `created_at`, `updated_at`, `deleted_at` (use UTC timezone)
- **Boolean columns:** Prefix with `is_`: `is_active`, `is_deleted`
- **Index naming:** `idx_{table}_{columns}`: `idx_users_email`, `idx_resources_status_created_at`
- **Constraint naming:** `fk_{table}_{column}`: `fk_resources_user_id`

---

## API Design Standards

### REST Resource Naming
- **Plural nouns for collections:** `/users`, `/resources`, `/invoices`
- **Hierarchical relationships:** `/users/{userId}/resources`, `/tenants/{tenantId}/users/{userId}`
- **Actions as subresources when needed:** `/resources/{id}/actions/process`, NOT `/processResource`
- **Avoid:** `/getUsers`, `/createResource`, `/deleteInvoice`

### API Versioning
- **Header versioning (preferred):** `Accept: application/vnd.bmad.v1+json`
- **URL path versioning (acceptable):** `/api/v1/resources`, `/api/v2/resources`
- **Never use:** Query parameter versioning (`?version=1`)
- **Deprecation window:** 6 months minimum notice before removing v1 endpoints
- **Major version changes:** Breaking changes only (removed fields, changed response structure)
- **Minor version additions:** Additive changes only (new optional fields, new endpoints)

### HTTP Method Usage

| Method | Semantics | Idempotent | Cacheable | Request Body | Use Case |
|---|---|---|---|---|---|
| `GET` | Retrieve resource(s) | Yes | Yes | No | Fetch data, filtering, pagination |
| `POST` | Create new resource or trigger action | No | No | Yes | Create new entities, async operations |
| `PUT` | Replace entire resource | Yes | No | Yes | Full resource update (all fields) |
| `PATCH` | Partial resource update | No | No | Yes | Partial updates (only changed fields) |
| `DELETE` | Remove resource | Yes | No | No | Mark as deleted or hard delete |
| `HEAD` | Retrieve headers only (no body) | Yes | Yes | No | Check resource existence, validate etag |

**Important Notes:**
- POST is NOT idempotent by default (use `X-Idempotency-Key` header for idempotent POST)
- Use PUT with `If-Match` header (etag) to prevent concurrent modification conflicts
- For partial updates, prefer PATCH to PUT; include only changed fields in body

### HTTP Status Code Guide

| Code | Reason | When to Use |
|---|---|---|
| **200** | OK | Successful GET, successful synchronous operation |
| **201** | Created | Resource successfully created via POST |
| **202** | Accepted | Async operation accepted (will process later) |
| **204** | No Content | Successful DELETE or empty response (no body) |
| **400** | Bad Request | Request validation failed (invalid params, malformed JSON) |
| **401** | Unauthorized | Authentication required/failed (missing or invalid token) |
| **403** | Forbidden | Authenticated but lacks permission (insufficient scopes) |
| **404** | Not Found | Resource does not exist (ID not found) |
| **409** | Conflict | Concurrent modification (etag mismatch), duplicate (idempotency key) |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Unexpected server-side error (include trace ID) |
| **503** | Service Unavailable | Dependency failure, maintenance mode |

**Status Code Decision Flowchart:**
```
Is the request syntactically valid?
├─ No → 400 Bad Request
└─ Yes
   Is the user authenticated?
   ├─ No → 401 Unauthorized
   └─ Yes
      Does the user have permission?
      ├─ No → 403 Forbidden
      └─ Yes
         Does the resource exist?
         ├─ No → 404 Not Found
         └─ Yes
            Can we process the request?
            ├─ No (conflict, rate limit, etc.) → 409 or 429
            └─ Yes → 200/201/202/204
```

### Pagination Pattern

**Query Parameters:**
```
GET /resources?limit=20&cursor=eyJpZCI6ICJyZXMtMTIzIiwgInRzIjogMTcwMzA2MDAwMH0=&sort=-created_at
```

**Response Format (Cursor-based):**
```json
{
  "data": [
    { "id": "res-123", "name": "Resource 1", "created_at": "2026-03-14T10:30:00Z" },
    { "id": "res-124", "name": "Resource 2", "created_at": "2026-03-14T10:25:00Z" }
  ],
  "pagination": {
    "limit": 20,
    "cursor": "eyJpZCI6ICJyZXMtMTIzIiwgInRzIjogMTcwMzA2MDAwMH0=",
    "next_cursor": "eyJpZCI6ICJyZXMtMTI1IiwgInRzIjogMTcwMzA1OTUwMH0=",
    "has_more": true,
    "total_count": 245
  }
}
```

**Advantages of cursor-based pagination:**
- Handles concurrent inserts/deletions gracefully (offset-based pagination is problematic)
- O(1) operation (doesn't require counting total items)
- Token is opaque to client (implementation can change)

### Filtering & Sorting Conventions
```
# Filtering: ?filter[field]=value or ?filter[field][operator]=value
GET /resources?filter[status]=active&filter[type]=premium&filter[created_at][gte]=2026-03-01

# Sorting: field name with optional - prefix for descending
GET /resources?sort=-created_at,name
```

### Error Response Format
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Resource with ID 'res-123' not found",
    "details": {
      "resource_id": "res-123",
      "resource_type": "Invoice"
    },
    "trace_id": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

---

## Error Handling Patterns

### Fail Fast Principle
- Validate inputs at the earliest point (API boundary)
- Return errors immediately rather than attempting recovery
- Never return partial/corrupted data
- Example: Validate all parameters before querying database

### Checked vs Unchecked Exceptions (Java/Kotlin)

**Unchecked (RuntimeException) - Preferred:**
- Programming errors that shouldn't be caught: NullPointerException, IllegalArgumentException
- Unexpected runtime failures: OutOfMemoryError, StackOverflowError
- Exceptions from dependency injection and reflection

**Checked Exceptions - Used Sparingly:**
- Expected, recoverable conditions: IOException, SQLException (wrap and rethrow as unchecked)
- Domain-specific errors: UserNotFoundException (define as unchecked)

**Pattern:**
```java
// Unchecked domain exceptions (extends RuntimeException)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userId) {
        super("User with ID " + userId + " not found");
    }
}

// Wrap checked exceptions as unchecked
try {
    database.save(entity);
} catch (SQLException e) {
    throw new DataAccessException("Failed to save entity", e);
}
```

### Error Codes
- **Format:** `SCREAMING_SNAKE_CASE`
- **Prefix with domain/area:** `PAYMENT_INSUFFICIENT_FUNDS`, `USER_NOT_FOUND`, `VALIDATION_INVALID_EMAIL`
- **Use codes for:** Programmatic error handling, i18n, analytics
- **Message:** Human-readable description (not used for logic)

### Logging Levels Guide

| Level | When to Use | Example |
|---|---|---|
| **DEBUG** | Detailed technical information for debugging | "SQL query executed: SELECT * FROM users WHERE id = ?", variable values |
| **INFO** | Significant business events | "User created successfully", "Payment processed", "Resource deleted" |
| **WARN** | Unexpected but recoverable situations | "Cache miss, fetching from DB", "Retry attempt 2/3", "Rate limit approaching" |
| **ERROR** | Error events that need attention | "Database connection failed", "External API returned 500", "Invalid request body" |
| **CRITICAL** | System in critical state, requires immediate action | "Database down", "All replicas unavailable", "Data corruption detected" |

**Rule:** Never log PII (passwords, credit cards, SSNs, tokens). Sanitize sensitive data.

---

## Database Access Patterns

### Repository Pattern
```java
// Interface defines contract
public interface UserRepository {
    User findById(String userId);
    List<User> findByStatus(UserStatus status);
    User save(User user);
    void delete(String userId);
}

// Implementation handles persistence
@Repository
public class PostgresUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public User findById(String userId) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            new UserRowMapper(),
            userId
        );
    }
}

// Service uses abstraction
@Service
public class UserService {
    private final UserRepository userRepository;

    public User getUser(String userId) {
        return userRepository.findById(userId); // Depends on interface
    }
}
```

### Query Optimization Rules

**Avoid N+1 Queries:**
```java
// Bad: N+1 query problem
List<User> users = userRepository.findAll();
for (User user : users) {
    List<Invoice> invoices = invoiceRepository.findByUserId(user.getId()); // N queries!
}

// Good: Single query with JOIN
List<User> users = userRepository.findAllWithInvoices(); // Fetch with LEFT JOIN
// or use explicit fetching strategy (JPA @EntityGraph, Hibernate.initialize)
```

**Index Usage:**
- Create indexes on frequently filtered/sorted columns: `status`, `created_at`
- Create composite indexes for multi-column queries: `idx_users_status_created_at`
- Analyze query plans: `EXPLAIN ANALYZE SELECT ...`
- Monitor slow query logs (> 100ms)

**Transaction Boundaries:**
```java
@Transactional
public void processPayment(String invoiceId) {
    Invoice invoice = invoiceRepository.findById(invoiceId); // Within transaction
    invoice.markAsPaid();
    invoiceRepository.save(invoice); // Automatic flush/commit on method exit
    // Database changes visible to other queries only after commit
}
```

- Keep transactions short (< 100ms ideally)
- Don't make external API calls within transactions
- Use READ_COMMITTED isolation level (default); use SERIALIZABLE only when necessary
- Prefer optimistic locking (`@Version` field) over pessimistic locking for updates

---

## Async & Event-Driven Patterns

### Producer/Consumer Contracts
```java
// Event definition (immutable)
public record UserCreatedEvent(
    String eventId,
    Instant timestamp,
    String userId,
    String email,
    String correlationId
) {}

// Producer publishes event
@Service
public class UserService {
    private final EventPublisher eventPublisher;

    public User createUser(CreateUserRequest request) {
        User user = new User(request.name(), request.email());
        userRepository.save(user);

        eventPublisher.publish(new UserCreatedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            user.getId(),
            user.getEmail(),
            MDC.get("correlationId")
        ));
        return user;
    }
}

// Consumer subscribes to event
@Service
public class NotificationListener {
    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleUserCreated(UserCreatedEvent event) {
        // Send welcome email
        notificationService.sendWelcomeEmail(event.email());
    }
}
```

### Idempotency
- Consumer must handle duplicate events (at-least-once delivery guarantee)
- Store processed event IDs in database
- Use event ID for deduplication

```java
@Service
public class NotificationListener {
    @KafkaListener(topics = "user-events")
    public void handleUserCreated(UserCreatedEvent event) {
        // Check if already processed
        if (eventProcessingRepository.exists(event.eventId())) {
            return; // Skip duplicate
        }

        // Process event
        notificationService.sendWelcomeEmail(event.email());

        // Mark as processed
        eventProcessingRepository.save(new ProcessedEvent(event.eventId()));
    }
}
```

### Dead Letter Queue (DLQ)
- Route unprocessable messages to DLQ after max retries (typically 3)
- Monitor DLQ for failures: alerts if > 10 messages in 5 minutes
- Manual replay procedure: dump DLQ, fix issue, re-publish events

**Kafka DLQ Setup:**
```java
@Bean
public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory(
        ConsumerFactory<Object, Object> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate)
    ));
    return factory;
}
```

### Retry Policies
```java
@Retryable(
    value = {TransientException.class},
    maxAttempts = 3,
    backoff = @Backoff(
        delay = 1000,
        multiplier = 2.0,
        maxDelay = 10000
    )
)
public void callExternalApi() throws TransientException {
    // Exponential backoff with jitter: 1s, 2s, 4s (max 10s)
}
```

---

## Security Standards

### Input Validation
- Validate all inputs at API boundary
- Use whitelist approach (allow known good, reject everything else)
- Define constraints: length, format, allowed values

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
    // @Valid triggers validation; ConstraintViolations caught by GlobalExceptionHandler
}

public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Length(min = 1, max = 255, message = "Name must be 1-255 characters")
    private String name;

    @NotBlank
    @Email(message = "Must be valid email")
    private String email;
}
```

### SQL Injection Prevention
- **Always use parameterized queries** - Never concatenate user input into SQL strings
- Use ORMs (JPA, Hibernate) which parameterize queries by default
- Use PreparedStatement in raw JDBC

```java
// Bad: SQL injection vulnerability
String query = "SELECT * FROM users WHERE email = '" + email + "'";
// If email = "'; DROP TABLE users; --" database will be destroyed

// Good: Parameterized query
String query = "SELECT * FROM users WHERE email = ?";
preparedStatement.setString(1, email);
```

### Secrets Management
- **Never store secrets in code or .env files**
- Use a vault service (HashiCorp Vault, AWS Secrets Manager, Google Secret Manager)
- Inject secrets as environment variables at runtime
- Rotate secrets quarterly minimum
- Audit secret access

**Production Pattern:**
```java
@Configuration
public class VaultConfig {
    @Bean
    public DatabaseProperties databaseProperties(
            @Value("${spring.datasource.password}") String password) {
        // Password injected from Vault at startup
        return new DatabaseProperties(password);
    }
}
```

### Authentication & Authorization
- Implement OAuth 2.0 / OpenID Connect for multi-service scenarios
- Use bearer tokens (JWT with short expiration: 1 hour)
- Validate token signature and expiration on every request
- Implement scopes for fine-grained permission control

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token != null && jwtValidator.isValid(token)) {
            Claims claims = jwtValidator.verify(token);
            SecurityContextHolder.setContext(createSecurityContext(claims));
        }
        chain.doFilter(request, response);
    }
}
```

---

## Testing Standards

### Unit Test Structure: Arrange-Act-Assert
```java
@Test
void shouldCalculateDiscount_whenUserIsVIP() {
    // Arrange: Set up test data and mocks
    User vipUser = new User("user-123", UserType.VIP);
    Order order = new Order(100.0, vipUser);

    // Act: Execute the unit under test
    double discountedPrice = pricingService.calculatePrice(order);

    // Assert: Verify the outcome
    assertThat(discountedPrice).isEqualTo(90.0); // 10% VIP discount
}
```

### Naming Conventions
```java
// Pattern: shouldDo_when[Condition]
@Test
void shouldThrowException_whenUserIdIsNull() { }

@Test
void shouldReturnActiveUsers_whenFilteringByStatus() { }

@Test
void shouldRetryThreeTimes_whenExternalApiFailsTransiently() { }
```

### Mock vs Stub Guidance

**Use Stubs for:**
- External dependencies (databases, APIs, cache) in unit tests
- Predictable return values: `when(userRepository.findById("123")).thenReturn(user)`
- Dependencies that would slow down tests or require external setup

**Use Mocks for:**
- Verifying interactions: `verify(eventPublisher).publish(event)`
- Ensuring a dependency was called with correct arguments
- Capturing arguments for assertion: `ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class)`

```java
@Test
void shouldPublishEventWhenUserIsCreated() {
    // Stub the repository
    User newUser = new User("user-123", "john@example.com");
    when(userRepository.save(any(User.class))).thenReturn(newUser);

    // Mock the event publisher to verify interaction
    ArgumentCaptor<UserCreatedEvent> captor = ArgumentCaptor.forClass(UserCreatedEvent.class);

    // Act
    User result = userService.createUser(createUserRequest);

    // Assert
    verify(eventPublisher).publish(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo("user-123");
}
```

### Integration Test Rules
```java
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Test
    @Transactional
    void shouldSaveAndRetrieveUser() {
        User user = new User("john", "john@example.com");
        userRepository.save(user);

        User retrieved = userRepository.findById(user.getId());
        assertThat(retrieved).isEqualTo(user);
    }
}
```

- Use Testcontainers for database/infrastructure (starts real containers)
- Run against real database instance, not mocks
- Keep integration tests focused (test repository + database interaction, not business logic)
- Use transactions and rollback to keep tests isolated

### Coverage Thresholds
- **Unit test coverage:** 80% minimum (strictly enforced in CI)
- **Integration test coverage:** 60% minimum
- **Critical paths (payment, auth, deletion):** 95% coverage required
- **Uncovered lines should be documented:** `@Exclude` or comments explaining why

---

## Logging Standards

### Structured Logging (JSON Format)
```json
{
  "timestamp": "2026-03-14T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.bmad.service.ResourceService",
  "message": "Resource created successfully",
  "service": "resource-service",
  "environment": "production",
  "trace_id": "123e4567-e89b-12d3-a456-426614174000",
  "span_id": "9a3a3a3a3a3a3a3a",
  "user_id": "user-123",
  "resource_id": "res-550e8400-e29b-41d4-a716-446655440000",
  "duration_ms": 145,
  "status": "success"
}
```

**Required Fields:**
- `timestamp` - ISO 8601 with millisecond precision
- `level` - DEBUG, INFO, WARN, ERROR
- `logger` - Source class/module
- `message` - Human-readable (no PII)
- `service` - Service name
- `environment` - deployment environment
- `trace_id` - Distributed trace correlation
- `span_id` - Operation span

**Context Fields (as applicable):**
- `user_id` - Authenticated user
- `request_id` - HTTP request identifier
- `resource_id` - Primary resource being operated on
- `duration_ms` - Operation execution time
- `status` - success/failure

**Anti-patterns:**
- Don't log passwords, tokens, credit cards, SSNs
- Don't log raw request/response bodies (log meaningful extracts)
- Don't log at INFO level for every line executed

---

## Code Review Checklist

1. **Functionality:** Does the code implement the feature/fix as intended?
2. **Tests:** Are unit tests present? Integration tests where needed? All tests pass?
3. **Coverage:** New code has >= 80% coverage; critical paths >= 95%?
4. **Naming:** Classes, methods, variables follow conventions? Meaning is clear without comments?
5. **SOLID Principles:** Does code follow SRP, DIP? Are dependencies injected?
6. **Error Handling:** Are all exceptions caught? Meaningful error messages? No silent failures?
7. **Logging:** Appropriate log levels? No PII? Includes trace ID for distributed tracing?
8. **Security:** No SQL injection risks? Secrets managed correctly? Input validated?
9. **Database:** Queries optimized? Indexes present? N+1 problem avoided? Transaction scopes correct?
10. **API Design:** REST conventions followed? Status codes correct? Error responses standardized?
11. **Performance:** Any obvious inefficiencies? Algorithms reasonable? Database calls batched?
12. **Async/Events:** Idempotent? Dead letter queue handled? Retry logic present?
13. **Backward Compatibility:** Are breaking changes documented? Deprecation path clear?
14. **Documentation:** API contracts updated? Configuration documented? Code comments explain "why"?
15. **Dependencies:** New dependencies justified? Version pinned? Security vulnerabilities checked?
16. **Code Style:** Follows project conventions? Passes linter/formatter? No dead code?
17. **Database Migrations:** If schema changes, are migrations included and tested?
18. **Monitoring:** Are new metrics/alerts added if needed? Observability considered?
19. **Secrets:** No hard-coded credentials? Using vault? Environment variables?
20. **Concurrency:** Are race conditions considered? Is state thread-safe?
