# Instagram Clone — Backend Master Engineering Plan

> **Status:** Backend Development Phase  
> **Frontend Development:** Deferred  
> **Architecture:** Modular Monolith  
> **Primary Goal:** Complete, secure, tested, production-ready backend before frontend implementation begins.

---

# 1. PROJECT OBJECTIVE

This project is a production-oriented Instagram Clone designed to demonstrate real-world full-stack engineering practices.

The current development priority is:

> **COMPLETE THE ENTIRE BACKEND FIRST.**

The React frontend must NOT be actively implemented during this phase.

The backend must provide all required:

- REST APIs
- Authentication
- Authorization
- Database functionality
- Business logic
- Social graph
- Posts
- Media handling
- Feed
- Engagement
- Notifications
- Stories
- Reels
- Chat
- Search
- Settings
- Privacy
- Admin/moderation
- Redis functionality
- Kafka event processing
- WebSocket functionality
- Background processing
- Validation
- Error handling
- Security
- Observability
- Testing
- Performance controls
- Production-readiness features

The backend is considered complete only when all documented backend requirements have been implemented, tested and verified.

---

# 2. SOURCE OF TRUTH

The folder:

```text
instagram-clone-documents/
```

contains the project's professional engineering documentation.

These documents are the primary source of truth.

Before implementing any module, inspect the relevant documentation.

The documentation may define:

- Requirements
- Architecture
- Database schema
- ER design
- API contracts
- Security requirements
- Module boundaries
- Event contracts
- Redis usage
- Kafka usage
- WebSocket behavior
- Media architecture
- Background jobs
- Privacy requirements
- Moderation requirements
- Performance requirements
- Observability
- Deployment requirements
- Disaster recovery
- Data migration
- Feature flags
- Architecture evolution

DO NOT invent a competing architecture when the documentation already defines the required behavior.

If documentation and existing code conflict:

1. Identify the conflict.
2. Explain the conflict.
3. Prefer the documented architecture.
4. Preserve working functionality where possible.
5. Make the smallest safe change.
6. Never silently discard existing functionality.

---

# 3. CURRENT DEVELOPMENT RULE

## BACKEND FIRST

The development order is:

```text
Documentation
      ↓
Backend Foundation
      ↓
Authentication
      ↓
User/Profile
      ↓
Social Graph
      ↓
Posts
      ↓
Media
      ↓
Engagement
      ↓
Feed
      ↓
Notifications
      ↓
Stories
      ↓
Reels
      ↓
Chat
      ↓
Search/Explore
      ↓
Settings/Privacy
      ↓
Admin/Moderation
      ↓
Kafka/Event Processing
      ↓
Redis Optimization
      ↓
Background Jobs
      ↓
Security Hardening
      ↓
Integration Testing
      ↓
Performance
      ↓
Observability
      ↓
Production Readiness
      ↓
BACKEND COMPLETE
      ↓
FRONTEND DEVELOPMENT
```

Do NOT move to frontend development until the backend completion gate has been passed.

---

# 4. TECHNOLOGY STACK

## Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Maven
- Bean Validation
- JWT
- BCrypt/password hashing
- Database migration tooling defined by project documentation

## Database

- MySQL

## Caching / Fast Data

- Redis

## Event Streaming

- Apache Kafka

## Real-Time Communication

- WebSocket

## Media

- Object storage/media infrastructure defined by documentation
- CDN where required

## Development

- Antigravity IDE
- Git
- Postman/API testing tools

## Explicitly NOT Used

```text
Docker ❌
Kubernetes ❌
Microservices ❌
```

The initial architecture is a modular monolith.

---

# 5. ARCHITECTURE

The backend follows a modular monolith architecture.

General request flow:

```text
Client
   ↓
Controller
   ↓
Validation
   ↓
Service
   ↓
Repository
   ↓
MySQL
```

Infrastructure integrations:

```text
                 Spring Boot
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
     MySQL         Redis         Kafka
       │
       ↓
   Persistence

Spring Boot
     │
     ↓
 WebSocket
     │
     ↓
Real-time features
```

Business modules must remain logically separated.

Do not convert the project into microservices unless a future architecture document explicitly requires it.

---

# 6. ENGINEERING PRINCIPLES

All backend code must follow these principles:

- Clean architecture
- Separation of concerns
- SOLID principles
- DRY
- High cohesion
- Low coupling
- Secure-by-default design
- Testability
- Maintainability
- Explicit business rules
- Consistent API contracts
- Proper transaction boundaries
- Proper validation
- Defensive programming
- Observability
- Performance awareness

Avoid:

- Giant classes
- Giant methods
- Duplicate business logic
- Hardcoded credentials
- Hardcoded URLs
- Exposed entities
- Unnecessary abstractions
- Premature optimization
- Premature microservices
- Fake implementations
- Mock production behavior
- Silent exception handling

---

# 7. BACKEND PACKAGE ARCHITECTURE

Use modular boundaries.

Conceptually:

```text
com.instagramclone
│
├── config
│
├── common
│   ├── exception
│   ├── response
│   ├── validation
│   ├── security
│   └── util
│
├── auth
│   ├── controller
│   ├── service
│   ├── dto
│   └── security
│
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
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

The exact package structure must follow the architecture documentation.

Do not create meaningless empty packages.

---

# 8. API RULES

All REST APIs must follow the documented API contract.

For every endpoint verify:

- HTTP method
- URL
- Request DTO
- Response DTO
- Authentication requirement
- Authorization requirement
- Validation
- Error responses
- Pagination
- Sorting
- Filtering
- Idempotency where applicable
- Transaction behavior

Never expose JPA entities directly.

Use DTOs.

Example:

```text
Controller
    ↓
Request DTO
    ↓
Service
    ↓
Entity
    ↓
Repository
    ↓
Database
```

Response:

```text
Database
    ↓
Entity
    ↓
Mapper
    ↓
Response DTO
    ↓
Controller
    ↓
Client
```

---

# 9. DATABASE RULES

MySQL is the primary persistent datastore.

All schema changes must use the project's migration system.

Never manually modify production schema outside migrations.

Database design must follow:

```text
Instagram Clone Database ER Design
```

from the documentation folder.

Every entity must be reviewed for:

- Primary key
- Foreign keys
- Constraints
- Unique constraints
- Indexes
- Nullability
- Timestamps
- Soft deletion where required
- Audit fields where required
- Referential integrity

Avoid unnecessary indexes.

Avoid N+1 queries.

Use pagination for large collections.

Use transactions where business consistency requires them.

---

# 10. AUTHENTICATION

Authentication must be production-quality.

Required concepts include, according to documentation:

- Registration
- Login
- Password hashing
- AuthenticationManager
- UserDetails
- JWT
- JWT validation
- Security filters
- Authentication context
- Current-user resolution
- Unauthorized handling
- Token expiration
- Secure configuration

Never:

- Store plaintext passwords
- Return passwords
- Log passwords
- Hardcode JWT secrets
- Expose signing keys
- Trust user IDs supplied by clients for current-user operations

---

# 11. AUTHORIZATION

Every protected resource must verify ownership or permission.

Examples:

```text
User A cannot edit User B's profile.

User A cannot delete User B's post.

User A cannot access private resources without permission.

Admin operations require explicit authorization.
```

Never rely only on frontend authorization.

Authorization must be enforced by the backend.

---

# 12. USER / PROFILE MODULE

Implement according to the documentation.

Expected capabilities may include:

- User registration
- User lookup
- Current user
- Profile retrieval
- Profile update
- Username uniqueness
- Email uniqueness
- Profile image reference
- Account status
- Privacy settings
- Soft deletion where required

Do not add undocumented fields without justification.

---

# 13. FOLLOW / SOCIAL GRAPH

Implement the complete documented social graph.

Potential capabilities:

- Follow user
- Unfollow user
- Followers
- Following
- Follow status
- Follow requests where required
- Private accounts
- Block relationships
- Social graph validation

Ensure:

- Duplicate follows are prevented
- Self-follow behavior follows documentation
- Authorization is enforced
- Queries are indexed
- Pagination is implemented

---

# 14. POST MODULE

Implement the documented post lifecycle.

Potential lifecycle:

```text
Create Post
    ↓
Validate
    ↓
Attach Media
    ↓
Persist
    ↓
Publish Event
    ↓
Feed / Notification processing
```

Support only documented post types.

Requirements may include:

- Create
- Read
- Update
- Delete
- Visibility
- Ownership
- Captions
- Media references
- Hashtags
- Mentions
- Timestamps
- Soft deletion where required

---

# 15. MEDIA MODULE

Media storage, transformation, and delivery is powered by **Cloudinary**, backed by MySQL metadata persistence and Redis rate limiting.

### 15.1 Architectural Flow
1. **Frontend Upload:** Client issues `POST /api/v1/media` as `multipart/form-data`.
2. **Security & Rate Limiting:** Enforced via Spring Security JWT and Redis-backed `@RateLimit(category = "media-upload", limit = 15, windowSeconds = 60)`.
3. **File Validation:** `FileUtility.validateFile(file, true)` restricts image uploads to 10MB (JPEG, PNG, WebP, GIF) and video uploads to 50MB (MP4, MOV, WebM).
4. **Cloudinary Pipeline:** Uploaded to Cloudinary with UUID-based folders (`instagram-clone/{category}/{userUuid}`) and unique public IDs.
5. **Metadata Persistence:** Assets are recorded in the MySQL `media` table (Flyway migration `V4__media_metadata.sql`). Binaries are **never** stored in MySQL.
6. **Resilient Fallback:** If Cloudinary credentials are not configured or set to placeholder/demo values, the system safely falls back to local disk storage (`/uploads/...`).

### 15.2 Media API Endpoints
- `POST /api/v1/media`: Upload a media file (form parameter: `file`, optional `folder`). Returns `ApiResponse<MediaResponse>`.
- `GET /api/v1/media/{mediaId}`: Fetch metadata for a stored media asset.
- `DELETE /api/v1/media/{mediaId}`: Delete media from MySQL and Cloudinary (requires ownership authorization).

### 15.3 Cross-Domain Lifecycle
- **Profile Avatars:** Uploaded to `instagram-clone/avatars/{userUuid}`. When replacing an avatar, the new image is uploaded first; the old Cloudinary asset is removed only after MySQL is updated.
- **Posts & Carousels:** Images/videos uploaded to `instagram-clone/posts/{userUuid}`. When a post is deleted, associated media assets in Cloudinary are destroyed.
- **Stories & Reels:** Uploaded to `instagram-clone/stories/{userUuid}` and `instagram-clone/reels/{userUuid}`. On deletion or expiration, media assets are purged.

For technical specifications, refer to [mediaTechnicalDocumentation.md](file:///A:/Instragram-clone/backend/mediaTechnicalDocumentation.md).

---

# 16. ENGAGEMENT MODULE

Implement documented engagement functionality.

Potential features:

- Likes
- Unlike
- Comments
- Delete comments
- Replies
- Saves/bookmarks
- Engagement counts
- Mention handling

Ensure:

- Duplicate likes are prevented
- Authorization is enforced
- Counts remain consistent
- Large collections are paginated
- Important operations are transactional

---

# 17. FEED MODULE

The feed is a critical backend module.

Implement according to:

```text
Instagram Clone Feed Module Technical Design
```

Do not build an unnecessarily complex recommendation engine unless the documentation requires it.

The feed must consider the documented:

- Following relationships
- Post visibility
- Ranking/order
- Pagination
- Caching
- Deleted content
- Blocked users
- Private accounts
- Performance requirements

Redis should only be introduced where the documented design requires it.

---

# 18. NOTIFICATION MODULE

Implement backend notification functionality.

Potential notification events:

```text
Follow
Like
Comment
Mention
Message
Story interaction
Other documented events
```

Use Kafka where asynchronous processing is defined.

Notifications should support:

- Creation
- Read/unread state
- Retrieval
- Pagination
- Real-time delivery where required

---

# 19. STORIES

Implement the documented Stories module.

Potential capabilities:

- Create story
- Media reference
- Expiration
- Story visibility
- Story viewers
- Story deletion
- Story ordering
- Story cleanup

Time-based behavior must be handled safely.

Expired stories must not remain visible.

Use background processing where required.

---

# 20. REELS

Implement the documented Reels backend.

Potential capabilities:

- Create reel
- Media
- Caption
- Visibility
- Engagement
- Comments
- Views
- Feed/explore integration where documented

Do not create an artificial recommendation algorithm unless required by documentation.

---

# 21. CHAT / MESSAGING

Implement according to the messaging documentation.

Potential components:

```text
Conversation
Message
Participant
Message status
Read status
Typing state
Presence
```

Use:

- REST APIs for persistent operations
- WebSocket for real-time communication
- Redis only where required
- Kafka only where documented for asynchronous events

Messages must be authorized.

Users must not access conversations they do not belong to.

---

# 22. SEARCH / EXPLORE

Implement documented search functionality.

Potential targets:

- Users
- Posts
- Hashtags
- Reels
- Other documented entities

Requirements:

- Input validation
- Pagination
- Search limits
- Authorization
- Performance-aware queries

Do not introduce Elasticsearch or another search engine unless the architecture documentation explicitly requires it.

---

# 23. SETTINGS / PRIVACY

Implement documented settings.

Potential areas:

- Account settings
- Privacy
- Notification preferences
- Security settings
- Private account
- Blocked users
- Data/privacy controls

Security-sensitive settings require re-authentication or additional verification where documented.

---

# 24. ADMIN / MODERATION

Implement the documented administration system.

Potential capabilities:

- User moderation
- Content moderation
- Reports
- Account actions
- Content removal
- Audit logs
- Administrative authorization

Admin endpoints must never become publicly accessible.

Use explicit role/permission checks.

---

# 25. REDIS ARCHITECTURE & PRODUCTION INTEGRATION

Redis is an infrastructure component strictly for ephemeral state, distributed coordination, and performance acceleration. It is NEVER a replacement for MySQL, which remains the authoritative persistent source of truth.

### 25.1 Architectural Invariants
1. **MySQL Authority:** MySQL remains the authoritative persistent database. All critical entities (users, posts, follows, comments, messages, stories) are permanently stored in MySQL.
2. **Dual-Write Prevention:** Cache updates and counter modifications are evicted or synchronized after successful MySQL database transactions.
3. **Graceful Degradation:** When Redis is unreachable or timed out, the application falls back safely without unhandled 500 errors.
4. **Key Hygiene & Bounded TTLs:** Every Redis key MUST have an explicit, bounded TTL and use uniform prefix naming (`com.instagramclone.common.constants.CacheConstants`).

### 25.2 Implemented Redis Production Capabilities
1. **Distributed Rate Limiting (`rate_limit:*`):**
   - Implemented via `RedisRateLimiterService` using an atomic Lua script for fixed/sliding windows.
   - Enforced by `@RateLimit` annotations across auth, content creation, comments, media upload, and chat APIs.
   - Fail-closed policy for sensitive authentication brute-force vectors; fallback for less sensitive endpoints.
2. **Distributed Lock for Scheduled Background Jobs (`lock:job:*`):**
   - Implemented in `RedisDistributedLockService`.
   - Uses `SET key owner NX PX leaseMs` with safe release via atomic Lua script checking owner match.
   - Prevents duplicate execution across multiple backend nodes during `cleanupExpiredStories` and `cleanupExpiredTokens`.
3. **Real-Time Multi-Device Presence & Typing (`presence:user:*`, `chat:typing:*`):**
   - Multi-device sessions tracked via Redis Sets (`presence:user:<userId>:sessions`).
   - Disconnecting one device leaves user online if other sessions remain active (`sCard > 0`).
   - Automatically records `last_seen` timestamp with 30-day bounded TTL.
   - Typing indicator keys (`chat:typing:<conversationId>:<userId>`) expire automatically via 5-second TTL.
   - Strict privacy enforcement: Presence queries mask online status and timestamps if users have blocked each other.
4. **High-Read Engagement Counter Caching (`count:post:likes:*`, `count:post:comments:*`):**
   - Accelerates high-frequency feed/post reads by caching like and comment counts with a 10-minute TTL.
   - Read-through caching with instantaneous eviction upon user mutations (`toggleLike`, `addComment`, `deleteComment`).
5. **Public User Profile Caching (`cache:user:profile:*`):**
   - Caches public profile metadata with a 10-minute TTL.
   - **Privacy Boundary Rule:** Viewer-specific follow status (`followedByCurrentUser`) is dynamically calculated and injected per request to prevent cross-viewer privacy leakage.
   - Evicted immediately upon profile edits, avatar updates, follows, and unfollows.
6. **Kafka Domain Event Idempotency (`idempotency:event:*`):**
   - Deduplicates redelivered Kafka domain events in `DomainEventConsumer` using atomic `setIfAbsent()` with a 24-hour TTL.
7. **JWT Token Blacklisting & Session Revocation (`blacklist:token:*`, `revocation:user:*`):**
   - Ephemeral token blacklist expiring at the exact remaining JWT lifetime.
   - User-level revocation timestamps invalidating all existing tokens on password reset or account security actions.
8. **Unread Notification Count Caching (`cache:notifications:unread:*`):**
   - Cached count with 15-minute TTL, evicted on notification arrival or read acknowledgment.





- Cache
- Rate limiting
- Temporary state
- Counters
- OTP/ephemeral data if documented
- Presence
- Feed caching
- Other documented use cases

Rules:

- Define TTLs
- Avoid stale critical data
- Do not treat cache as permanent storage
- Handle Redis failure gracefully where required
- Avoid cache stampedes
- Invalidate cache deliberately

---

# 26. KAFKA

Kafka is the asynchronous event infrastructure.

Use Kafka for documented domain events.

Conceptually:

```text
Business Action
      ↓
Domain Event
      ↓
Kafka
      ↓
Consumer
      ↓
Async Processing
```

Examples may include:

```text
UserFollowed
PostCreated
PostLiked
CommentCreated
MessageSent
NotificationCreated
MediaUploaded
StoryCreated
```

Only create events defined or justified by the architecture.

Kafka consumers must consider:

- Consumer groups
- Serialization
- Retry
- Failure handling
- Idempotency
- Dead-letter strategy where documented
- Event versioning
- Observability

Do not use Kafka simply because it is installed.

---

# 27. WEBSOCKET

WebSocket is used for real-time behavior.

Potential uses:

- Chat
- Notifications
- Presence
- Typing indicators
- Other documented real-time functionality

Requirements:

- Authentication
- Authorization
- Connection lifecycle
- Error handling
- Message validation
- Disconnect handling
- Observability

Do not allow unauthorized subscriptions.

---

# 28. BACKGROUND JOBS

Implement background processing defined by documentation.

Potential jobs:

- Story expiration
- Media processing
- Notification processing
- Cleanup
- Data maintenance
- Retry processing
- Other scheduled tasks

Jobs must be:

- Idempotent where possible
- Observable
- Failure tolerant
- Safe to retry

---

# 29. VALIDATION

All external input must be validated.

Validate:

- Request body
- Query parameters
- Path variables
- File metadata
- Pagination parameters
- Sorting
- Search input

Never trust frontend validation.

Backend validation is authoritative.

---

# 30. ERROR HANDLING

Use centralized exception handling.

Return consistent errors.

Never expose:

- Stack traces
- SQL errors
- Internal class names
- Password information
- JWT secrets
- Infrastructure credentials

Errors should contain enough information for clients while avoiding information leakage.

---

# 31. LOGGING

Use structured, meaningful logs.

Log important:

- Authentication events
- Authorization failures
- Business failures
- Kafka failures
- Redis failures
- WebSocket failures
- Background job failures
- Database/infrastructure failures

Never log:

- Passwords
- JWT tokens
- Secrets
- Sensitive personal information unnecessarily

---

# 32. SECURITY HARDENING

Before backend completion, perform a dedicated security review.

Check:

- Authentication
- Authorization
- JWT handling
- Password security
- CORS
- CSRF strategy
- Rate limiting
- Input validation
- SQL injection
- IDOR/resource ownership
- User enumeration
- Sensitive data exposure
- File upload security
- WebSocket authorization
- Kafka security configuration
- Redis security configuration
- Admin authorization
- Error leakage
- Secrets management

Follow the project's OWASP/security documentation.

---

# 33. TESTING STRATEGY

The backend is NOT complete merely because it compiles.

Testing must include appropriate:

## Unit Tests

- Services
- Validators
- Security logic
- Business rules
- Utility logic

## Repository Tests

- Important queries
- Constraints
- Relationships
- Pagination

## Integration Tests

- REST APIs
- Database
- Security
- Authentication
- Authorization
- Redis integrations where appropriate
- Kafka integrations where appropriate

## End-to-End Backend Flows

Examples:

```text
Register
 ↓
Login
 ↓
JWT
 ↓
Create Profile
 ↓
Follow
 ↓
Create Post
 ↓
Like
 ↓
Comment
 ↓
Notification
```

The exact flows must follow project documentation.

---

# 34. API VERIFICATION

Use Postman or equivalent API tooling to verify real behavior.

Do not claim:

```text
"API works"
```

without testing the actual endpoint.

Verify:

- Success
- Validation failure
- Unauthorized
- Forbidden
- Not found
- Duplicate data
- Invalid input
- Edge cases

---

# 35. PERFORMANCE

Before backend completion, review:

- Database indexes
- Query performance
- N+1 queries
- Pagination
- Connection pool
- Redis usage
- Kafka throughput
- WebSocket behavior
- Large payloads
- Media metadata
- Feed performance
- Notification volume

Do not optimize blindly.

Use evidence from tests/metrics where possible.

---

# 36. OBSERVABILITY

Backend must provide sufficient visibility into:

- Application health
- Database health
- Redis health
- Kafka health
- API errors
- Request latency
- Important business events
- Background jobs
- WebSocket connections

Use the observability architecture documented in:

```text
instagram-clone-documents/
```

---

# 37. CONFIGURATION

Never hardcode environment-specific values.

Use configuration/environment variables for:

- Database credentials
- JWT secrets
- Redis connection
- Kafka connection
- Media credentials
- External service credentials
- URLs
- Security configuration

Maintain safe development configuration.

Never commit production secrets.

---

# 38. GIT / SECURITY

The repository must not contain:

```text
.env
secrets
passwords
private keys
JWT secrets
database credentials
API keys
cloud credentials
```

Use `.gitignore` appropriately.

Review Git status before commits.

---

# 39. IMPLEMENTATION WORKFLOW

For EVERY backend module, follow:

```text
1. Read documentation
       ↓
2. Inspect existing code
       ↓
3. Identify dependencies
       ↓
4. Design implementation
       ↓
5. Implement database changes
       ↓
6. Implement entities
       ↓
7. Implement repositories
       ↓
8. Implement services
       ↓
9. Implement DTOs/mappers
       ↓
10. Implement controllers
       ↓
11. Implement security
       ↓
12. Implement events if required
       ↓
13. Implement Redis if required
       ↓
14. Implement WebSocket if required
       ↓
15. Write tests
       ↓
16. Build
       ↓
17. Run tests
       ↓
18. Verify APIs
       ↓
19. Security review
       ↓
20. Performance review
       ↓
21. Documentation comparison
       ↓
22. Mark module COMPLETE
```

---

# 40. DEFINITION OF DONE

A backend module is NOT complete until:

- [ ] Documentation reviewed
- [ ] Database design implemented
- [ ] Migration created
- [ ] Entity implemented
- [ ] Repository implemented
- [ ] Service implemented
- [ ] DTOs implemented
- [ ] Mapping implemented
- [ ] Controller implemented
- [ ] Validation implemented
- [ ] Authentication implemented where required
- [ ] Authorization implemented
- [ ] Error handling implemented
- [ ] Redis implemented where required
- [ ] Kafka implemented where required
- [ ] WebSocket implemented where required
- [ ] Unit tests created
- [ ] Integration tests created where appropriate
- [ ] API tested
- [ ] Build successful
- [ ] Tests successful
- [ ] Security review completed
- [ ] Performance reviewed
- [ ] Documentation verified

Only then mark the module COMPLETE.

---

# 41. BACKEND MASTER CHECKLIST

## Foundation

- [ ] Spring Boot
- [ ] Java 21
- [ ] Maven
- [ ] MySQL
- [ ] Migration system
- [ ] Redis
- [ ] Kafka
- [ ] WebSocket
- [ ] Configuration
- [ ] Exception handling
- [ ] Validation
- [ ] Logging
- [ ] Health checks

## Authentication

- [ ] Registration
- [ ] Login
- [ ] Password hashing
- [ ] JWT
- [ ] JWT validation
- [ ] Security filter
- [ ] Authentication context
- [ ] Authorization
- [ ] Current user
- [ ] Logout/token strategy
- [ ] Rate limiting where required

## User

- [ ] User entity
- [ ] User repository
- [ ] User service
- [ ] User DTOs
- [ ] Profile
- [ ] Profile update
- [ ] Account status
- [ ] Privacy

## Social Graph

- [ ] Follow
- [ ] Unfollow
- [ ] Followers
- [ ] Following
- [ ] Private accounts
- [ ] Follow requests if required
- [ ] Block
- [ ] Social graph indexes

## Content

- [ ] Posts
- [ ] Media
- [ ] Captions
- [ ] Hashtags
- [ ] Mentions
- [ ] Visibility
- [ ] Delete/update
- [ ] Media metadata

## Engagement

- [ ] Likes
- [ ] Comments
- [ ] Replies
- [ ] Saves
- [ ] Counters

## Feed

- [ ] Feed generation
- [ ] Visibility rules
- [ ] Block rules
- [ ] Pagination
- [ ] Ranking/order
- [ ] Redis where required
- [ ] Performance optimization

## Notifications

- [ ] Notification creation
- [ ] Notification retrieval
- [ ] Read/unread
- [ ] Kafka events
- [ ] WebSocket delivery
- [ ] Pagination

## Stories

- [ ] Story creation
- [ ] Story retrieval
- [ ] Expiration
- [ ] Viewers
- [ ] Visibility
- [ ] Cleanup

## Reels

- [ ] Reel creation
- [ ] Reel retrieval
- [ ] Media
- [ ] Engagement
- [ ] Views
- [ ] Visibility

## Chat

- [ ] Conversations
- [ ] Participants
- [ ] Messages
- [ ] Message status
- [ ] Read status
- [ ] WebSocket
- [ ] Presence
- [ ] Authorization

## Search

- [ ] User search
- [ ] Post search
- [ ] Hashtag search
- [ ] Pagination
- [ ] Input validation
- [ ] Performance

## Settings / Privacy

- [ ] Account settings
- [ ] Privacy
- [ ] Notification preferences
- [ ] Security settings
- [ ] Blocked users
- [ ] Data controls

## Admin

- [ ] Admin authentication
- [ ] Authorization
- [ ] User moderation
- [ ] Content moderation
- [ ] Reports
- [ ] Audit logging

## Infrastructure

- [ ] Redis
- [ ] Kafka
- [ ] WebSocket
- [ ] Background jobs
- [ ] Media infrastructure
- [ ] Observability

## Quality

- [ ] Unit tests
- [ ] Integration tests
- [ ] API tests
- [ ] Security testing
- [ ] Performance testing
- [ ] Error handling
- [ ] Logging
- [ ] Documentation verification

---

# 42. BACKEND COMPLETION GATE

The backend may be declared COMPLETE only when:

```text
ALL DOCUMENTED BACKEND REQUIREMENTS
                ↓
          IMPLEMENTED
                ↓
             TESTED
                ↓
            VERIFIED
                ↓
          SECURITY REVIEWED
                ↓
        PERFORMANCE REVIEWED
                ↓
       PRODUCTION READINESS
                ↓
             COMPLETE
```

The presence of compiled code is NOT sufficient.

The presence of passing unit tests is NOT sufficient.

The backend must satisfy the documented functional, security, data, integration and operational requirements.

---

# 43. FRONTEND DEVELOPMENT GATE

DO NOT begin serious frontend development until:

- Backend modules are complete
- APIs are stable
- Authentication is stable
- Database migrations are stable
- Security is reviewed
- Redis integrations are verified
- Kafka integrations are verified
- WebSocket APIs are verified
- API contracts are documented
- Backend integration tests pass
- Production-readiness review is complete

After that, frontend development can begin.

---

# 44. ANTIGRAVITY AGENT RULES

The Antigravity coding agent must follow these rules.

## Rule 1 — Documentation First

Before implementing a module, read its relevant documentation.

## Rule 2 — Inspect Before Changing

Never blindly overwrite existing code.

## Rule 3 — Backend Only

During the current phase:

```text
Backend → YES
Frontend feature development → NO
```

Do not spend implementation effort on React screens unless explicitly requested.

## Rule 4 — No Docker

Never introduce Docker.

## Rule 5 — No Microservices

Keep the application modular monolithic.

## Rule 6 — No Fake Features

Do not create mock implementations just to mark a task complete.

## Rule 7 — Verify Everything

Do not claim success without executing the relevant build/test/verification commands.

## Rule 8 — No Premature Completion

If a module is partially implemented, mark it PARTIAL.

## Rule 9 — No Silent Architecture Changes

Explain architecture changes before making major changes.

## Rule 10 — Protect Existing Work

Reuse existing working code.

---

# 45. AGENT EXECUTION COMMAND

When this README is used as the active project instruction, the agent should execute the following strategy:

```text
READ README
   ↓
READ DOCUMENTATION
   ↓
INSPECT CURRENT BACKEND
   ↓
IDENTIFY COMPLETED MODULES
   ↓
IDENTIFY PARTIAL MODULES
   ↓
IDENTIFY MISSING MODULES
   ↓
SELECT NEXT BACKEND MODULE
   ↓
IMPLEMENT
   ↓
TEST
   ↓
VERIFY
   ↓
SECURITY REVIEW
   ↓
PERFORMANCE REVIEW
   ↓
MARK COMPLETE
   ↓
MOVE TO NEXT BACKEND MODULE
```

Do not stop after implementing only one controller or one entity.

Complete each module vertically from database to API to tests.

---

# 46. IMPORTANT — DO NOT BUILD EVERYTHING BLINDLY

The agent must not generate the entire backend in one uncontrolled operation.

Work module-by-module.

For each module:

1. Understand requirements.
2. Inspect existing code.
3. Implement.
4. Test.
5. Verify.
6. Fix.
7. Mark complete.
8. Move to the next module.

This prevents:

- Duplicate code
- Architecture drift
- Broken dependencies
- Inconsistent APIs
- Security holes
- Database conflicts
- Unmaintainable code

---

# 47. CURRENT PROJECT PRIORITY

At the beginning of each session, determine:

```text
What backend modules are already COMPLETE?
What backend modules are PARTIAL?
What backend modules are MISSING?
What documentation requirements remain?
```

Then continue from the earliest incomplete dependency.

Never restart the project unnecessarily.

---

# 48. FINAL BACKEND OBJECTIVE

The final backend should provide a coherent production-quality system:

```text
                         CLIENT
                           │
                           ▼
                    REST / WebSocket
                           │
                           ▼
                  Spring Boot Backend
                           │
       ┌───────────────────┼───────────────────┐
       │                   │                   │
       ▼                   ▼                   ▼
   REST APIs          WebSocket            Events
       │                   │                   │
       ▼                   ▼                   ▼
   Services           Real-time             Kafka
       │                                      │
       ▼                                      ▼
   Repositories                         Consumers/Workers
       │
       ▼
      MySQL
       ▲
       │
     Redis
```

The system must support:

- Secure users
- Social relationships
- Content
- Media
- Engagement
- Personalized feed
- Notifications
- Stories
- Reels
- Messaging
- Search
- Privacy
- Moderation
- Events
- Caching
- Background processing
- Observability
- Testing
- Production readiness

---

# 49. FINAL SUCCESS CRITERIA

The backend project is successful when:

```text
✅ All documented backend requirements implemented

✅ All backend modules integrated

✅ Database schema complete

✅ API contracts implemented

✅ Authentication secure

✅ Authorization enforced

✅ Redis correctly integrated

✅ Kafka correctly integrated

✅ WebSocket correctly integrated

✅ Background jobs implemented

✅ Error handling consistent

✅ Security reviewed

✅ Unit tests passing

✅ Integration tests passing

✅ API flows verified

✅ Performance reviewed

✅ Observability implemented

✅ No hardcoded secrets

✅ No Docker

✅ No unnecessary microservices

✅ Documentation and implementation aligned

✅ Backend production-ready
```

---

# 50. FINAL INSTRUCTION TO THE AGENT

**Complete the backend.**

Do not treat this README as a suggestion.

Treat:

```text
instagram-clone-documents/
```

as the detailed engineering specification and this README as the execution/coordination contract.

The ultimate objective is:

> **Finish and verify the complete Instagram Clone backend before serious frontend development begins.**

When one backend module is complete, continue with the next dependency until the entire backend completion gate is satisfied.

Do not stop merely because the application compiles.

Do not move to frontend until the backend is genuinely complete.

---

## PROJECT STATUS

```text
Backend:        IN PROGRESS
Frontend:       DEFERRED
Architecture:   MODULAR MONOLITH
Docker:         NOT USED
MySQL:          REQUIRED
Redis:          REQUIRED
Kafka:          REQUIRED
WebSocket:      REQUIRED
Goal:           COMPLETE BACKEND FIRST
```


---

# 51. BACKEND PACKAGE ARCHITECTURE — UTILITY & COMMON

The backend architecture strictly decouples business domain logic from reusable technical utilities and application-wide shared contracts:

```text
com.instagramclone
├── common/                     # Application-wide shared concepts, models, and contracts
│   ├── constants/              # Shared constants (ApiConstants, SecurityConstants, CacheConstants, KafkaConstants)
│   ├── event/                  # Domain event envelopes and publishing (DomainEvent, DomainEventPublisher)
│   ├── exception/              # Global exceptions and GlobalExceptionHandler
│   ├── response/               # Authoritative response wrappers (ApiResponse<T>, PageResponse<T>)
│   └── util/                   # Backward-compatible delegate wrappers (SecurityUtils)
│
├── utility/                    # Stateless, reusable technical helpers
│   ├── ApiUtility.java         # Standardizes ResponseEntity<ApiResponse<T>> and PageResponse construction
│   ├── UUIDUtility.java        # Strict UUID parsing, validation, and zero numeric fallback enforcement
│   ├── DateTimeUtility.java    # ISO formatting, UTC timestamps, and expiration helpers
│   ├── PaginationUtility.java  # PageRequest builder and boundary clamping (max page size: 100)
│   ├── FileUtility.java        # Media validation (10MB image, 50MB video) and unique filename generation
│   ├── SecurityUtility.java    # Authentication context extraction (UserPrincipal, UUID, username)
│   ├── RedisUtility.java       # Cache key building, safe get/set/delete/hasKey with fallback
│   └── KafkaUtility.java       # Reusable Kafka publish helper with non-blocking error handling
│
├── config/                     # Spring & infrastructure configuration (Security, Redis, Kafka, WebSocket)
├── ratelimit/                  # Distributed Redis rate limiting interceptors & annotations
├── audit/                      # Structured audit logging entity, repository, and async service
│
└── [feature]/                  # Modular domain features (user, post, story, reel, feed, follow, engagement, chat, notification)
    ├── controller/             # REST API endpoints
    ├── service/                # Business domain logic
    ├── repository/             # Spring Data JPA data access
    ├── entity/                 # JPA domain entities
    └── dto/                    # Feature-specific request/response models
```

---

# 52. PACKAGE RESPONSIBILITIES & DEPENDENCY RULES

1. **COMMON Package Responsibility**:
   - Represents the shared vocabulary and contracts of the entire monolith.
   - Houses authoritative API response schemas (`ApiResponse<T>`, `PageResponse<T>`), shared constants, and global exception handling.
   - **Rule**: Common must never import or depend upon feature-specific services or controllers.

2. **UTILITY Package Responsibility**:
   - Houses stateless, low-level technical helper functions that are called across multiple modules.
   - **Rule**: Utility classes never contain business logic, database queries, or workflow decisions.
   - **Rule**: Utility never depends on feature services or controllers. No circular dependencies are permitted.

3. **FEATURE Package Responsibility**:
   - Contains high-cohesion domain logic, validation, entities, and data access.
   - Controllers remain thin and delegate directly to services.
   - Services consume `Utility` and `Common` components, never exposing internal repositories across feature boundaries.

---

# 53. PRODUCTION-READINESS & SECURITY HARDENING ARCHITECTURE

### 53.1 WebSocket STOMP Channel Authorization
- **Independent Security Boundary:** WebSocket STOMP connections operate with channel interceptor verification (`WebSocketAuthInterceptor`) independently from HTTP filter chains.
- **Pre-Send Interception:**
  - `STOMP CONNECT`: Validates `Authorization: Bearer <token>`, verifies JWT signature, checks `TokenBlacklistService`, resolves `UserPrincipal`, and registers authenticated user in `accessor.setUser(...)` and `SecurityContextHolder`.
  - `STOMP SUBSCRIBE`: Intercepts destination patterns. When subscribing to `/topic/conversations/{conversationId}`, queries `ConversationMemberRepository` to enforce that the authenticated user is a valid participant of the conversation. Unauthorized subscriptions are rejected immediately with `SecurityException`.
- **Fault-Tolerant Header Mutation:** Safely handles immutable message header envelopes during testing and production dispatch.

### 53.2 Stateless JWT Blacklisting & Dynamic Session Revocation
- **Architecture:** `TokenBlacklistService` backed by Redis with a high-performance in-memory concurrent map fallback when Redis is unreachable.
- **Key Schemas & TTL:**
  - Token-level revocation: `blacklist:token:<sha256(jwt)>` with TTL equal to the token's remaining validity duration.
  - User-level session revocation: `blacklist:user:<userId>` with TTL equal to maximum access token lifetime (15 minutes).
- **Enforcement Points:**
  - Evaluated in `JwtAuthenticationFilter` on every HTTP request.
  - Evaluated in `WebSocketAuthInterceptor` on STOMP CONNECT.
- **Revocation Triggers:**
  - **Explicit Logout:** `POST /api/v1/auth/logout` extracts the Bearer token, computes its SHA-256 hash, and blacklists it until expiration while revoking active refresh tokens.
  - **Password Change:** `PUT /api/v1/users/me/password` revokes all active database refresh tokens and blacklists active JWT access sessions in Redis, terminating all existing sessions across devices.

### 53.3 Bidirectional User Block Privacy & Social Boundaries
- **Social Graph Protection:** `UserBlockRepository` checks integrated across social and messaging boundaries.
- **Enforcement:**
  - `FollowServiceImpl`: Verifies neither user has blocked the other before allowing follow requests (`BAD_REQUEST` with informative privacy message).
  - `ChatServiceImpl`: Prevents creating direct message conversations between users where a block relationship exists.

### 53.4 Centralized Exception & Payload Handling
- Standardized error contracts via `GlobalExceptionHandler` and `ApiResponse.error()`:
  - `DataIntegrityViolationException`: Maps database constraint violations to HTTP 409 Conflict.
  - `HttpMessageNotReadableException`: Maps malformed JSON or invalid request payloads to HTTP 400 Bad Request.
  - `MethodArgumentTypeMismatchException`: Maps invalid path variable formats (e.g., malformed UUIDs) to HTTP 400 Bad Request.
  - `MaxUploadSizeExceededException`: Maps oversized multipart uploads to HTTP 413 Payload Too Large.

### 53.5 Observability, Distributed Tracing & Health
- **Correlation ID Tracking:** `CorrelationIdFilter` inspects incoming `X-Correlation-ID` headers or generates random UUIDs. Pushes `correlationId` into SLF4J MDC and echoes it in HTTP response headers.
- **Structured Logging:** Formatted log output `[%X{correlationId}]` correlates log lines across HTTP filters, service calls, and database operations.
- **Actuator Health Subsystems:** Custom `CloudinaryHealthIndicator` verifies Cloudinary API reachability and status under `/actuator/health`.

### 53.6 Performance & Database Connection Optimization
- **N+1 Prevention:** Global Hibernate batch-fetching enabled via `spring.jpa.properties.hibernate.default_batch_fetch_size: 30`.
- **HikariCP Pool Parameters:** Production baseline configured with `maximum-pool-size: 20`, `minimum-idle: 10`, `idle-timeout: 300000ms`, `connection-timeout: 20000ms`.

---

# 54. INSTAGRAM-STYLE EMAIL AND ACCOUNT VERIFICATION SYSTEM

### 54.1 Architectural Overview
The email and account verification system provides production-grade identity verification, password recovery, session invalidation, and transactional email delivery for the Instagram Clone backend. It is designed to resist user enumeration attacks, prevent database token leaks, and guarantee distributed event consistency.

```text
[User / Client]
       │
       ▼
[AuthController] ── (Rate Limited via Redis @RateLimit)
       │
       ▼
[AuthService] ── (MySQL Transaction: user + tokens)
       │
       ▼ (afterCommit())
[EmailEventPublisher]
       │
       ├──► [Spring ApplicationEventPublisher] (Async In-Memory)
       └──► [Kafka Producer] (Multi-Node Cluster)
                  │
                  ▼
          [EmailEventListener] (Redis Idempotency: 24h Deduplication)
                  │
                  ▼
          [MailService / JavaMailSender] ──► [HTML Email with Fallback]
```

### 54.2 Cryptographic Token Design & Anti-Enumeration Defense
1. **Blind Token Storage (Never Store Raw Tokens):**
   - 256 bits of cryptographic entropy are generated using `SecureRandom` as a 64-character hex token string (`rawToken`).
   - The token is hashed with **SHA-256** before persisting to MySQL (`token_hash VARCHAR(64) UNIQUE`).
   - Only the raw token is transmitted to the user in their email verification or password reset link.
   - If the database is compromised, attackers cannot reconstruct valid verification or password reset links.
2. **Account Enumeration Defense:**
   - `POST /api/v1/auth/forgot-password` and `POST /api/v1/auth/resend-verification` return **identical HTTP 200 responses** (`"If an account exists with this email, a verification link has been sent."` / `"If an account exists with this email, a password reset link has been sent."`) regardless of whether the email is registered or verified.
   - Timing and error paths are normalized to prevent attackers from discovering registered user emails.

### 54.3 Verification & Password Reset Workflows
1. **Registration Verification:**
   - On registration, user is marked `email_verified = false`. An `EmailVerificationToken` is generated with 24-hour expiration (`app.mail.verification-token-validity-minutes: 1440`).
   - `POST /api/v1/auth/verify-email`: Accepts `{"token": "..."}`, validates signature/expiry, marks user `email_verified = true` with `email_verified_at = NOW()`, and marks token `used_at = NOW()`.
   - Single-use guarantee: Reused, expired, or tampered tokens are rejected with HTTP 400 Bad Request.
2. **Resend Verification:**
   - `POST /api/v1/auth/resend-verification`: Rate-limited (3 requests/minute). Invalidates all previous unused verification tokens for that user and issues a fresh token.
3. **Password Reset & Session Revocation:**
   - `POST /api/v1/auth/forgot-password`: Rate-limited (3 requests/minute). Invalidates previous unused reset tokens and creates an expiring token (30 minutes).
   - `POST /api/v1/auth/reset-password`: Accepts `{"token": "...", "newPassword": "..."}`. Validates token, hashes new password with BCrypt (strength 12), invalidates the reset token.
   - **Cluster-Wide Session Termination:** Revokes all user refresh tokens in MySQL and blacklists active user sessions cluster-wide in Redis (`tokenBlacklistService.blacklistUser(...)`).
   - Dispatches an asynchronous security notification email alerting the user of the password change.

### 54.4 Transactional Event Delivery & Redis Deduplication
1. **Transactional Outbox Timing (`afterCommit()`):**
   - Email events (`EmailEvent`) are registered with Spring's `TransactionSynchronizationManager` and dispatched **strictly after the MySQL transaction has committed**.
   - If user creation or password update fails and rolls back, zero phantom emails are sent.
2. **Distributed Redis Idempotency:**
   - `EmailEventListener` uses Redis atomic keys (`idempotency:event:email:<eventId>`) with a 15-minute lease and 24-hour retention.
   - Prevents duplicate email dispatches across concurrent Spring in-memory listeners and multi-node Kafka consumers.

### 54.5 Configuration & Environment Variables
| Variable | Default | Description |
|---|---|---|
| `MAIL_HOST` | `localhost` | SMTP mail server hostname |
| `MAIL_PORT` | `587` | SMTP port (587 for TLS, 465 for SSL) |
| `MAIL_USERNAME` | `""` | SMTP provider authentication username |
| `MAIL_PASSWORD` | `""` | SMTP provider authentication password |
| `MAIL_FROM` | `no-reply@instagram.com` | From email address |
| `MAIL_FROM_NAME` | `Instagram` | Sender display name |
| `MAIL_VERIFY_URL` | `http://localhost:5173/verify-email` | Client email verification landing URL |
| `MAIL_RESET_URL` | `http://localhost:5173/reset-password` | Client password reset landing URL |
| `MAIL_VERIFY_VALIDITY_MINS` | `1440` | Verification token TTL (24 hours) |
| `MAIL_RESET_VALIDITY_MINS` | `30` | Password reset token TTL (30 minutes) |
| `APP_MAIL_ENABLED` | `true` | Set to false to disable network SMTP sending (safe local/test mode) |

---

# 55. PURPOSE-BASED MULTI-CATEGORY EMAIL MANAGEMENT SYSTEM

### 55.1 Architectural Overview
The Instagram Clone email system is extended into an enterprise-grade, multi-category messaging platform. Each email type defines an explicit category, priority, mandatory vs optional policy, recipient privacy rules, deduplication/debouncing window, and retry strategy.

```text
  [Business Event: Auth / Follow / Engagement / Settings]
                           │
                           ▼
                 [EmailEventPublisher] (Strictly afterCommit())
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
 [Spring Event Bus]                [Kafka Broker]
           │                               │
           └───────────────┬───────────────┘
                           ▼
                  [EmailEventListener] (Redis Idempotency: 24h retention)
                           │
                           ▼
                  [EmailDispatcher] (Centralized Routing Engine)
                     │
                     ├─► 1. Validate Command & Resolve User
                     ├─► 2. User Notification Preferences (UserSettings / Cache)
                     ├─► 3. Anti-Spam Debouncing (Redis Key: debounce:mail:*)
                     ├─► 4. HMAC-SHA256 Unsubscribe Token Generation
                     ├─► 5. Responsive HTML / Text Rendering (XSS-Safe HtmlUtils)
                     ├─► 6. Sender Identity Separation (MailProperties)
                     ├─► 7. Audit Log Persistence (email_delivery_logs)
                     └─► 8. Provider Execution & Bounded Retries (EmailProvider)
```

### 55.2 Strongly Typed Category & Policy Model
Every email belongs to a designated category with purpose-specific policies:
1. **ACCOUNT & SECURITY (Mandatory):** Security-critical account emails (`EMAIL_VERIFICATION`, `PASSWORD_RESET`, `PASSWORD_CHANGED`, `NEW_LOGIN`, `SECURITY_SETTINGS_CHANGED`).
   - `mandatory = true`: Bypasses optional marketing or activity email preferences.
   - Unsubscribe links are suppressed.
   - Attempts to unsubscribe via the API return HTTP 400 Bad Request.
2. **SOCIAL ACTIVITY (Optional):** Follow and engagement notifications (`NEW_FOLLOWER`, `POST_LIKED`, `POST_COMMENTED`).
   - Governed by user preferences (`email_social_follows`, `email_social_likes`, `email_social_comments`).
   - Subject to Redis debouncing (30-minute window) to prevent spam from repetitive likes/unlikes or rapid comments.
3. **MESSAGING (Optional):** Direct message reminders (`UNREAD_MESSAGE_DIGEST`).
   - Governed by `email_message_reminders`.
4. **DIGEST & PRODUCT (Optional):** Product updates and periodic digests (`PRODUCT_ANNOUNCEMENT`, `DAILY_ACTIVITY_DIGEST`, `WEEKLY_ACTIVITY_DIGEST`).
   - Opt-in by default for promotional emails (`email_product_announcements`, `email_activity_digest`).
   - Configurable digest frequency (`WEEKLY` or `DAILY`).

### 55.3 Sender Identity Segregation
Sender headers are cleanly separated by business purpose using a single provider without requiring separate SMTP credentials:
- **Security:** `security@instagram.com` (*Instagram Security*)
- **Account:** `no-reply@instagram.com` (*Instagram*)
- **Activity:** `activity@instagram.com` (*Instagram*)
- **Messaging:** `messages@instagram.com` (*Instagram Direct*)
- **Updates:** `updates@instagram.com` (*Instagram Updates*)

### 55.4 Delivery Lifecycle State Model
Every dispatch attempt is recorded in MySQL `email_delivery_logs`:
- `QUEUED`: Staged for event processing.
- `PROCESSING`: Validating variables and rendering templates.
- `SENT_TO_PROVIDER`: Successfully accepted by SMTP / mail provider.
- `DELIVERED_SIMULATED`: Delivery simulated in local/test/offline mode without provider error.
- `SKIPPED_PREFERENCE`: Skipped because the recipient opted out of this category in user settings.
- `SKIPPED_THROTTLED`: Skipped because an identical action was received within the Redis debouncing window.
- `RETRYING`: Transient provider failure; scheduled for bounded retry with backoff.
- `FAILED`: Max retries exhausted or fatal provider failure recorded.

### 55.5 Cryptographic Unsubscribe Links & Preference Endpoints
1. **Unauthenticated Unsubscribe:**
   - Link contains `userId`, `emailType`, `expires`, and an `HMAC-SHA256` signature computed over `userId:emailType:expires`.
   - Verified in constant time via `MessageDigest.isEqual(...)` to prevent timing attacks.
   - Endpoint: `POST /api/v1/auth/unsubscribe` (Rate-limited: 10 req/min).
2. **Authenticated Preferences:**
   - `GET /api/v1/users/me/email-preferences`: Returns granular email toggles for the authenticated user.
   - `PUT /api/v1/users/me/email-preferences`: Updates email toggles and invalidates the Redis user preferences cache.

### 55.6 Flyway Migration V6 Schema
- `ALTER TABLE user_settings`: Added `email_social_follows`, `email_social_likes`, `email_social_comments`, `email_message_reminders`, `email_activity_digest`, `email_product_announcements`, `digest_frequency`.
- `CREATE TABLE email_delivery_logs`: Persistent audit ledger tracking `id`, `recipient_email`, `user_id`, `email_type`, `category`, `subject`, `status`, `attempts`, `last_error`, `sent_at`, and `created_at`.

---

**END OF BACKEND MASTER README**