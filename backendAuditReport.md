# Instagram Clone — Backend Production-Readiness Audit Report

**Date:** October 12, 2026  
**Auditor:** Senior Java Backend Architect & Production-Readiness Engineer  
**Scope:** Instagram Clone Spring Boot 3.3.4 / Java 21 Backend  
**Status:** AUDIT COMPLETE — REMEDIATION ROADMAP ESTABLISHED  

---

## 1. Executive Summary

A comprehensive architectural and security audit was conducted on the Instagram Clone backend repository (`backend/`). The backend is structured as a modular monolith adhering to Spring Boot 3.3.4, Java 21 LTS, Spring Security 6, Spring Data JPA/Hibernate 6, MySQL 8 with Flyway migrations (`V1` to `V4`), Redis caching/rate-limiting, Apache Kafka asynchronous domain events, STOMP WebSocket real-time messaging, and Cloudinary media delivery.

### Overall Readiness Score: **78 / 100**
* **Core Business Logic & Domain Models:** **92%** complete and operational.
* **Database Schema & Migrations:** **90%** compliant with UUID `BINARY(16)` and indexing standards.
* **Security & Authorization:** **70%** (critical gaps identified in WebSocket subscription BOLA, stateless JWT access-token revocation, and session invalidation on password change).
* **Reliability & Background Jobs:** **85%** (ephemeral story cleanup and token purges present).
* **Observability & Operational Hardening:** **65%** (missing correlation IDs in HTTP filter chain and structured MDC propagation).
* **Automated Test Suite:** **43 passing tests** (missing WebSocket authorization, token revocation, block privacy, and constraint violation tests).

---

## 2. Completed Features Inventory

| Feature / Domain | Main Classes & Components | API Endpoints / Contracts | Persistence / Infra |
| :--- | :--- | :--- | :--- |
| **Authentication & Identity** | `AuthController`, `AuthServiceImpl`, `JwtTokenProvider`, `JwtAuthenticationFilter` | `POST /api/v1/auth/register`<br>`POST /api/v1/auth/login`<br>`POST /api/v1/auth/refresh`<br>`POST /api/v1/auth/logout` | `users`, `profiles`, `refresh_tokens`, BCrypt (cost 12), SHA-256 token hashing |
| **User Profile & Settings** | `UserController`, `SettingsController`, `UserServiceImpl`, `SettingsServiceImpl` | `GET /api/v1/users/me`<br>`GET /api/v1/users/{userId}`<br>`PUT /api/v1/users/profile`<br>`PUT /api/v1/users/avatar`<br>`PUT /api/v1/settings/password`<br>`POST/DELETE /api/v1/settings/blocks/{targetUserId}` | `user_settings`, `user_blocks`, `profiles` |
| **Post & Carousel Media** | `PostController`, `PostServiceImpl`, `MediaServiceImpl` | `POST /api/v1/posts`<br>`GET /api/v1/posts/{postId}`<br>`GET /api/v1/posts/user/{userId}`<br>`PUT /api/v1/posts/{postId}`<br>`DELETE /api/v1/posts/{postId}` | `posts`, `post_media`, `media` (Cloudinary partition `posts/{userId}`) |
| **Social Graph (Follows)** | `FollowController`, `FollowServiceImpl` | `POST /api/v1/users/{targetUserId}/follow`<br>`GET /api/v1/users/{targetUserId}/followers`<br>`GET /api/v1/users/{targetUserId}/following` | `follows` (unique composite index `uq_follows`) |
| **Engagement (Likes, Comments, Saves)** | `EngagementController`, `EngagementServiceImpl` | `POST /api/v1/posts/{postId}/likes`<br>`POST /api/v1/posts/{postId}/comments`<br>`GET /api/v1/posts/{postId}/comments`<br>`DELETE /api/v1/posts/comments/{commentId}`<br>`POST /api/v1/posts/{postId}/save` | `likes`, `comments`, `saved_posts` |
| **Feed Generation** | `FeedController`, `FeedServiceImpl` | `GET /api/v1/feed/timeline`<br>`GET /api/v1/feed/explore` | SQL subquery queries on following posts |
| **Ephemeral Stories** | `StoryController`, `StoryServiceImpl` | `POST /api/v1/stories`<br>`GET /api/v1/stories/feed`<br>`GET /api/v1/stories/user/{userId}`<br>`POST /api/v1/stories/{storyId}/view`<br>`GET /api/v1/stories/{storyId}/viewers`<br>`DELETE /api/v1/stories/{storyId}` | `stories`, `story_views`, 24h expiration, Cloudinary cleanup |
| **Reels (Short Video)** | `ReelController`, `ReelServiceImpl` | `POST /api/v1/reels`<br>`GET /api/v1/reels/feed`<br>`GET /api/v1/reels/user/{userId}`<br>`DELETE /api/v1/reels/{reelId}` | `reels`, video transformations, duration metadata |
| **Real-Time Direct Chat** | `ChatController`, `WebSocketChatController`, `ChatServiceImpl` | `POST /api/v1/conversations`<br>`GET /api/v1/conversations`<br>`GET /api/v1/conversations/{id}/messages`<br>`POST /api/v1/conversations/{id}/messages`<br>`STOMP /app/chat.send/{conversationId}` | `conversations`, `conversation_members`, `messages`, STOMP broker `/topic/conversations/{id}` |
| **Cloudinary Media Storage** | `CloudinaryService`, `MediaServiceImpl`, `MediaController` | `POST /api/v1/media/upload`<br>`DELETE /api/v1/media/{publicId}` | `media` table, compensating rollback deletions, retry backoff |
| **Rate Limiting** | `RedisRateLimiterService`, `RateLimitInterceptor`, `WebSocketRateLimitInterceptor` | Annotation `@RateLimit` on HTTP and STOMP endpoints | Redis Lua sliding/fixed-window script with in-memory fallback |
| **Audit Logs** | `AuditLogService`, `AuditLogRepository` | Internal asynchronous audit logging | `audit_logs` table (V3 migration) |
| **Scheduled Jobs** | `ScheduledCleanupJobs` | Cron-driven jobs: story expiration, refresh token purge | `@Scheduled` Spring tasks |

---

## 3. Defects & Security Vulnerabilities

### Finding 1: WebSocket Broken Object Level Authorization (BOLA/IDOR) on Conversation Subscriptions
* **Severity:** **HIGH**
* **Affected Component:** `com.instagramclone.security.WebSocketAuthInterceptor.java:89-111`
* **Vulnerability Description:** While `handleSubscribe()` validates destination `/topic/notifications/{userId}` against the authenticated user's ID, it performs **NO authorization check** on `/topic/conversations/{conversationId}`. Any authenticated user can subscribe to any conversation topic and intercept private direct messages between any other users.
* **Remediation:** Inject `ConversationMemberRepository` into `WebSocketAuthInterceptor`. Intercept `/topic/conversations/{conversationId}`, parse the UUID, and verify `conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userPrincipal.getId())`. Reject non-members with `MessageDeliveryException`.

### Finding 2: Stateless JWT Access-Token Non-Revocation on Logout
* **Severity:** **MEDIUM-HIGH**
* **Affected Components:** `com.instagramclone.user.service.AuthServiceImpl.java:171-188`, `com.instagramclone.security.JwtAuthenticationFilter.java:39`
* **Vulnerability Description:** When a user calls `POST /api/v1/auth/logout`, only refresh tokens in the database are revoked. The issued JWT access token remains valid for its remaining lifespan (up to 15 minutes). If an access token is compromised or used on a public computer, the session cannot be terminated immediately.
* **Remediation:** Implement a Redis-backed `TokenBlacklistService`. On logout, blacklist the current access token in Redis with a TTL matching its remaining validity (`blacklist:token:<sha256>`). In `JwtAuthenticationFilter` and `WebSocketAuthInterceptor`, verify `!tokenBlacklistService.isBlacklisted(token)`.

### Finding 3: Missing Session & Refresh Token Invalidation on Password Change
* **Severity:** **MEDIUM-HIGH**
* **Affected Component:** `com.instagramclone.user.service.SettingsServiceImpl.java:87-107`
* **Vulnerability Description:** In `changePassword()`, the user's password hash is updated, but existing refresh tokens are not revoked (`refreshTokenRepository.revokeAllUserTokens(currentUserId, LocalDateTime.now())` is never called), and no audit log is generated. An attacker possessing an active session maintains access indefinitely despite the password update.
* **Remediation:** Inject `RefreshTokenRepository` and `AuditLogService` into `SettingsServiceImpl`. Revoke all existing refresh tokens upon successful password change, blacklist active tokens for the user in Redis, and record an audit event.

### Finding 4: Missing Block Privacy Validation in Follows and Messaging
* **Severity:** **MEDIUM**
* **Affected Components:** `com.instagramclone.follow.service.FollowServiceImpl.java:38-77`, `com.instagramclone.chat.service.ChatServiceImpl.java:51-96, 133-176`
* **Vulnerability Description:** Users can block other users via `SettingsService`, but `FollowServiceImpl.followUser()` and `ChatServiceImpl` do not check `UserBlockRepository`. A blocked user can still follow their blocker or send direct messages to them.
* **Remediation:** Inject `UserBlockRepository` into `FollowServiceImpl` and `ChatServiceImpl`. Check `existsByBlockerIdAndBlockedId` in both directions and reject actions with `BadRequestException` ("Cannot interact with this user").

### Finding 5: Missing SecurityContext Propagation in STOMP Inbound Messages
* **Severity:** **MEDIUM**
* **Affected Components:** `com.instagramclone.security.WebSocketAuthInterceptor.java:36-53`, `com.instagramclone.chat.controller.WebSocketChatController.java:18-24`
* **Vulnerability Description:** When STOMP `@MessageMapping("/chat.send/{conversationId}")` executes, it calls `chatService.sendMessage()`, which expects `SecurityUtils.getCurrentUserId()` from `SecurityContextHolder`. In Spring STOMP worker threads, `SecurityContextHolder` is not set by default from `accessor.getUser()`.
* **Remediation:** In `WebSocketAuthInterceptor.preSend()`, if `accessor.getUser() instanceof Authentication auth`, set `SecurityContextHolder.getContext().setAuthentication(auth)`. In `afterSendCompletion()`, clear the security context to prevent thread-pool leakage.

---

## 4. Performance & Scalability Risks

### Finding 6: Severe N+1 Query Cascade in Feed Generation
* **Severity:** **HIGH**
* **Affected Components:** `com.instagramclone.feed.service.FeedServiceImpl.java:48-87`, `com.instagramclone.post.repository.PostRepository.java:28-34`
* **Risk Description:** In `getHomeFeed()` and `getExploreFeed()`, each retrieved `Post` triggers lazy queries for:
  1. `post.getMediaList()` (1 query per post)
  2. `post.getUser().getProfile()` (1 query per post)
  3. `engagementService.getLikeCount()` (1 query per post)
  4. `engagementService.getCommentCount()` (1 query per post)
  5. `engagementService.isLikedByUser()` (1 query per post)
  6. `engagementService.isSavedByUser()` (1 query per post)  
  *Total:* 1 + (20 × 6) = **121 database queries** per single feed page request.
* **Remediation:**
  1. Add `spring.jpa.properties.hibernate.default_batch_fetch_size: 30` to `application.yml` to batch-fetch lazy collections and proxies in single `IN (?, ?...)` queries.
  2. Utilize `@EntityGraph` or fetch joins on `PostRepository.findTimelinePosts` where possible.

### Finding 7: Lack of HikariCP Baseline in Default Configuration
* **Severity:** **LOW-MEDIUM**
* **Affected Component:** `backend/src/main/resources/application.yml` vs `application-dev.yml`
* **Risk Description:** Connection pool settings (`maximum-pool-size`, `minimum-idle`, `connection-timeout`, `max-lifetime`) are currently only declared in `application-dev.yml`. If launched under a different profile without overrides, Hikari falls back to defaults.
* **Remediation:** Define baseline Hikari connection pool configuration in base `application.yml`.

---

## 5. Duplicate, Inconsistent, or Fragile Architecture

### Finding 8: Duplicate Security Utilities (`SecurityUtility` vs `SecurityUtils`)
* **Affected Components:**
  - `com.instagramclone.utility.SecurityUtility.java` (Canonical in `utility`)
  - `com.instagramclone.common.util.SecurityUtils.java` (Legacy facade in `common.util`)
* **Status:** `SecurityUtils` cleanly delegates to `SecurityUtility`. Documented and safe for backward compatibility, but developers should standardize on `SecurityUtility`.

### Finding 9: Global Exception Handling Gaps for Data Integrity & Malformed Requests
* **Severity:** **MEDIUM**
* **Affected Component:** `com.instagramclone.common.exception.GlobalExceptionHandler.java`
* **Defect Description:** Unhandled `DataIntegrityViolationException` (e.g. unique constraint violation on like or follow), `HttpMessageNotReadableException` (malformed JSON), and `MethodArgumentTypeMismatchException` (malformed UUIDs) fall into `handleGeneralException(Exception ex)`, returning generic HTTP 500 errors instead of client-friendly HTTP 409 / HTTP 400 responses.
* **Remediation:** Add explicit `@ExceptionHandler` methods for `DataIntegrityViolationException` (HTTP 409 Conflict), `HttpMessageNotReadableException` (HTTP 400 Bad Request), `MethodArgumentTypeMismatchException` (HTTP 400 Bad Request), and `MaxUploadSizeExceededException` (HTTP 413 Payload Too Large).

---

## 6. Observability & Production Operation Gaps

### Finding 10: Missing Correlation ID & Distributed Tracing Filter
* **Severity:** **MEDIUM**
* **Defect Description:** Incoming HTTP requests do not receive or forward an `X-Correlation-ID` header. SLF4J MDC is not populated with a correlation identifier, preventing end-to-end request tracing across microservice logs.
* **Remediation:** Implement `CorrelationIdFilter` (`OncePerRequestFilter`), extract or generate `X-Correlation-ID`, attach to SLF4J `MDC.put("correlationId", ...)`, add to response headers, and update log patterns to output `[%X{correlationId}]`.

### Finding 11: Missing Cloudinary Health Indicator in Spring Actuator
* **Severity:** **LOW-MEDIUM**
* **Defect Description:** Actuator exposes `/actuator/health`, but does not verify whether Cloudinary credentials and connectivity are intact.
* **Remediation:** Implement `CloudinaryHealthIndicator` implementing `HealthIndicator`.

---

## 7. Action Plan & Implementation Roadmap

1. **Phase 2 — Security Hardening:**
   - Fix WebSocket conversation authorization in `WebSocketAuthInterceptor`.
   - Create Redis-backed `TokenBlacklistService`.
   - Integrate access token blacklist checking into `JwtAuthenticationFilter` and `WebSocketAuthInterceptor`.
   - Revoke refresh tokens and session blacklist in `SettingsServiceImpl.changePassword()`.
   - Enforce `UserBlockRepository` checks in `FollowServiceImpl` and `ChatServiceImpl`.
   - Propagate `SecurityContext` in `WebSocketAuthInterceptor`.

2. **Phase 3 — Data & Architectural Refinements:**
   - Add `DataIntegrityViolationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MaxUploadSizeExceededException` in `GlobalExceptionHandler`.
   - Enable Hibernate batch fetching (`hibernate.default_batch_fetch_size: 30`) in `application.yml` to resolve N+1 feed queries.
   - Consolidate HikariCP defaults in `application.yml`.

3. **Phase 4 & 5 — Redis, Kafka & Real-Time Enhancements:**
   - Verify Kafka producer idempotence and retry backoff.
   - Verify ScheduledCleanupJobs safety and Cloudinary orphan cleanup.

4. **Phase 6 — Reliability & Observability:**
   - Create `CorrelationIdFilter` and configure in Spring Security.
   - Update log pattern to format correlation ID.
   - Add `CloudinaryHealthIndicator` for Spring Boot Actuator.

5. **Phase 7 — Testing & Verification:**
   - Add automated unit and integration tests.
   - Run full Maven test suite (`mvn test`) and report verified build results.
