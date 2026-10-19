<!-- # Instagram Clone — Engineering Standards

> **Document Type:** Engineering / Coding Standards  
> **Applies To:** Entire Instagram Clone Backend  
> **Primary Architecture:** Modular Monolith  
> **Current Priority:** Backend Development  
> **Docker:** Not Used  
> **Frontend:** Deferred until backend completion

---

# 1. PURPOSE

This document defines the mandatory engineering standards for the Instagram Clone project.

The purpose is to ensure that the project remains:

- Clean
- Secure
- Maintainable
- Testable
- Scalable
- Consistent
- Production-oriented
- Easy for another developer to understand

These standards apply to all backend code, database changes, APIs, tests, configuration, infrastructure integrations and documentation.

The project must not sacrifice code quality simply to implement features faster.

---

# 2. CORE ENGINEERING PRINCIPLES

All implementation must follow:

- SOLID
- DRY
- KISS
- Separation of Concerns
- High Cohesion
- Low Coupling
- Composition over unnecessary inheritance
- Explicit business rules
- Secure by default
- Fail safely
- Testability first
- Maintainability over cleverness

Prefer:

```text
Simple + Explicit + Testable
```

over:

```text
Complex + Clever + Difficult to Maintain
```

---

# 3. ARCHITECTURE STANDARD

The application uses a:

> **Modular Monolith Architecture**

Do NOT introduce microservices.

Modules must have clear responsibilities.

Conceptually:

```text
                    Spring Boot Application
                             |
       ------------------------------------------------
       |        |        |        |        |          |
      Auth     User    Follow    Post    Feed     Notification
       |        |        |        |        |          |
       ------------------------------------------------
                             |
                    Shared Infrastructure
                             |
              --------------------------------
              |              |               |
            MySQL          Redis           Kafka
```

Modules must communicate through well-defined interfaces.

Avoid unnecessary cross-module access to internal implementation details.

---

# 4. MODULE BOUNDARIES

Each module should own its business logic.

Example:

```text
user
follow
post
media
engagement
feed
notification
story
reel
chat
search
settings
admin
```

A module should not directly manipulate another module's database tables unless explicitly justified by the architecture.

Prefer:

```text
Module A
   ↓
Module B Service/API
```

instead of:

```text
Module A
   ↓
Module B Repository
```

Cross-module dependencies must be intentional.

---

# 5. PACKAGE STRUCTURE

Use a consistent module-oriented structure.

Example:

```text
com.instagramclone
│
├── common
│   ├── config
│   ├── exception
│   ├── response
│   ├── validation
│   ├── security
│   └── util
│
├── auth
│   ├── controller
│   ├── dto
│   ├── service
│   └── security
│
├── user
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── mapper
│   ├── repository
│   └── service
│
├── follow
├── post
├── media
├── engagement
├── feed
├── notification
├── story
├── reel
├── chat
├── search
├── settings
└── admin
```

Follow the actual project architecture documentation when it specifies a more precise structure.

Do not create unnecessary package layers.

---

# 6. JAVA STANDARDS

## Java Version

Use:

```text
Java 21
```

Use modern Java features where they improve clarity.

Examples:

- Records for immutable DTOs where appropriate
- Pattern matching where useful
- Switch expressions
- Text blocks where useful
- `Optional` appropriately
- Modern collection APIs

Do not use modern language features merely for novelty.

---

# 7. NAMING CONVENTIONS

## Classes

Use PascalCase.

```java
UserService
PostController
JwtAuthenticationFilter
NotificationService
```

## Methods

Use camelCase.

```java
createUser()
findByUsername()
updateProfile()
validateToken()
```

## Variables

Use camelCase.

```java
currentUser
userId
postRepository
accessToken
```

## Constants

Use:

```java
MAX_FILE_SIZE
JWT_EXPIRATION
DEFAULT_PAGE_SIZE
```

## Packages

Use lowercase.

```text
com.instagramclone.user
com.instagramclone.auth
com.instagramclone.post
```

Avoid abbreviations unless they are universally understood.

---

# 8. CLASS RESPONSIBILITY

Every class should have one clear primary responsibility.

Bad:

```text
UserService
- authentication
- database operations
- email sending
- Kafka publishing
- file uploads
- validation
- notification creation
```

Prefer:

```text
UserService
AuthenticationService
EmailService
UserRepository
MediaService
EventPublisher
NotificationService
```

A class becoming too large is a signal to refactor.

---

# 9. METHOD RESPONSIBILITY

Methods should do one logical task.

Avoid methods like:

```text
processEverything()
handleUser()
doCompleteOperation()
```

Prefer explicit methods:

```text
validateRegistration()
createUser()
publishUserCreatedEvent()
sendWelcomeNotification()
```

Methods should generally remain small enough to understand without scrolling through hundreds of lines.

---

# 10. CONTROLLER STANDARD

Controllers are responsible for:

- HTTP requests
- Request validation
- Authentication context extraction
- Calling services
- Returning HTTP responses

Controllers must NOT contain complex business logic.

Bad:

```java
@PostMapping
public ResponseEntity<?> createPost(...) {

    // validation
    // business rules
    // database calls
    // Kafka
    // Redis
    // notification
}
```

Prefer:

```text
Controller
    ↓
Service
    ↓
Repository / Infrastructure
```

---

# 11. SERVICE STANDARD

Business rules belong in services.

Services should handle:

- Business validation
- Authorization decisions
- Transactions
- Domain operations
- Cross-module orchestration
- Event publication where appropriate

Services should not become giant procedural classes.

If a service becomes too large, identify separate responsibilities and refactor.

---

# 12. REPOSITORY STANDARD

Repositories are responsible for persistence operations.

Do not put business rules inside repositories.

Repositories should provide:

- Entity persistence
- Required queries
- Pagination
- Sorting
- Optimized database access

Avoid unnecessary custom queries.

Prefer derived queries when they remain readable.

Use explicit JPQL/native queries only when justified.

---

# 13. ENTITY STANDARD

Entities represent persistence state.

Do not expose JPA entities directly through APIs.

Avoid putting unrelated business logic inside entities.

Entities should contain:

- Persistent fields
- Relationships
- Persistence-specific configuration
- Required domain invariants where appropriate

Do not expose sensitive fields through response DTOs.

---

# 14. DTO STANDARD

Use DTOs for API communication.

Separate:

```text
Request DTO
Response DTO
Internal/domain model
Persistence entity
```

Example:

```text
CreateUserRequest
LoginRequest
UserResponse
ProfileResponse
UpdateProfileRequest
```

Never return the complete `User` entity directly.

Especially never expose:

- Password hash
- Internal security fields
- Internal audit information unless documented
- Sensitive infrastructure data

---

# 15. MAPPER STANDARD

Use explicit mapping between:

```text
Entity ↔ DTO
```

Use the project's documented mapper technology.

Mapping should remain predictable and easy to test.

Avoid hidden business logic inside mappers.

---

# 16. API DESIGN STANDARD

APIs must follow the documented API contract.

Every endpoint must clearly define:

- HTTP method
- URL
- Authentication
- Authorization
- Request
- Response
- Validation
- Error behavior

Use consistent REST conventions.

Example:

```text
GET    /api/v1/users/{userId}
PATCH  /api/v1/users/{userId}
POST   /api/v1/users/{userId}/follow
DELETE /api/v1/users/{userId}/follow
```

Do not create duplicate endpoints for the same operation.

---

# 17. API VERSIONING

Use the documented API versioning strategy.

Current convention:

```text
/api/v1/...
```

Do not randomly introduce:

```text
/api/user
/api/v2/user
/api/users-new
```

without an architectural reason.

Breaking API changes require explicit versioning decisions.

---

# 18. HTTP STATUS CODES

Use appropriate HTTP status codes.

Examples:

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests
500 Internal Server Error
```

Do not return `200 OK` for every situation.

---

# 19. RESPONSE FORMAT

Use a consistent response strategy defined by the project API documentation.

If the project uses a standard wrapper:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

follow that contract consistently.

Do not mix response formats between modules.

---

# 20. ERROR RESPONSE STANDARD

Errors must be predictable.

A production error response should contain appropriate information such as:

```text
timestamp
status
error/code
message
path
validation details where applicable
```

Do NOT expose:

- Stack traces
- SQL statements
- Internal class names
- Database credentials
- JWT secrets
- Infrastructure details

---

# 21. EXCEPTION HANDLING

Use centralized exception handling.

Prefer:

```text
@RestControllerAdvice
```

for REST APIs.

Create meaningful domain/application exceptions where required.

Examples:

```text
UserNotFoundException
DuplicateUsernameException
InvalidCredentialsException
UnauthorizedOperationException
ResourceNotFoundException
```

Do not use:

```java
catch (Exception e) {
    // ignore
}
```

Never silently swallow exceptions.

---

# 22. VALIDATION

Validate all external input.

Use Bean Validation where appropriate.

Example:

```java
@NotBlank
@Size(...)
private String username;
```

Validate:

- Request body
- Path variables
- Query parameters
- Pagination
- Sorting
- File metadata
- Search input

Frontend validation is never sufficient.

Backend validation is authoritative.

---

# 23. NULL HANDLING

Avoid unnecessary `null` propagation.

Use:

```text
Optional
validation
explicit exceptions
default values
```

where appropriate.

Do not use `Optional` as an entity field unless explicitly justified.

Do not return `null` from APIs when an explicit empty result is more appropriate.

---

# 24. TRANSACTION STANDARD

Use transactions intentionally.

Use:

```java
@Transactional
```

where multiple database operations must succeed or fail together.

Do not put `@Transactional` blindly on every method.

Avoid long-running transactions.

Do not hold database transactions while waiting for:

- External APIs
- Kafka
- File uploads
- WebSocket operations
- Slow network operations

unless explicitly required.

---

# 25. DATABASE STANDARD

MySQL is the primary persistent database.

All schema changes must use the project's migration system.

Never:

- Manually modify production schema
- Commit generated database dumps unnecessarily
- Ignore foreign keys
- Add duplicate indexes without reason

Every schema change must be reviewed for:

- Constraints
- Indexes
- Referential integrity
- Nullability
- Performance
- Migration rollback/recovery implications

---

# 26. DATABASE NAMING

Use consistent naming.

Tables:

```text
users
posts
follows
comments
likes
notifications
messages
```

Columns:

```text
user_id
created_at
updated_at
deleted_at
```

Follow the project's database documentation if it specifies exact names.

Never randomly mix:

```text
createdAt
created_at
creationDate
```

inside database schema.

---

# 27. INDEXING

Indexes must be based on actual query patterns.

Important relationships should be indexed appropriately.

Examples:

```text
followers
following
user_id
post_id
created_at
```

Composite indexes should match actual queries.

Do not create an index for every column.

Too many indexes can hurt write performance.

---

# 28. N+1 QUERY PREVENTION

Always review ORM relationships for N+1 problems.

Avoid blindly using:

```java
FetchType.EAGER
```

for relationships.

Prefer:

- Explicit fetch strategies
- Fetch joins
- Entity graphs
- Projections
- Batch fetching

where appropriate.

---

# 29. PAGINATION

Large collections must be paginated.

Examples:

- Followers
- Following
- Posts
- Comments
- Notifications
- Messages
- Search results

Never return an unlimited collection from a production API.

Use the documented pagination strategy.

---

# 30. SECURITY STANDARD

Security is a mandatory requirement.

Never:

- Store plaintext passwords
- Log passwords
- Hardcode secrets
- Commit API keys
- Expose JWT secrets
- Trust client-provided ownership
- Disable security controls without justification

Security must be enforced server-side.

---

# 31. PASSWORD SECURITY

Use the project's approved password encoder.

Passwords must be:

```text
Plain password
      ↓
Password Encoder
      ↓
Hash
      ↓
Database
```

Never decrypt passwords.

Never log passwords.

Never return password hashes through API responses.

---

# 32. JWT STANDARD

JWT secrets/keys must come from secure configuration.

Never:

```java
String secret = "my-secret";
```

Use environment/configuration management.

JWT handling must include:

- Signature validation
- Expiration validation
- Proper claims
- Authentication context
- Invalid token handling
- Expired token handling

Do not trust JWT claims blindly when sensitive authorization decisions require database verification.

---

# 33. AUTHORIZATION STANDARD

Authentication answers:

> Who are you?

Authorization answers:

> Are you allowed to do this?

Every protected resource must verify authorization.

Example:

```text
User A
   ↓
DELETE /posts/123
   ↓
Is post 123 owned by User A?
   ↓
YES → allow
NO  → deny
```

Never rely on the frontend to enforce ownership.

---

# 34. IDOR PREVENTION

Protect against insecure direct object references.

Never assume:

```text
/user/123
```

means the current user can access user 123.

Always evaluate:

- Ownership
- Visibility
- Relationship
- Role
- Privacy settings

---

# 35. CORS

CORS must be explicitly configured.

Do not use:

```text
allowOrigin = "*"
```

for authenticated production APIs unless there is a documented reason.

Allowed origins must come from configuration.

---

# 36. CSRF

CSRF strategy must match the authentication mechanism.

Do not blindly disable security features.

If JWT is used through headers in a stateless API, configure security according to the documented architecture.

If cookies are used for authentication, carefully evaluate CSRF protection.

---

# 37. RATE LIMITING

Rate limiting should be applied to sensitive/high-risk endpoints where required.

Examples:

- Login
- Registration
- Password operations
- OTP operations
- Search
- Messaging
- Media upload
- Public APIs

Redis may be used for distributed rate limiting where appropriate.

---

# 38. REDIS STANDARD

Redis is an optimization/infrastructure component.

Do not use Redis as the permanent source of truth for relational business data.

Use Redis for documented use cases such as:

- Cache
- Rate limiting
- Temporary state
- Presence
- Feed caching
- Counters
- Other documented requirements

Every cached value should have:

- Key convention
- TTL where appropriate
- Serialization strategy
- Invalidation strategy
- Failure behavior

---

# 39. REDIS KEY NAMING

Use predictable key patterns.

Example:

```text
instagram:user:{userId}
instagram:feed:{userId}
instagram:rate-limit:{userId}
instagram:presence:{userId}
```

Avoid random key names.

Keys should be easy to inspect and invalidate.

---

# 40. CACHE RULE

Never cache data without considering invalidation.

For example:

```text
Write
 ↓
Database
 ↓
Invalidate/update cache
```

Cache consistency must be deliberately designed.

Do not allow stale sensitive data to remain indefinitely.

---

# 41. KAFKA STANDARD

Kafka is used for asynchronous event processing.

Do not use Kafka as a replacement for normal synchronous service calls.

Use events where asynchronous processing provides value.

Examples:

```text
PostCreated
UserFollowed
PostLiked
CommentCreated
MessageSent
NotificationCreated
MediaUploaded
```

Only introduce events required by the architecture.

---

# 42. EVENT DESIGN

Events should be:

- Explicit
- Versionable
- Serializable
- Idempotently consumable
- Observable

Avoid sending complete database entities as events.

Prefer dedicated event DTOs.

Example:

```text
PostCreatedEvent

postId
authorId
createdAt
eventVersion
```

---

# 43. KAFKA CONSUMER STANDARD

Consumers must safely handle:

- Duplicate messages
- Processing failures
- Retries
- Invalid messages
- Serialization failures
- Consumer restarts

Consumers should be idempotent whenever possible.

Never assume:

```text
message delivered once
```

unless the architecture explicitly guarantees the required semantics.

---

# 44. WEBSOCKET STANDARD

WebSocket connections must be authenticated where required.

Every message must be validated.

Authorization must be enforced for:

- Conversation membership
- Message sending
- Subscription
- Private events

Never trust the client to identify another user's permissions.

---

# 45. FILE / MEDIA UPLOAD SECURITY

Media uploads must validate:

- File type (Image vs Video classification)
- MIME type (Whitelisted image/video MIME categories)
- Magic bytes / content inspection via `FileUtility.validateFile(file, true)`
- Size boundaries: Image ≤ 10MB, Video ≤ 50MB
- File extension matching genuine payload
- Ownership & authorization before modification or deletion

Never trust:

```text
filename
extension
Content-Type
```

from the client alone.

Do not allow executable uploads (.exe, .sh, .bat, .js, .html, .svg with scripts).

---

# 45.1 CLOUDINARY MEDIA ARCHITECTURE & STORAGE STANDARDS

Cloudinary is the mandatory cloud storage and delivery engine for all user media.

### Mandatory Rules:
1. **Never Store Binaries in MySQL:** MySQL stores only asset metadata in the `media` table (UUID, publicId, secureUrl, resourceType, mediaType, format, dimensions, duration, fileSize, folder, status).
2. **Never Expose API Secret:** `CLOUDINARY_API_SECRET` must reside strictly in backend environment variables. Never return secret in API responses, logs, or commit to version control.
3. **Always Store Public ID:** Every asset must retain its Cloudinary `publicId` in MySQL for deletion, replacement, and resource governance.
4. **Resilient Local Fallback:** When Cloudinary credentials are dummy/unconfigured (local dev or offline integration tests), the system must fallback gracefully to local storage without throwing 500 errors.
5. **Safe Replace Lifecycle:** When updating assets (e.g., avatar update), the new asset must be uploaded and persisted in MySQL *before* the previous asset is destroyed in Cloudinary.
6. **Rate Limiting:** Protect all upload endpoints with distributed Redis rate limiting (`@RateLimit(category = "media-upload", limit = 15, windowSeconds = 60)`).

---

# 46. LOGGING STANDARD

Logs should help diagnose problems without leaking sensitive information.

Good:

```text
User registration failed due to duplicate username
```

Bad:

```text
Password=123456
JWT=eyJ...
DB_PASSWORD=...
```

Never log:

- Passwords
- JWT tokens
- API keys
- Secrets
- Private credentials

---

# 47. LOG LEVELS

Use appropriate levels.

```text
TRACE
DEBUG
INFO
WARN
ERROR
```

Avoid excessive `ERROR` logging.

A normal validation failure is not necessarily an application error.

Do not log the same exception repeatedly at multiple layers.

---

# 48. CONFIGURATION STANDARD

Environment-specific configuration belongs in configuration files/environment variables.

Examples:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

JWT_SECRET
JWT_EXPIRATION

REDIS_HOST
REDIS_PORT

KAFKA_BOOTSTRAP_SERVERS
```

Never commit actual secrets.

Provide safe example configuration where useful.

---

# 49. ENVIRONMENT PROFILES

Use appropriate profiles.

Example:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Development configuration must not accidentally contain production credentials.

---

# 50. SECRET MANAGEMENT

Secrets must never be committed.

Before committing, inspect:

```text
git status
git diff
```

Ensure no:

- `.env`
- API keys
- Private keys
- Passwords
- JWT secrets
- Cloud credentials

are included.

---

# 51. TESTING STANDARD

Testing is mandatory.

Every meaningful business module must have tests.

Testing pyramid:

```text
             E2E
              ▲
         Integration
              ▲
            Unit
```

Unit tests should be fast.

Integration tests should verify real interactions.

---

# 52. UNIT TEST STANDARD

Test:

- Business rules
- Validation
- Services
- Security logic
- Edge cases
- Failure conditions

Do not test only getters/setters.

Bad test:

```text
assert user.getUsername() == username
```

Good test:

```text
duplicate username registration is rejected
```

---

# 53. INTEGRATION TEST STANDARD

Integration tests should verify:

- HTTP API
- Database
- Security
- Authentication
- Authorization
- Important infrastructure integrations

Do not mock every dependency.

Use real infrastructure/test infrastructure when required to validate integration behavior.

---

# 54. TEST NAMING

Test names should explain behavior.

Prefer:

```text
shouldRejectRegistrationWhenUsernameAlreadyExists()
shouldAllowUserToDeleteOwnPost()
shouldRejectPostDeletionForAnotherUser()
shouldRejectExpiredJwt()
```

Avoid:

```text
test1()
testUser()
testMethod()
```

---

# 55. EDGE CASES

Every feature must consider:

- Null input
- Empty input
- Duplicate input
- Invalid input
- Unauthorized access
- Forbidden access
- Missing resources
- Deleted resources
- Concurrent requests
- Large input
- Pagination boundaries
- Expired data

---

# 56. CONCURRENCY

For operations vulnerable to race conditions, use appropriate:

- Database constraints
- Transactions
- Optimistic locking
- Atomic Redis operations
- Idempotency
- Unique constraints

Do not rely solely on:

```text
if (!exists) {
    create();
}
```

for uniqueness.

Database constraints must protect critical invariants.

---

# 57. ENTITY RELATIONSHIPS

Use JPA relationships carefully.

Avoid unnecessary bidirectional relationships.

Avoid infinite JSON serialization.

Never expose entities directly to Jackson serialization.

Use DTOs.

Review:

- Cascade
- Orphan removal
- Fetch type
- Foreign keys
- Delete behavior

before adding relationships.

---

# 58. ENUM STANDARD

Use enums for controlled domain states.

Examples:

```text
AccountStatus
PostVisibility
NotificationType
MessageStatus
MediaStatus
```

Do not represent fixed states using arbitrary strings throughout the codebase.

---

# 59. DATE/TIME STANDARD

Use modern Java time types.

Prefer:

```java
Instant
LocalDate
LocalDateTime
ZonedDateTime
```

according to the semantic requirement.

For globally relevant timestamps, prefer UTC-based storage.

Do not manually manipulate timestamps using strings.

---

# 60. ID STANDARD

Use the identifier strategy defined by the database architecture.

Do not mix identifier strategies unnecessarily.

Every module must consistently follow the project's ID design.

---

# 61. UTILITY CLASS STANDARD

Do not create giant:

```text
Utils
Helper
CommonUtils
EverythingUtils
```

classes.

Utilities should have one clear purpose.

Examples:

```text
JwtUtils
DateTimeUtils
PaginationUtils
```

Only create a utility when reuse justifies it.

---

# 62. DEPENDENCY STANDARD

Do not add dependencies simply because they are popular.

Before adding a dependency ask:

1. Is it required?
2. Is the functionality already available?
3. Does it introduce security risk?
4. Does it increase maintenance cost?
5. Is it compatible with the project's Spring Boot/Java versions?

Avoid duplicate libraries performing the same job.

---

# 63. DEPENDENCY INJECTION

Prefer constructor injection.

Use:

```java
@RequiredArgsConstructor
```

or explicit constructors where appropriate.

Avoid field injection:

```java
@Autowired
private UserService userService;
```

Prefer constructor-based dependency injection.

---

# 64. LOMBOK

Use Lombok only where it improves readability.

Avoid generating confusing behavior.

Be careful with:

```text
@Data
@EqualsAndHashCode
@ToString
```

on JPA entities.

Do not automatically generate `equals()`/`hashCode()` based on mutable fields.

---

# 65. JPA ENTITY EQUALITY

Entity equality must be carefully designed.

Do not blindly use every field.

Avoid equality implementations that trigger lazy-loading or relationship traversal.

Follow the project's established entity identity strategy.

---

# 66. CODE COMMENTS

Write comments only when they explain:

- Why
- Important tradeoffs
- Non-obvious business rules
- Security reasoning
- Workarounds

Do not comment obvious code.

Bad:

```java
// increment counter
counter++;
```

Good:

```java
// Redis counter is incremented atomically to prevent lost updates
```

---

# 67. TODO STANDARD

Do not leave meaningless TODOs.

Bad:

```text
TODO: implement this later
```

Good:

```text
TODO(INST-123): Replace temporary fallback with documented media processor
```

Only leave TODOs that represent real future work.

---

# 68. DEAD CODE

Do not leave:

- Unused imports
- Unused methods
- Unused classes
- Commented-out code
- Duplicate implementations
- Debug prints

Remove dead code.

Git preserves history.

---

# 69. DEBUGGING

Never commit:

```java
System.out.println(...)
```

Use the project's logging framework.

Temporary debugging code must be removed before completion.

---

# 70. PERFORMANCE STANDARD

Do not optimize without evidence.

First ensure:

- Correctness
- Maintainability
- Proper indexing
- Proper pagination
- Correct caching
- Efficient queries

Then optimize measured bottlenecks.

Avoid premature optimization.

---

# 71. MEMORY / RESOURCE MANAGEMENT

Be careful with:

- Large file uploads
- Large query results
- Large JSON responses
- Streams
- Kafka consumers
- Redis data
- WebSocket connections
- Thread pools

Never load unnecessarily large datasets into memory.

---

# 72. THREADING

Do not create unmanaged threads manually unless explicitly required.

Prefer Spring-managed mechanisms for:

- Async work
- Scheduled jobs
- Kafka consumers
- Background processing

Thread pools must be configured intentionally.

---

# 73. BACKGROUND JOB STANDARD

Background jobs must be:

- Idempotent
- Observable
- Retry-safe
- Failure-aware
- Resource-conscious

A failed background job must not silently disappear.

---

# 74. API PAGINATION STANDARD

Paginated responses should consistently provide the information required by the API contract.

Potential information:

```text
content
page
size
totalElements
totalPages
hasNext
```

Follow the project's documented response format.

---

# 75. SEARCH STANDARD

Search APIs must protect against:

- Empty queries
- Extremely large queries
- Excessive result sizes
- Expensive wildcard queries
- Unauthorized data exposure

Apply reasonable limits.

---

# 76. PRIVACY STANDARD

Private user data must never be exposed accidentally.

Before returning user information, ask:

```text
Should this field be visible to this caller?
```

Sensitive information must be restricted.

---

# 77. ADMIN STANDARD

Admin APIs require explicit authorization.

Never use:

```text
if (user != null)
```

as an admin check.

Use roles/permissions according to the documented authorization model.

Every administrative action should be auditable where required.

---

# 78. AUDIT STANDARD

Security-sensitive operations should be auditable where required.

Examples:

- Account changes
- Administrative actions
- Moderation
- Security changes
- Privacy changes

Audit data must not contain unnecessary secrets.

---

# 79. GIT STANDARDS

Use meaningful commits.

Good:

```text
feat(auth): implement JWT authentication
feat(follow): implement follow relationship
fix(post): prevent unauthorized deletion
test(feed): add feed pagination tests
refactor(user): separate profile mapping
```

Avoid:

```text
update
changes
final
done
abc
```

---

# 80. BRANCHING

Use meaningful branch names.

Examples:

```text
feature/authentication
feature/follow-system
feature/post-module
feature/feed
fix/jwt-validation
fix/profile-authorization
```

Avoid:

```text
test
new
branch1
mybranch
```

---

# 81. COMMIT SAFETY

Before committing:

```text
git status
git diff
```

Review:

- Secrets
- Debug code
- Unintended files
- Generated artifacts
- Large files
- Configuration changes

---

# 82. BUILD STANDARD

Before declaring a module complete:

```text
mvn clean test
```

or the project's documented equivalent must succeed.

Frontend build is NOT required during backend-only development unless explicitly requested.

---

# 83. TEST STANDARD BEFORE COMPLETION

A module should not be marked complete until:

```text
Compilation       ✅
Unit Tests        ✅
Integration Tests ✅ where required
API Verification  ✅
Security Review   ✅
Database Review   ✅
Documentation     ✅
```

---

# 84. CODE REVIEW CHECKLIST

Before considering code complete, review:

### Architecture

- [ ] Correct module
- [ ] Correct dependency direction
- [ ] No unnecessary coupling

### Code

- [ ] Naming is consistent
- [ ] Methods have clear responsibilities
- [ ] No duplicate logic
- [ ] No dead code

### API

- [ ] Correct endpoint
- [ ] Correct HTTP status
- [ ] Validation
- [ ] Authorization
- [ ] DTOs

### Database

- [ ] Migration
- [ ] Constraints
- [ ] Indexes
- [ ] Query performance

### Security

- [ ] Authentication
- [ ] Authorization
- [ ] Input validation
- [ ] No secrets
- [ ] No sensitive logging

### Testing

- [ ] Unit tests
- [ ] Integration tests where required
- [ ] Edge cases

---

# 85. DOCUMENTATION STANDARD

Every major module should have appropriate documentation.

Documentation should explain:

- Purpose
- Architecture
- API
- Database behavior
- Events
- Redis usage
- Security
- Important decisions

Do not create documentation that contradicts the engineering documents.

---

# 86. ARCHITECTURE DECISION STANDARD

Major architectural decisions must be documented.

Examples:

- Why Redis is used
- Why Kafka is used
- Why a specific caching strategy exists
- Why an API was versioned
- Why a database index exists
- Why a WebSocket approach was selected

Avoid undocumented architecture changes.

---

# 87. FAILURE HANDLING

Infrastructure failures must be considered.

Examples:

```text
MySQL unavailable
Redis unavailable
Kafka unavailable
Media service unavailable
WebSocket disconnect
```

The application should fail gracefully where possible.

Do not hide infrastructure failures.

---

# 88. RESILIENCE

Where appropriate, use:

- Timeouts
- Retries
- Backoff
- Circuit breakers
- Dead-letter queues
- Idempotency
- Graceful degradation

Do not add resilience patterns blindly.

Use them where the architecture requires them.

---

# 89. SECURITY REVIEW BEFORE RELEASE

Before backend completion, explicitly inspect for:

```text
Authentication vulnerabilities
Authorization vulnerabilities
IDOR
SQL injection
XSS-related API risks
CSRF
CORS problems
Rate-limit bypass
File upload vulnerabilities
Sensitive data exposure
Secret leakage
JWT vulnerabilities
WebSocket authorization issues
Kafka security issues
Redis security issues
Admin privilege escalation
```

Fix identified issues before declaring production readiness.

---

# 90. OBSERVABILITY STANDARD

Important operations should be observable.

Monitor:

- Request latency
- HTTP errors
- Authentication failures
- Database errors
- Kafka failures
- Redis failures
- Background job failures
- WebSocket connection problems

Use the project's documented observability stack.

---

# 91. HEALTH CHECKS

Health checks should verify critical infrastructure appropriately.

Potential dependencies:

```text
Application
MySQL
Redis
Kafka
```

Health checks must not expose secrets or sensitive configuration.

---

# 92. NO OVERENGINEERING

Do not add technology just to make the project appear enterprise-level.

The project already uses:

```text
Spring Boot
MySQL
Redis
Kafka
WebSocket
```

Do not introduce additional infrastructure unless the documentation or a real requirement justifies it.

Especially avoid unnecessary:

```text
Microservices
Docker
Kubernetes
Elasticsearch
Service Mesh
Complex distributed transactions
```

---

# 93. BACKEND-FIRST RULE

During the current development phase:

```text
Backend = ACTIVE
Frontend = DEFERRED
```

Backend development takes priority.

Do not spend implementation effort on frontend UI while backend requirements remain incomplete.

The frontend will consume stable, tested backend APIs later.

---

# 94. ANTIGRAVITY AGENT BEHAVIOR

When Antigravity works on this project, it must:

1. Read `readme.md`.
2. Read relevant files from `instagram-clone-documents/`.
3. Read this `standard.md`.
4. Inspect the existing code.
5. Identify what already exists.
6. Avoid duplicating existing functionality.
7. Implement according to documented architecture.
8. Follow these coding standards.
9. Run relevant tests.
10. Fix failures.
11. Verify behavior.
12. Report what changed.

Never blindly rewrite the project.

---

# 95. AGENT MUST NOT

The agent must NOT:

- Introduce Docker
- Convert to microservices
- Hardcode secrets
- Expose entities
- Store plaintext passwords
- Ignore authorization
- Skip validation
- Create fake implementations
- Claim tests passed without running them
- Delete working code without justification
- Introduce unnecessary dependencies
- Build frontend features during backend-first development
- Mark incomplete modules as complete

---

# 96. WHEN A STANDARD CONFLICTS WITH DOCUMENTATION

Priority order:

```text
1. Security requirements
2. Explicit project architecture/documentation
3. This engineering standard
4. Existing implementation
5. Agent preference
```

If a conflict is discovered:

1. Do not silently choose.
2. Identify the conflict.
3. Explain it.
4. Follow the higher-priority requirement.
5. Document the decision if significant.

---

# 97. FINAL QUALITY BAR

Code is considered high quality when another developer can:

- Understand it
- Test it
- Modify it
- Debug it
- Extend it
- Secure it

without needing the original developer to explain every line.

---

# 98. FINAL ENGINEERING RULE

> **Build simple, secure, testable and maintainable software first.**

The objective is not to create the largest codebase.

The objective is to create a backend that is:

```text
Correct
+
Secure
+
Maintainable
+
Tested
+
Observable
+
Performant
+
Documented
```

---

# 99. PROJECT STANDARD SUMMARY

```text
Language              Java 21
Framework             Spring Boot
Architecture          Modular Monolith
Database              MySQL
Cache                 Redis
Events                Kafka
Real-time             WebSocket
API                   REST
Authentication        JWT
Password Security     BCrypt/approved encoder
Testing               Unit + Integration + API
Migration             Project-approved migration tool
Containerization      None
Microservices         None
Frontend               Deferred
```

---

# 100. ENGINEERING STANDARD STATUS

```text
Document:       standard.md
Purpose:        Coding & Engineering Standards
Scope:          Entire Project
Current Phase:  Backend
Architecture:   Modular Monolith
Status:         ACTIVE
```



**END OF ENGINEERING STANDARDS** -->
# UUID IDENTIFIER STANDARD

## 1. Primary Identifier Strategy

All persistent domain entities MUST use UUID as their primary identifier.

The project MUST NOT use auto-increment numeric IDs such as:

- `INT`
- `BIGINT`
- `Long`
- `Integer`

for entity primary keys.

### Standard

```text
Entity ID → UUID
Foreign Key → UUID
API Resource ID → UUID
```

---

## 2. Java Standard

Use:

```java
java.util.UUID
```

Example:

```java
@Id
@GeneratedValue
@Column(columnDefinition = "BINARY(16)")
private UUID id;
```

---

## 3. Database Standard

MySQL UUID values SHOULD be stored efficiently using:

```text
BINARY(16)
```

rather than storing UUIDs as unnecessarily large textual values.

Example:

```sql
id BINARY(16) NOT NULL PRIMARY KEY
```

Foreign keys referencing UUID identifiers must use the same compatible representation.

---

## 4. API Standard

REST APIs MUST expose UUID identifiers.

Example:

```text
GET /api/v1/users/{userId}

GET /api/v1/posts/{postId}

DELETE /api/v1/posts/{postId}

GET /api/v1/users/{userId}/followers
```

Example:

```text
/api/v1/users/550e8400-e29b-41d4-a716-446655440000
```

---

## 5. DTO Standard

Request and response DTOs must use:

```java
UUID id;
```

where an entity identifier is required.

Example:

```java
public record UserResponse(
        UUID id,
        String username,
        String email
) {}
```

---

## 6. Relationship Standard

All entity relationships must use UUID-compatible foreign keys.

Examples:

```text
user_id       → UUID
post_id       → UUID
comment_id    → UUID
message_id    → UUID
conversation_id → UUID
notification_id → UUID
```

Composite relationship tables must also use UUID foreign keys.

Example:

```text
follows
├── follower_id  UUID
└── following_id UUID
```

---

## 7. Numeric Fields

UUID is ONLY for entity/resource identification.

Do NOT replace naturally numeric values with UUID.

Examples:

```text
page              → Integer
pageSize          → Integer
likeCount         → Long
commentCount      → Long
viewCount         → Long
duration          → Integer/Long
sortOrder         → Integer
```

---

## 8. Security Requirement

The backend MUST NOT rely on sequential identifiers for authorization.

Every resource access must verify ownership or authorization independently.

UUID does NOT replace authorization checks.

Example:

```text
User A requests:

GET /api/v1/posts/{postId}

        ↓

Validate JWT
        ↓
Validate permissions
        ↓
Verify resource access
        ↓
Return resource
```

UUID is an identifier strategy, NOT an authorization mechanism.

---

## 9. Migration Requirement

If existing entities currently use numeric IDs, migrate them systematically.

Do NOT create a mixture such as:

```text
User.id       → UUID
Post.id       → Long
Comment.id    → UUID
Message.id    → Long
```

The project should maintain a consistent UUID-based identifier strategy.

All related foreign keys, DTOs, repository methods, service methods, controller parameters, migrations, tests, Kafka events, WebSocket messages, and API documentation must be updated accordingly.

---

## 10. Engineering Rule

UUID is the project's official persistent entity identifier standard.

New entities MUST use UUID.

Existing numeric entity identifiers MUST be migrated to UUID before the backend is considered complete.

No new numeric auto-increment entity IDs may be introduced.
---

# 11. PACKAGE ARCHITECTURE STANDARDS (UTILITY & COMMON)

## 1. Separation of Concerns
The backend enforces a clean distinction between:
- **BUSINESS LOGIC** (confined strictly within feature `*Service` implementations)
- **SHARED CONTRACTS & RESPONSES** (managed in `com.instagramclone.common`)
- **TECHNICAL UTILITIES** (managed in `com.instagramclone.utility`)
- **INFRASTRUCTURE CONFIGURATION** (managed in `com.instagramclone.config`)

## 2. The `utility` Package Rules
Every class in `com.instagramclone.utility` must satisfy:
1. **Single Responsibility**: Each utility class is focused on one technical domain (e.g. `UUIDUtility`, `FileUtility`, `RedisUtility`, `KafkaUtility`, `DateTimeUtility`, `PaginationUtility`, `ApiUtility`, `SecurityUtility`).
2. **No "God" Utility Classes**: Classes like `EverythingUtility.java` or `CommonHelper.java` are strictly prohibited.
3. **No Domain Decision-Making**: A utility must never implement business rules (e.g., `RedisUtility` provides `set(key, value, ttl)`, not `saveUserProfile()`).
4. **Clean Dependency Direction**:
   ```text
   Feature Controller  ---> ApiUtility / Common Response
   Feature Service     ---> Common Models / Utility Helpers
   Repository          ---> Only entities / DB types
   Utility             ---> Only technical primitives / standard libraries
   ```

## 3. The `common` Package Rules
1. **Authoritative Response Architecture**: All endpoints must return `ResponseEntity<ApiResponse<T>>` or `ResponseEntity<ApiResponse<PageResponse<T>>>`. No parallel response hierarchies (`ResponseWrapper`, `BaseResult`) are allowed.
2. **Authoritative Exceptions**: Global HTTP mapping is centralized in `GlobalExceptionHandler` using `ApiResponse.error(...)`.
3. **Categorized Constants**: All application-wide static constants must be categorized into specialized classes (`ApiConstants`, `SecurityConstants`, `CacheConstants`, `KafkaConstants`) rather than one massive generic file.

## 4. UUID & Pagination Integrity
- All entity identifiers and resource paths are strictly Java `UUID`.
- All paginated endpoints enforce bounds through `PaginationUtility.clamp()` or `PaginationUtility.createPageable()` with a strict maximum of 100 items per page to prevent memory exhaustion attacks.

---

# 12. PRODUCTION-READINESS, SECURITY & OBSERVABILITY STANDARDS

## 1. WebSocket Authorization Standard
- WebSockets must never assume that an authenticated HTTP handshake grants blanket access to all STOMP topics.
- Every destination with authorization requirements (such as `/topic/conversations/{conversationId}`) must inspect the authenticated principal in `ChannelInterceptor.preSend()` and verify resource membership before subscription approval.

## 2. Stateless Token Revocation Standard
- JWT access tokens must be revocable prior to expiration.
- Logout and credential update workflows must register the token identifier or SHA-256 hash in a distributed Redis blacklist (`blacklist:token:<hash>`) with a TTL matching the token's remaining validity duration.
- Critical credential updates (e.g., password change) must trigger user-level session invalidation (`blacklist:user:<userId>`) to invalidate active tokens across all client instances.

## 3. Social Privacy Boundary Standard
- Social relationships (follows, messages) must respect user blocking settings in both directions.
- Direct messaging and following between blocked pairs must fail with immediate client feedback and zero data leakage.

## 4. Centralized Error Contract Standard
- All uncaught exceptions, constraint violations, and framework deserialization errors must route through `GlobalExceptionHandler`.
- Error responses must strictly conform to `ApiResponse<T>` with standard status codes (400 for malformed input / type mismatch, 401 for bad/expired credentials, 403 for forbidden actions, 409 for unique constraints, 413 for oversized payloads, 429 for rate limit violations, 500 for internal errors).

## 5. Observability & Tracing Standard
- Every HTTP request must be tagged with a correlation ID (`X-Correlation-ID`) across SLF4J MDC and echoed back in the response headers.
- Health checks for external dependencies (Cloudinary, Redis, MySQL) must be exposed via Spring Boot Actuator health indicators without leaking private configuration credentials.

