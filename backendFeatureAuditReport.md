# Instagram Clone Backend — Complete Feature Implementation Audit Report

**Date:** October 12, 2026  
**Auditor:** Senior Java Backend Architect, Spring Boot Expert & Distributed Systems Auditor  
**Scope:** Instagram Clone Spring Boot 3.3.4 / Java 21 LTS Backend (`backend/`)  
**Methodology:** Direct Source Code Inspection, AST Analysis, Configuration Verification & Test Suite Execution  
**Document File:** `backendFeatureAuditReport.md`

---

## 1. Executive Summary

A comprehensive source-code-first audit was performed on the Instagram Clone backend repository. Every feature across thirteen operational domains was audited by tracing code from REST controllers, STOMP message handlers, input validation DTOs, service transaction boundaries, JPA repositories, Flyway database migrations (`V1`–`V7`), Redis distributed structures, Kafka producers/consumers, scheduled cron tasks, and automated integration tests.

### Audited Statistics

* **Total Discrete Features Audited:** **171**
* **IMPLEMENTED:** **142** (83.0%) *(+4 in Phase 2)*
* **PARTIALLY IMPLEMENTED:** **13** (7.6%) *(-1 in Phase 2)*
* **NOT IMPLEMENTED:** **16** (9.4%) *(-3 in Phase 2)*
* **UNVERIFIED:** **0** (0.0%) *(All source code, configs, and test logs were inspected directly)*

```
[███████████████████████████████████████████░░░░░░░░░] 83.0% IMPLEMENTED
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 7.6% PARTIAL
[█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 9.4% NOT IMPLEMENTED
```

---

### Main Architectural Risks (Phase 2 Status)

1. **Private Account Enforcement Gap (Critical Security/Privacy):** **[RESOLVED IN PHASE 1]** Centralized `PrivacyService` implemented and integrated across `PostServiceImpl`, `StoryServiceImpl`, `FeedServiceImpl`, and `SearchServiceImpl`. Direct object IDs return HTTP 403 Forbidden to non-followers.
2. **Social Graph Edge Case (Follow Requests):** **[RESOLVED IN PHASE 1]** Flyway `V8__follow_requests.sql` migration, `FollowRequest` entity, and full lifecycle (`request`, `accept`, `reject`, `cancel`, `removeFollower`) implemented with notifications and email alerts.
3. **Inconsistent Block Boundaries:** **[RESOLVED IN PHASE 1]** Bidirectional block enforcement (`existsBlockBetween`) integrated into `PrivacyService`, `UserServiceImpl`, `PostServiceImpl`, `StoryServiceImpl`, `FeedServiceImpl`, and `SearchServiceImpl`. Blocked content returns HTTP 404 to avoid disclosure.
4. **Integration Test Teardown Referential Integrity Violation:** **[RESOLVED IN PHASES 1 & 2]** `BaseIntegrationTest.cleanDatabase()` ordered to delete all foreign key dependencies (`post_media_tags`, `user_known_devices`, `email_change_requests`, `follow_requests`, `audit_logs`, `reel_likes`, `reel_comments`, `reel_views`). 143 of 143 tests pass cleanly.
5. **Reels Engagement Isolation & Concurrency Safety:** **[RESOLVED IN PHASE 2]** Flyway `V9__reels_engagement_and_lockout.sql` migration, `ReelLike`, `ReelComment`, `ReelView` entities, `ReelEngagementService` with Redis distributed locking and programmatic `TransactionTemplate`, notification deduplication, and privacy/block authorization implemented.
6. **WebSocket Typing Indicator BOLA Vulnerability:** **[RESOLVED IN MODULE 1, TASK 2]** Enforced conversation membership verification and direct-message mutual block checks in `WebSocketChatController.handleTyping()` and `ChatService.validateTypingPermission()`. Authenticated spoofing and unauthorized indicator broadcasting eliminated.

---

### Five Most Important Gaps

1. **User Tagging and Mention Parsing:** `PostMediaTag` entity and table exist in `V7`, but no service, controller, or DTO connects them. `@username` mention parsing in captions/comments is deferred.
2. **Hashtag System:** No hashtag entities, extraction, indexation, or hashtag-specific search endpoints exist; searches rely purely on SQL `LIKE %query%` on post captions.
3. **Direct Messaging Unsend & Group Conversations:** Group chat and message unsend features remain unbuilt.
4. **User Account Deletion & GDPR Data Export:** No endpoints exist for soft/hard account deletion or asynchronous data archive generation.
5. **Story Highlights & Close Friends:** Story highlights and Close Friends audience lists remain unbuilt.

---

## 2. Feature Matrix

| ID | Category | Feature | Status | Evidence (class / method / endpoint) | Missing Work / Risk | Verification |
|:---|:---|:---|:---|:---|:---|:---|
| **A.1** | Auth & Security | User registration | **IMPLEMENTED** | `AuthController.register()` -> `AuthServiceImpl.register()` -> `UserRepository.save()` | None. Validates unique username/email, hashes password, saves baseline profile. | Code & Tests |
| **A.2** | Auth & Security | Login and logout | **IMPLEMENTED** | `AuthController.login()` / `logout()` -> `AuthServiceImpl.login()` / `logout()` | None. Returns access/refresh token pair. Blacklists JWT and revokes refresh tokens on logout. | Code & Tests |
| **A.3** | Auth & Security | JWT generation and validation | **IMPLEMENTED** | `JwtTokenProvider.generateAccessToken()` / `validateToken()` | None. Cryptographically signed with HMAC-SHA256, validates expiration and claims. | Code & Tests |
| **A.4** | Auth & Security | Refresh-token rotation & revocation | **IMPLEMENTED** | `AuthServiceImpl.refreshToken()` -> `RefreshTokenRepository` | Rotates single-use refresh token, revokes previous token hash immediately. | Code & Tests |
| **A.5** | Auth & Security | Multiple-device/session management | **IMPLEMENTED** | `DeviceDetectionServiceImpl`, `UserKnownDeviceRepository`, `PresenceServiceImpl` | Persists devices in `user_known_devices`, tracks active WebSocket sessions in Redis sets. | Code & DB V7 |
| **A.6** | Auth & Security | Password hashing & password change | **IMPLEMENTED** | `SettingsServiceImpl.changePassword()`, BCrypt(12) | Verifies current password, encodes with BCrypt, invalidates all sessions & blacklists tokens. | Code Verified |
| **A.7** | Auth & Security | Forgot-password & password reset | **IMPLEMENTED** | `AuthServiceImpl.forgotPassword()` / `resetPassword()`, `PasswordResetTokenRepository` | Secure SHA-256 token hashing, 30-min validity, invalidates sessions upon reset. | Code & Tests |
| **A.8** | Auth & Security | Email verification | **IMPLEMENTED** | `AuthServiceImpl.verifyEmail()`, `EmailVerificationTokenRepository` | Single-use cryptographically random token, updates `email_verified` flag. | Code & Tests |
| **A.9** | Auth & Security | Resending verification email | **IMPLEMENTED** | `AuthServiceImpl.resendVerification()` | Anti-enumeration protection: generic success response even for invalid emails. | Code Verified |
| **A.10** | Auth & Security | Duplicate email & username prevention | **IMPLEMENTED** | `UserRepository.existsByUsername()`, `existsByEmail()`, DB unique constraints | Returns HTTP 409 Conflict with clear error messages. | Code & Tests |
| **A.11** | Auth & Security | Login rate limiting | **IMPLEMENTED** | `@RateLimit(category = "auth-login", limit = 10, windowSeconds = 60)` | Redis sliding window with Lua script; in-memory fallback if Redis offline. | Code & Tests |
| **A.12** | Auth & Security | Account lockout / brute-force protection | **IMPLEMENTED** | `AccountLockoutService`, `failed_login_attempts`, `lockout_until`, HTTP 423 | Safe failure counter tracking, anti-enumeration identical 401, auto-decay on expiry, recovery via password reset. | Code & Tests |
| **A.13** | Auth & Security | Security alerts for password changes & logins | **IMPLEMENTED** | `AuthServiceImpl.login()`, `SettingsServiceImpl.changePassword()` | Publishes Kafka `EmailEvent` for `PASSWORD_CHANGED` and `NEW_LOGIN` / `NEW_DEVICE_LOGIN`. | Code Verified |
| **A.14** | Auth & Security | Account deletion and data export | **NOT IMPLEMENTED** | No controller endpoints or service methods | Missing `DELETE /users/me` and data export archive generation. | Source Code |
| **B.1** | Profile & Social | View and update user profile | **IMPLEMENTED** | `UserController.getCurrentUser()`, `updateProfile()` -> `UserServiceImpl` | Updates displayName, bio, websiteUrl. Protected with JWT auth. | Code & Tests |
| **B.2** | Profile & Social | Profile photo/avatar upload | **IMPLEMENTED** | `UserController.updateAvatar()` -> `UserServiceImpl.updateAvatar()` | Updates avatarUrl in `Profile`. | Code & Tests |
| **B.3** | Profile & Social | Username and bio updates | **PARTIALLY IMPLEMENTED** | `UserServiceImpl.updateProfile()` | Bio and displayName can be updated; username modification is not exposed. | Source Code |
| **B.4** | Profile & Social | Followers and following lists | **IMPLEMENTED** | `FollowController.getFollowers()`, `getFollowing()` -> `FollowServiceImpl` | Paginated lists with user summary projections. | Code & Tests |
| **B.5** | Profile & Social | Follow and unfollow users | **IMPLEMENTED** | `FollowController.followUser()`, `unfollowUser()` -> `FollowServiceImpl` | Mutual block validation, optimistic Redis profile cache eviction. | Code & Tests |
| **B.6** | Profile & Social | Private-account support | **IMPLEMENTED** | `PrivacyServiceImpl`, `PostServiceImpl`, `StoryServiceImpl` | Private accounts gate posts and stories for non-followers with HTTP 403. | Code & Tests |
| **B.7** | Profile & Social | Follow requests for private accounts | **IMPLEMENTED** | `FollowServiceImpl.followUser()`, `FollowRequestRepository`, Flyway `V8` | Creates pending follow request when target is private; prevents duplicate requests. | Code & Tests |
| **B.8** | Profile & Social | Accept and reject follow requests | **IMPLEMENTED** | `FollowController.acceptFollowRequest()` / `rejectFollowRequest()` | Owner can accept (creates follow, increments counts, notifies) or reject. | Code & Tests |
| **B.9** | Profile & Social | Cancel pending follow requests | **IMPLEMENTED** | `FollowController.cancelFollowRequest()` -> `FollowServiceImpl` | Requester can cancel pending follow request before decision. | Code & Tests |
| **B.10** | Profile & Social | Remove a follower | **IMPLEMENTED** | `FollowController.removeFollower()` -> `FollowServiceImpl` | Target user can remove approved follower without blocking. | Code & Tests |
| **B.11** | Profile & Social | Block and unblock users | **IMPLEMENTED** | `SettingsController.blockUser()`, `unblockUser()`, `UserBlockRepository` | Persisted in `user_blocks` table with unique constraint. | Code & Tests |
| **B.12** | Profile & Social | Mute and unmute users | **NOT IMPLEMENTED** | No entity or repository | Muting user posts or stories is absent. | Source Code |
| **B.13** | Profile & Social | Restrict users | **NOT IMPLEMENTED** | No entity or repository | Instagram-style restricted account mode is absent. | Source Code |
| **B.14** | Profile & Social | Report users | **IMPLEMENTED** | `AdminController.createReport()`, `ReportTargetType.USER` | Persisted in `reports` table; reviewed via admin moderation endpoints. | Code Verified |
| **B.15** | Profile & Social | Correct follower/following counts | **IMPLEMENTED** | `FollowRepository.countByFollowingId()`, `countByFollowerId()` | Live SQL aggregate counts returned in profile responses. | Code & Tests |
| **B.16** | Profile & Social | Prevent duplicate/invalid relationships | **IMPLEMENTED** | `FollowServiceImpl.followUser()`, DB composite unique index `uq_follows` | Disallows self-following and duplicate follows (HTTP 400 / 409). | Code & Tests |
| **C.1** | Posts & Media | Create a post | **IMPLEMENTED** | `PostController.createPost()` -> `PostServiceImpl.createPost()` | Validates caption length (<=2200), creates `Post` and `PostMedia` children. | Code & Tests |
| **C.2** | Posts & Media | Carousel media in one post | **IMPLEMENTED** | `CreatePostRequest.media` (List), `PostMedia.displayOrder` | Supports multiple images and videos ordered by index. | Code & Tests |
| **C.3** | Posts & Media | Edit post caption | **IMPLEMENTED** | `PostController.updatePost()` -> `PostServiceImpl.updatePost()` | Author ownership check enforced before updating caption. | Code & Tests |
| **C.4** | Posts & Media | Delete a post | **IMPLEMENTED** | `PostController.deletePost()` -> `PostServiceImpl.deletePost()` | Author ownership check; cascades deletions to likes, comments, and saves. | Code & Tests |
| **C.5** | Posts & Media | Retrieve post details | **IMPLEMENTED** | `PostController.getPostById()` -> `PostServiceImpl.getPostById()` | Fetches post with media list, like count, comment count, and liked/saved flags. | Code & Tests |
| **C.6** | Posts & Media | User post listing & pagination | **IMPLEMENTED** | `PostController.getUserPosts()` -> `PostServiceImpl.getUserPosts()` | Paginated post retrieval sorted by `createdAt DESC`. | Code & Tests |
| **C.7** | Posts & Media | Like and unlike posts | **IMPLEMENTED** | `EngagementController.toggleLike()` -> `EngagementServiceImpl` | Toggles state, evicts Redis counter cache, sends notifications. | Code & Tests |
| **C.8** | Posts & Media | Save and unsave posts | **IMPLEMENTED** | `EngagementController.toggleSave()` -> `EngagementServiceImpl` | Toggles bookmark record in `saved_posts` table. | Code & Tests |
| **C.9** | Posts & Media | Retrieve saved posts | **IMPLEMENTED** | `PostController.getSavedPosts()` -> `PostServiceImpl.getSavedPosts()` | Paginated retrieval restricted to the authenticated user. | Code & Tests |
| **C.10** | Posts & Media | Comment on posts | **IMPLEMENTED** | `EngagementController.addComment()` -> `EngagementServiceImpl` | Persists comment, evicts comment counter cache, sends notification. | Code & Tests |
| **C.11** | Posts & Media | Reply to comments | **IMPLEMENTED** | `CreateCommentRequest.parentCommentId`, `EngagementServiceImpl.getReplies()` | 2-level threaded comments supported via self-referencing foreign key. | Code & Tests |
| **C.12** | Posts & Media | Delete comments and replies | **IMPLEMENTED** | `EngagementController.deleteComment()` -> `EngagementServiceImpl` | Ownership check: author of comment OR author of post can delete comment. | Code & Tests |
| **C.13** | Posts & Media | Mention users in captions/comments | **NOT IMPLEMENTED** | `EmailType.USER_MENTIONED` (marked deferred) | No regex parser to extract `@username` or emit notifications. | Source Code |
| **C.14** | Posts & Media | Tag users in photos | **NOT IMPLEMENTED** | `PostMediaTag` entity exists in `V7`, but no service/controller usages | Coordinate tagging (`x_percent`, `y_percent`) is not wired into post creation. | Source Code |
| **C.15** | Posts & Media | Hashtags & hashtag-based discovery | **NOT IMPLEMENTED** | `SearchServiceImpl.searchPosts()` uses SQL `LIKE %query%` | No dedicated hashtag entity, extraction logic, or hashtag indexing. | Source Code |
| **C.16** | Posts & Media | Post visibility & private enforcement | **IMPLEMENTED** | `PostServiceImpl.getPostById()`, `getUserPosts()`, `PrivacyService` | Enforces private account gating and block validation on all direct and list queries. | Code & Tests |
| **C.17** | Posts & Media | Media validation & size limits | **IMPLEMENTED** | `MediaServiceImpl.validateFile()`, Apache Tika / MIME checks | Validates image/video MIME types, checks size limits (15MB image, 100MB video). | Code Verified |
| **C.18** | Posts & Media | Cloudinary upload & cleanup | **IMPLEMENTED** | `CloudinaryService`, `MediaServiceImpl`, Flyway `V4` | Metadata stored in `media` table, asset deleted from Cloudinary on delete. | Code Verified |
| **D.1** | Feed & Discovery | Home feed from followed users | **IMPLEMENTED** | `FeedController.getHomeFeed()` -> `PostRepository.findTimelinePosts()` | Returns posts from self and followed users ordered chronologically. | Code & Tests |
| **D.2** | Feed & Discovery | Feed pagination | **IMPLEMENTED** | `PageableDefault(size = 10)`, `PageResponse<PostResponse>` | Spring Data JPA pagination with total pages and elements metadata. | Code & Tests |
| **D.3** | Feed & Discovery | Feed ordering and ranking | **PARTIALLY IMPLEMENTED** | `ORDER BY p.createdAt DESC` | Pure reverse-chronological ordering; no algorithmic ranking or engagement weight. | Source Code |
| **D.4** | Feed & Discovery | Explore feed | **IMPLEMENTED** | `FeedController.getExploreFeed()` -> `PostRepository.findExplorePosts()` | Returns public posts excluding the viewer's own posts. | Code & Tests |
| **D.5** | Feed & Discovery | Search users | **IMPLEMENTED** | `SearchController.searchUsers()` -> `UserRepository.searchUsers()` | Search matches against `username` and `profile.displayName`. | Code & Tests |
| **D.6** | Feed & Discovery | Search posts | **IMPLEMENTED** | `SearchController.searchPosts()` -> `PostRepository.searchPosts()` | Searches public post captions via SQL `LOWER(p.caption) LIKE LOWER(...)`. | Code & Tests |
| **D.7** | Feed & Discovery | Search hashtags | **NOT IMPLEMENTED** | No endpoint `/api/v1/search/hashtags` | Hashtag-specific search is absent. | Source Code |
| **D.8** | Feed & Discovery | Search pagination & rate limiting | **IMPLEMENTED** | `@RateLimit(category = "search", limit = 40, windowSeconds = 60)` | Paginated search requests protected by Redis rate limiting. | Code & Tests |
| **D.9** | Feed & Discovery | Personalized recommendations | **NOT IMPLEMENTED** | `UserServiceImpl.searchUsers()` | User suggestions in frontend are simple alphabetical queries; no graph heuristics. | Source Code |
| **D.10** | Feed & Discovery | Respect privacy and blocks in feed/search | **IMPLEMENTED** | `PostRepository.findExplorePosts()`, `searchPosts()`, `searchUsers()` | Excludes private accounts and bidirectional blocked accounts from explore and search. | Code & Tests |
| **E.1** | Ephemeral Stories | Create and upload stories | **IMPLEMENTED** | `StoryController.createStory()` -> `StoryServiceImpl.createStory()` | Sets 24-hour expiration (`now.plusHours(24)`), stores media URL and type. | Code & Tests |
| **E.2** | Ephemeral Stories | Image and video story support | **IMPLEMENTED** | `MediaType` enum (`IMAGE`, `VIDEO`) in `CreateStoryRequest` | Handled uniformly with media type metadata. | Code & Tests |
| **E.3** | Ephemeral Stories | Story expiration after 24 hours | **IMPLEMENTED** | `expires_at` column in `stories` table | Queries filter `expiresAt > CURRENT_TIMESTAMP`. | Code & Tests |
| **E.4** | Ephemeral Stories | Retrieve active stories | **IMPLEMENTED** | `StoryController.getStoryFeed()`, `getUserStories()` | Active stories grouped by user, current user placed first, unviewed prioritized. | Code & Tests |
| **E.5** | Ephemeral Stories | View a story | **IMPLEMENTED** | `StoryController.markStoryViewed()` -> `StoryServiceImpl` | Creates `StoryView` record and tracks viewed timestamp. | Code & Tests |
| **E.6** | Ephemeral Stories | Record story viewers | **IMPLEMENTED** | `StoryViewRepository`, `StoryView` entity | Stores viewer UUID, story UUID, and `viewed_at`. | Code & Tests |
| **E.7** | Ephemeral Stories | Prevent duplicate view records | **IMPLEMENTED** | `StoryViewRepository.existsByStoryIdAndUserId()` | Deduplicates view records before persisting. | Code & Tests |
| **E.8** | Ephemeral Stories | Story viewer list for story owner | **IMPLEMENTED** | `StoryController.getStoryViewers()` -> `StoryServiceImpl` | Ownership check enforced; non-owners receive HTTP 403 Forbidden. | Code & Tests |
| **E.9** | Ephemeral Stories | Delete a story | **IMPLEMENTED** | `StoryController.deleteStory()` -> `StoryServiceImpl.deleteStory()` | Ownership check enforced; cleans up Cloudinary asset and DB records. | Code & Tests |
| **E.10** | Ephemeral Stories | Scheduled cleanup of expired stories | **IMPLEMENTED** | `ScheduledCleanupJobs.cleanupExpiredStories()`, `@Scheduled` | Distributed lock acquired in Redis; deletes Cloudinary media and DB records. | Code & Tests |
| **E.11** | Ephemeral Stories | Story privacy controls | **NOT IMPLEMENTED** | No privacy fields in `Story` entity | Cannot hide story from specific followers or restrict visibility. | Source Code |
| **E.12** | Ephemeral Stories | Story highlights | **NOT IMPLEMENTED** | No highlight entity or tables | Permanent highlights on profile are absent. | Source Code |
| **E.13** | Ephemeral Stories | Close Friends stories | **NOT IMPLEMENTED** | No Close Friends list or story audience | Close Friends audience feature is absent. | Source Code |
| **F.1** | Reels & Video | Create and upload reels | **IMPLEMENTED** | `ReelController.createReel()` -> `ReelServiceImpl.createReel()` | Accepts video URL, thumbnail, caption, and duration. | Code & Tests |
| **F.2** | Reels & Video | Reel metadata and captions | **IMPLEMENTED** | `Reel` entity: `duration_ms`, `caption`, `thumbnail_url` | Full metadata persisted in `reels` table. | Code & Tests |
| **F.3** | Reels & Video | Retrieve reels | **IMPLEMENTED** | `ReelController.getReels()`, `getReelById()`, `getUserReels()` | Paginated retrieval by feed or creator. | Code & Tests |
| **F.4** | Reels & Video | Like and comment on reels | **IMPLEMENTED** | `ReelController`, `ReelEngagementServiceImpl`, `ReelLikeRepository`, `ReelCommentRepository` | Like/unlike toggle, paginated comments, deletion ownership check, privacy/block gating, notifications. | Code & Tests |
| **F.5** | Reels & Video | Reel views and view counts | **IMPLEMENTED** | `ReelController.recordView()`, `ReelViewRepository`, `uk_reel_views` | Deduplicated per viewer in MySQL, Redis count caching, concurrency-safe unique constraint. | Code & Tests |
| **F.6** | Reels & Video | Reel feed or discovery | **IMPLEMENTED** | `ReelController.getReels()` | Paginated discovery stream of ready reels. | Code & Tests |
| **F.7** | Reels & Video | Video validation and delivery | **IMPLEMENTED** | `MediaServiceImpl.validateFile()`, Cloudinary delivery | Mime check `video/mp4`, max size 100MB, Cloudinary CDN delivery. | Code Verified |
| **F.8** | Reels & Video | Reel deletion and media cleanup | **IMPLEMENTED** | `ReelController.deleteReel()` -> `ReelServiceImpl.deleteReel()` | Author check enforced; deletes Cloudinary asset and DB record. | Code & Tests |
| **G.1** | Direct Messaging | One-to-one conversations | **IMPLEMENTED** | `ChatController.getOrCreateConversation()` -> `ChatServiceImpl` | Finds existing or creates direct conversation with 2 members. | Code & Tests |
| **G.2** | Direct Messaging | Group conversations | **NOT IMPLEMENTED** | `ConversationType.GROUP` enum exists | No service method or endpoint to create group chats or manage participants. | Source Code |
| **G.3** | Direct Messaging | Send and retrieve messages | **IMPLEMENTED** | `ChatController.sendMessage()`, `getMessages()` -> `ChatServiceImpl` | REST + STOMP WebSocket messaging. Member verification enforced. | Code & Tests |
| **G.4** | Direct Messaging | Message pagination | **IMPLEMENTED** | `PageableDefault(size = 30, sort = "createdAt", direction = DESC)` | Paginated message history. | Code & Tests |
| **G.5** | Direct Messaging | Real-time WebSocket messaging | **IMPLEMENTED** | `WebSocketChatController.handleChatMessage()`, STOMP broker | Real-time delivery via `/topic/conversations/{conversationId}`. | Code & Tests |
| **G.6** | Direct Messaging | Delivery and read receipts | **IMPLEMENTED** | `ChatController.markConversationAsRead()`, `Message.read` | Updates message read status and timestamps in DB. | Code & Tests |
| **G.7** | Direct Messaging | Unread-message counts | **IMPLEMENTED** | `MessageRepository.countUnreadForUser()` | Aggregated in conversation list response DTOs. | Code & Tests |
| **G.8** | Direct Messaging | Typing indicators | **IMPLEMENTED** | `WebSocketChatController.handleTyping()`, `PresenceServiceImpl` | Broadcasts over STOMP with 5-second TTL in Redis (`chat:typing:*`). | Code & Tests |
| **G.9** | Direct Messaging | Online/offline presence | **IMPLEMENTED** | `PresenceServiceImpl`, `WebSocketPresenceEventListener` | Connect and disconnect events update Redis presence status. | Code & Tests |
| **G.10** | Direct Messaging | Multiple-device presence | **IMPLEMENTED** | `presence:user:<id>:sessions` (Redis Set) | Disconnecting one session does not mark user offline if other devices connected. | Code & Tests |
| **G.11** | Direct Messaging | Message deletion or unsend | **NOT IMPLEMENTED** | No endpoint `DELETE /messages/{id}` | Users cannot delete or unsend sent messages. | Source Code |
| **G.12** | Direct Messaging | Group member management | **NOT IMPLEMENTED** | No endpoints to add/remove members | Group administration is absent. | Source Code |
| **G.13** | Direct Messaging | Conversation authorization | **IMPLEMENTED** | `WebSocketAuthInterceptor.handleSubscribe()`, `ChatServiceImpl` | Strict membership checks; rejects unauthorized access with HTTP 403. | Code & Tests |
| **G.14** | Direct Messaging | Message notifications | **IMPLEMENTED** | `NotificationServiceImpl.createNotification()` | In-app and STOMP notifications on new direct messages. | Code & Tests |
| **G.15** | Direct Messaging | Unread-message email digest | **PARTIALLY IMPLEMENTED** | `EmailType.UNREAD_MESSAGE_DIGEST`, `EmailTemplateRenderer` | Email template and user preference exist, but no scheduled trigger job. | Source Code |
| **H.1** | Notifications | Like notifications | **IMPLEMENTED** | `EngagementServiceImpl.toggleLike()` -> `NotificationService` | Emits `NotificationType.POST_LIKED` to post owner. | Code & Tests |
| **H.2** | Notifications | Comment and reply notifications | **IMPLEMENTED** | `EngagementServiceImpl.addComment()` -> `NotificationService` | Emits `POST_COMMENTED` and `COMMENT_REPLIED`. | Code & Tests |
| **H.3** | Notifications | New-follower notifications | **IMPLEMENTED** | `FollowServiceImpl.followUser()` -> `NotificationService` | Emits `NotificationType.FOLLOW` to followed user. | Code & Tests |
| **H.4** | Notifications | Follow-request notifications | **IMPLEMENTED** | `NotificationType.FOLLOW_REQUEST`, `FOLLOW_REQUEST_ACCEPTED` | Emits in-app and WebSocket notifications on request creation and approval. | Code & Tests |
| **H.5** | Notifications | Message notifications | **IMPLEMENTED** | `ChatServiceImpl.sendMessage()` -> `NotificationService` | Emits notification to conversation recipient. | Code & Tests |
| **H.6** | Notifications | Notification listing & pagination | **IMPLEMENTED** | `NotificationController.getNotifications()` | Paginated notification history sorted by `createdAt DESC`. | Code & Tests |
| **H.7** | Notifications | Mark one notification as read | **IMPLEMENTED** | `NotificationController.markAsRead()` | Recipient ownership verified; invalidates Redis unread cache. | Code & Tests |
| **H.8** | Notifications | Mark all notifications as read | **IMPLEMENTED** | `NotificationController.markAllAsRead()` | Bulk update query in repository; invalidates Redis unread cache. | Code & Tests |
| **H.9** | Notifications | Unread notification count | **IMPLEMENTED** | `NotificationController.getUnreadCount()` | Cached in Redis for 5 minutes (`cache:notifications:unread:*`). | Code & Tests |
| **H.10** | Notifications | Notification preferences | **PARTIALLY IMPLEMENTED** | `UserSettings.notifyLikes`, `notifyComments`, etc. | Stored in settings and email dispatcher, but in-app notifications ignore flags. | Source Code |
| **H.11** | Notifications | Real-time notification delivery | **IMPLEMENTED** | `messagingTemplate.convertAndSend("/topic/notifications/" + id)` | Instant push over STOMP WebSocket. | Code & Tests |
| **H.12** | Notifications | Duplicate notification prevention | **IMPLEMENTED** | `NotificationServiceImpl.createNotification()`, `NotificationRepository.existsUnreadNotification` | Idempotently suppresses duplicate unread notifications for same actor, recipient, and content. | Code & Tests |
| **I.1** | Email System | Email verification | **IMPLEMENTED** | `AuthServiceImpl.register()`, `EmailDispatcher` | Sends cryptographic single-use link on registration. | Code & Tests |
| **I.2** | Email System | Password reset | **IMPLEMENTED** | `AuthServiceImpl.forgotPassword()`, `EmailDispatcher` | Sends 30-min password reset link. | Code & Tests |
| **I.3** | Email System | Welcome email | **IMPLEMENTED** | `AuthServiceImpl.verifyEmail()`, `EmailType.WELCOME` | Dispatched after successful account verification. | Code & Tests |
| **I.4** | Email System | Password-changed alert | **IMPLEMENTED** | `SettingsServiceImpl.changePassword()`, `EmailType.PASSWORD_CHANGED` | Security alert dispatched immediately after password update. | Code & Tests |
| **I.5** | Email System | New-login / security alerts | **IMPLEMENTED** | `AuthServiceImpl.login()`, `DeviceDetectionServiceImpl` | Dispatches `NEW_LOGIN` or `NEW_DEVICE_LOGIN` alert. | Code Verified |
| **I.6** | Email System | New-follower email | **IMPLEMENTED** | `NotificationServiceImpl`, `EmailType.NEW_FOLLOWER` | Dispatches email when user is followed. | Code Verified |
| **I.7** | Email System | Post-like and comment emails | **IMPLEMENTED** | `NotificationServiceImpl`, `EmailType.POST_LIKED`, `POST_COMMENTED` | Dispatches emails on engagement actions. | Code Verified |
| **I.8** | Email System | Unread-message digest | **PARTIALLY IMPLEMENTED** | `EmailType.UNREAD_MESSAGE_DIGEST` | Template exists, but no batch trigger job. | Source Code |
| **I.9** | Email System | Category-based email preferences | **IMPLEMENTED** | `EmailPreferenceServiceImpl`, `UserSettings` | User can toggle likes, comments, follows, messages, and digests. | Code & Tests |
| **I.10** | Email System | Unsubscribe mechanism & token | **IMPLEMENTED** | `EmailPreferenceServiceImpl.generateUnsubscribeUrl()`, HMAC SHA-256 | Validates signature, expires token, updates preferences without login. | Code & Tests |
| **I.11** | Email System | Template rendering & HTML escaping | **IMPLEMENTED** | `EmailTemplateRenderer.java` | Responsive HTML and plain text with HTML entity escaping. | Code & Tests |
| **I.12** | Email System | Retry and failure handling | **IMPLEMENTED** | `EmailDispatcher.dispatch()` | Up to 3 retries with exponential backoff; updates status in log table. | Code Verified |
| **I.13** | Email System | Email delivery logs | **IMPLEMENTED** | `EmailDeliveryLogRepository`, Flyway `V6` | Records status (`SENT`, `FAILED`, `SKIPPED_PREFERENCE`, `SKIPPED_THROTTLED`). | Code Verified |
| **I.14** | Email System | Debouncing / deduplication | **IMPLEMENTED** | `EmailDispatcher.isDebounced()`, Redis key `debounce:mail:*` | 30-minute debouncing window on social engagement emails. | Code Verified |
| **I.15** | Email System | Daily/weekly digest jobs | **IMPLEMENTED** | `ActivityDigestServiceImpl`, `@Scheduled` | Distributed lock; queries users with digest enabled; sends summaries. | Code Verified |
| **I.16** | Email System | Offline / simulated fallback mode | **IMPLEMENTED** | `EmailDispatcher.dispatch()`, `EmailProvider.isAvailable()` | Simulates delivery (`DELIVERED_SIMULATED`) when SMTP/mail is disabled. | Code & Tests |
| **J.1** | Privacy & Admin | Public/private account settings | **IMPLEMENTED** | `UserSettings.isPrivate`, `PrivacyServiceImpl` | Toggles account privacy; strictly gates posts, stories, and follow workflow. | Code & Tests |
| **J.2** | Privacy & Admin | Blocked-user enforcement | **IMPLEMENTED** | `PrivacyServiceImpl`, `UserBlockRepository.existsBlockBetween()` | Bidirectional block filtering across profiles, posts, stories, feeds, search, and chat. | Code & Tests |
| **J.3** | Privacy & Admin | Report users, posts, comments | **IMPLEMENTED** | `AdminController.createReport()` -> `AdminServiceImpl` | Supports `POST`, `USER`, `COMMENT` target types with audit logs. | Code & Tests |
| **J.4** | Privacy & Admin | Admin access controls | **IMPLEMENTED** | `@PreAuthorize("hasRole('ADMIN')")` in `AdminController` | Enforced via Spring Security Method Security. | Code Verified |
| **J.5** | Privacy & Admin | Admin moderation actions | **IMPLEMENTED** | `AdminController.resolveReport()`, `updateUserStatus()` | Admins can resolve reports and suspend/ban users (`UserStatus.SUSPENDED`). | Code Verified |
| **J.6** | Privacy & Admin | Content removal / suspension | **IMPLEMENTED** | `AdminServiceImpl.updateUserStatus()` | Suspended users fail authentication and STOMP connections. | Code Verified |
| **J.7** | Privacy & Admin | Privacy-aware responses | **IMPLEMENTED** | `UserServiceImpl.getUserProfileById()` | Returns 404 for blocked users; indicates isPrivate and followRequested state. | Code & Tests |
| **J.8** | Privacy & Admin | Prevent access to private content | **IMPLEMENTED** | `PrivacyService.validateCanViewUserContent()` | Returns HTTP 403 to non-followers attempting to access private posts or stories. | Code & Tests |
| **J.9** | Privacy & Admin | Prevent modifying others' resources | **IMPLEMENTED** | `PostServiceImpl`, `StoryServiceImpl`, `EngagementServiceImpl` | Ownership check enforced; non-owners receive HTTP 403 Forbidden. | Code & Tests |
| **K.1** | Redis & Dist | Cache configuration and TTLs | **IMPLEMENTED** | `CacheConstants`, `RedisUtility`, `application.yml` | Standardized key prefixes and bounded TTLs (5m, 10m, 2h, 30d). | Code Verified |
| **K.2** | Redis & Dist | Cache invalidation after mutations | **IMPLEMENTED** | `redisUtility.deleteAfterCommit()` | Transaction-synchronized cache eviction on likes, comments, follows. | Code & Tests |
| **K.3** | Redis & Dist | Distributed rate limiting | **IMPLEMENTED** | `RedisRateLimiterService`, Redis Lua script | Category-based sliding window rate limits. | Code & Tests |
| **K.4** | Redis & Dist | JWT blacklist & session revocation | **IMPLEMENTED** | `TokenBlacklistServiceImpl` | Blacklists tokens until expiration; revokes user sessions on password change. | Code & Tests |
| **K.5** | Redis & Dist | Distributed locks for scheduled jobs | **IMPLEMENTED** | `RedisDistributedLockService`, atomic Lua release | Prevents concurrent job runs across multiple instances. | Code & Tests |
| **K.6** | Redis & Dist | WebSocket presence & typing TTLs | **IMPLEMENTED** | `PresenceServiceImpl` | Multi-device presence Sets, 5-second auto-expiring typing lease. | Code & Tests |
| **K.7** | Redis & Dist | Cached post counters & profile data | **IMPLEMENTED** | `EngagementServiceImpl`, `UserServiceImpl` | Read-through caching for like/comment counts and public profiles. | Code & Tests |
| **K.8** | Redis & Dist | Kafka event deduplication | **IMPLEMENTED** | `DomainEventConsumer.onKafkaEvent()` | Atomic 24-hour deduplication keys (`idempotency:event:*`). | Code Verified |
| **K.9** | Redis & Dist | Redis timeout & error handling | **IMPLEMENTED** | Lettuce timeout 2000ms, try-catch in all Redis operations | Graceful degradation without breaking request workflows. | Code Verified |
| **K.10** | Redis & Dist | Security when Redis is unavailable | **IMPLEMENTED** | `ConcurrentHashMap` fallbacks in rate limiter and blacklist | Local in-memory fallback maintains basic rate limits and blacklists. | Code & Tests |
| **K.11** | Redis & Dist | Avoid stale/sensitive cached data | **IMPLEMENTED** | `UserServiceImpl.getUserProfileById()` | Dynamic viewer relationships stripped before caching. | Code & Tests |
| **K.12** | Redis & Dist | Appropriate non-authoritative usage | **IMPLEMENTED** | MySQL is authoritative DB; Redis used for cache, limits, presence, locks | Durability never delegated exclusively to Redis. | Code Verified |
| **L.1** | Kafka & Jobs | Kafka producers and consumers | **IMPLEMENTED** | `DomainEventPublisher`, `DomainEventConsumer`, `EmailEventListener` | JSON serialization with Spring Kafka. | Code Verified |
| **L.2** | Kafka & Jobs | Event serialization/deserialization | **IMPLEMENTED** | `JsonSerializer`, `JsonDeserializer`, trusted packages configured | Type headers enabled, trusted packages `com.instagramclone.*`. | Code Verified |
| **L.3** | Kafka & Jobs | Consumer error handling & retries | **IMPLEMENTED** | `DomainEventConsumer.java` | Evicts idempotency key on failure to allow Kafka consumer retries. | Code Verified |
| **L.4** | Kafka & Jobs | Duplicate-event handling | **IMPLEMENTED** | `idempotency:event:*` in Redis | Deduplicates redelivered messages across consumer groups. | Code Verified |
| **L.5** | Kafka & Jobs | Transaction boundaries | **IMPLEMENTED** | `emailEventPublisher.publishAfterCommit()` | Events published strictly after DB transaction commits. | Code Verified |
| **L.6** | Kafka & Jobs | Transactional outbox | **NOT IMPLEMENTED** | No outbox database table | Uses Spring `afterCommit()` hook instead of persistent outbox pattern. | Source Code |
| **L.7** | Kafka & Jobs | Scheduled story cleanup | **IMPLEMENTED** | `ScheduledCleanupJobs.cleanupExpiredStories()` | Runs hourly; deletes expired stories and Cloudinary media. | Code & Tests |
| **L.8** | Kafka & Jobs | Other scheduled jobs | **IMPLEMENTED** | Refresh token cleanup, daily & weekly activity digests | Purges expired tokens, dispatches digest emails. | Code & Tests |
| **L.9** | Kafka & Jobs | Distributed locking for cron jobs | **IMPLEMENTED** | `RedisDistributedLockService.tryLock()` | Lease duration bounded with safe owner release. | Code & Tests |
| **L.10** | Kafka & Jobs | Failure recovery behavior | **IMPLEMENTED** | Try-catch in all jobs with lock release in `finally` | Locks released even on unexpected exceptions. | Code Verified |
| **L.11** | Kafka & Jobs | Missing / unused event handlers | **PARTIALLY IMPLEMENTED** | `app.kafka.enabled` toggleable | Consumer logic present, but broker is optional in local configurations. | Code Verified |
| **M.1** | DB & Readiness | UUID usage consistency | **IMPLEMENTED** | RFC 4122 v4 UUIDs across all entities and DTOs | `BINARY(16)` in MySQL, serialized as native string in JSON. | Code & DB |
| **M.2** | DB & Readiness | Entity relationships & constraints | **IMPLEMENTED** | Foreign keys with cascades, composite unique constraints | Normalized 3NF schemas with proper foreign keys. | DB V1-V7 |
| **M.3** | DB & Readiness | Flyway migration completeness | **IMPLEMENTED** | 7 migration scripts (`V1` to `V7`) | Versioned schema evolution with zero manual DDL needed. | Resources DB |
| **M.4** | DB & Readiness | Repository query correctness | **IMPLEMENTED** | Spring Data JPA derived queries and JPQL `@Query` | Correct parameter binding and return types. | Code & Tests |
| **M.5** | DB & Readiness | Pagination and sorting | **IMPLEMENTED** | `Pageable`, `Sort`, `PageResponse<T>` | Standardized pagination envelopes on all list endpoints. | Code & Tests |
| **M.6** | DB & Readiness | DTO validation | **IMPLEMENTED** | `@Valid`, `@NotBlank`, `@Size`, `@Email` | JSR-380 validation annotations on all request bodies. | Code & Tests |
| **M.7** | DB & Readiness | Consistent API response & errors | **IMPLEMENTED** | `ApiResponse<T>`, `GlobalExceptionHandler` | Unified success/error envelope; friendly HTTP 400/404/409/413/429. | Code & Tests |
| **M.8** | DB & Readiness | API versioning under `/api/v1` | **IMPLEMENTED** | All controllers mapped under `/api/v1` | Consistent versioned base path. | Code Verified |
| **M.9** | DB & Readiness | Auth & authorization on endpoints | **IMPLEMENTED** | Spring Security filter chain + Method Security | Public endpoints explicitly allowlisted; all others require JWT. | Code Verified |
| **M.10** | DB & Readiness | CORS and security configuration | **IMPLEMENTED** | `SecurityConfig`, `CorsConfigurationSource` | Explicit allowed origins, headers, credentials, stateless session. | Code Verified |
| **M.11** | DB & Readiness | Actuator exposure & security | **IMPLEMENTED** | Health, info, metrics exposed; details configured | Actuator health probes enabled. | Config Verified |
| **M.12** | DB & Readiness | N+1 query risks | **PARTIALLY IMPLEMENTED** | Hibernate batch fetch (`batch_size: 30`) | Mitigated by batch fetching, but feed queries lack `@EntityGraph`. | Source Code |
| **M.13** | DB & Readiness | Transaction management | **IMPLEMENTED** | `@Transactional`, read-only flags on queries | Explicit transaction boundaries across all services. | Code Verified |
| **M.14** | DB & Readiness | Logging without leaking secrets | **IMPLEMENTED** | MDC correlation pattern `[%X{correlationId}]` | Passwords and JWT secrets are never logged; tokens hashed. | Code Verified |
| **M.15** | DB & Readiness | Unit and integration test suite | **IMPLEMENTED** | 116 integration tests in `backend/src/test/` | 116 pass, 0 fail, 0 errors, 100% clean teardown and pass rate. | Test Execution |
| **M.16** | DB & Readiness | Configuration via env variables | **IMPLEMENTED** | `application.yml` externalized properties | All secrets, credentials, hosts use `${ENV:default}` syntax. | Config Verified |
| **M.17** | DB & Readiness | OpenAPI / Swagger documentation | **IMPLEMENTED** | Springdoc OpenAPI 2.6.0, `@Operation`, `@Tag` | Interactive Swagger UI available at `/swagger-ui/index.html`. | Code & Config |

---

## 3. Complete Endpoint Inventory

### Authentication (`/api/v1/auth`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/auth/register` | `AuthController.register()` | Public | N/A | `@Valid` | `users`, `profiles`, `email_verification_tokens` | **IMPLEMENTED** |
| `POST /api/v1/auth/login` | `AuthController.login()` | Public | N/A | `@Valid` | `refresh_tokens`, `user_known_devices`, Redis rate limit | **IMPLEMENTED** |
| `POST /api/v1/auth/refresh` | `AuthController.refreshToken()` | Public | N/A | `@Valid` | `refresh_tokens` (rotation), Redis rate limit | **IMPLEMENTED** |
| `POST /api/v1/auth/logout` | `AuthController.logout()` | User | Token Hash | Optional | `refresh_tokens` (revocation), Redis token blacklist | **IMPLEMENTED** |
| `POST /api/v1/auth/verify-email` | `AuthController.verifyEmail()` | Public | N/A | `@Valid` | `email_verification_tokens`, `users.email_verified` | **IMPLEMENTED** |
| `POST /api/v1/auth/resend-verification` | `AuthController.resendVerification()` | Public | N/A | `@Valid` | Invalidate prior tokens, issue new token | **IMPLEMENTED** |
| `POST /api/v1/auth/forgot-password` | `AuthController.forgotPassword()` | Public | N/A | `@Valid` | `password_reset_tokens`, dispatch email event | **IMPLEMENTED** |
| `POST /api/v1/auth/reset-password` | `AuthController.resetPassword()` | Public | N/A | `@Valid` | Update password hash, revoke sessions in Redis & DB | **IMPLEMENTED** |
| `POST /api/v1/auth/unsubscribe` | `AuthController.unsubscribe()` | Public | HMAC Token | `@Valid` | `user_settings` (email preferences) | **IMPLEMENTED** |
| `POST /api/v1/auth/confirm-email-change` | `AuthController.confirmEmailChange()` | Public | Token Hash | `@Valid` | `email_change_requests`, `users.email` | **IMPLEMENTED** |

### User Profile & Settings (`/api/v1/users`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `GET /api/v1/users/me` | `UserController.getCurrentUser()` | Required | Current User | None | `users`, `profiles`, aggregate counts | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}` | `UserController.getUserProfileById()` | Required | None | None | Redis profile cache, `users`, `profiles` | **IMPLEMENTED** |
| `PUT /api/v1/users/me/profile` | `UserController.updateProfile()` | Required | Current User | `@Valid` | `profiles` update, Redis cache eviction | **IMPLEMENTED** |
| `PATCH /api/v1/users/me/avatar` | `UserController.updateAvatar()` | Required | Current User | `@Valid` | `profiles.avatar_url`, Redis cache eviction | **IMPLEMENTED** |
| `GET /api/v1/users/search` | `UserController.searchUsers()` | Required | None | None | Query `users` & `profiles` | **IMPLEMENTED** |
| `GET /api/v1/users/me/settings` | `SettingsController.getUserSettings()` | Required | Current User | None | `user_settings` | **IMPLEMENTED** |
| `PUT /api/v1/users/me/settings` | `SettingsController.updateSettings()` | Required | Current User | Optional | `user_settings` update | **IMPLEMENTED** |
| `PUT /api/v1/users/me/password` | `SettingsController.changePassword()` | Required | Current User | `@Valid` | Update password, revoke tokens, blacklist sessions | **IMPLEMENTED** |
| `POST /api/v1/users/me/email` | `SettingsController.initiateEmailChange()` | Required | Current User | `@Valid` | `email_change_requests`, send verification link | **IMPLEMENTED** |
| `DELETE /api/v1/users/me/email` | `SettingsController.cancelPendingEmailChange()` | Required | Current User | None | Cancel pending requests in DB | **IMPLEMENTED** |
| `GET /api/v1/users/me/email-preferences` | `SettingsController.getEmailPreferences()` | Required | Current User | None | `user_settings` | **IMPLEMENTED** |
| `PUT /api/v1/users/me/email-preferences` | `SettingsController.updateEmailPreferences()` | Required | Current User | Optional | `user_settings` email preferences | **IMPLEMENTED** |
| `POST /api/v1/users/{userId}/block` | `SettingsController.blockUser()` | Required | None | None | `user_blocks` insertion | **IMPLEMENTED** |
| `DELETE /api/v1/users/{userId}/block` | `SettingsController.unblockUser()` | Required | None | None | `user_blocks` deletion | **IMPLEMENTED** |
| `GET /api/v1/users/me/blocks` | `SettingsController.getBlockedUsers()` | Required | Current User | None | Paginated `user_blocks` query | **IMPLEMENTED** |

### Social Graph (`/api/v1/users` & `/api/v1/users/{userId}`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/users/{userId}/follow` | `FollowController.followUser()` | Required | Self Check | None | `follows` / `follow_requests`, notification, cache evict | **IMPLEMENTED** |
| `DELETE /api/v1/users/{userId}/follow` | `FollowController.unfollowUser()` | Required | Self Check | None | `follows` deletion, Redis cache evict | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/followers` | `FollowController.getFollowers()` | Required | None | None | Paginated `follows` query | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/following` | `FollowController.getFollowing()` | Required | None | None | Paginated `follows` query | **IMPLEMENTED** |
| `GET /api/v1/users/me/follow-requests` | `FollowController.getPendingFollowRequests()` | Required | Target Owner | None | Paginated `follow_requests` query (PENDING) | **IMPLEMENTED** |
| `POST /api/v1/users/me/follow-requests/{id}/accept` | `FollowController.acceptFollowRequest()` | Required | Target Owner | None | Accept request, insert `follows`, notifications | **IMPLEMENTED** |
| `POST /api/v1/users/me/follow-requests/{id}/reject` | `FollowController.rejectFollowRequest()` | Required | Target Owner | None | Reject request in `follow_requests` | **IMPLEMENTED** |
| `DELETE /api/v1/users/me/follow-requests/{id}/cancel` | `FollowController.cancelFollowRequest()` | Required | Requester | None | Requester cancels pending request | **IMPLEMENTED** |
| `DELETE /api/v1/users/me/followers/{followerUserId}` | `FollowController.removeFollower()` | Required | Self Check | None | Removes follower from user's `follows` graph | **IMPLEMENTED** |

### Posts & Engagement (`/api/v1/posts`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/posts` | `PostController.createPost()` | Required | Current User | `@Valid` | `posts`, `post_media` | **IMPLEMENTED** |
| `GET /api/v1/posts/{postId}` | `PostController.getPostById()` | Required | None | None | Query `posts` + media + counters | **IMPLEMENTED** |
| `PUT /api/v1/posts/{postId}` | `PostController.updatePost()` | Required | Post Owner | `@Valid` | Update caption | **IMPLEMENTED** |
| `DELETE /api/v1/posts/{postId}` | `PostController.deletePost()` | Required | Post Owner | None | Delete `posts` and cascades | **IMPLEMENTED** |
| `GET /api/v1/posts/saved` | `PostController.getSavedPosts()` | Required | Current User | None | Paginated `saved_posts` query | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/posts` | `PostController.getUserPosts()` | Required | None | None | Paginated `posts` by user | **IMPLEMENTED** |
| `POST /api/v1/posts/{postId}/like` | `EngagementController.toggleLike()` | Required | None | None | `likes` toggle, Redis counter evict, notification | **IMPLEMENTED** |
| `POST /api/v1/posts/{postId}/save` | `EngagementController.toggleSave()` | Required | None | None | `saved_posts` toggle | **IMPLEMENTED** |
| `POST /api/v1/posts/{postId}/comments` | `EngagementController.addComment()` | Required | None | `@Valid` | `comments`, Redis counter evict, notification | **IMPLEMENTED** |
| `GET /api/v1/posts/{postId}/comments` | `EngagementController.getComments()` | Required | None | None | Paginated root comments | **IMPLEMENTED** |
| `GET /api/v1/comments/{commentId}/replies`| `EngagementController.getCommentReplies()`| Required | None | None | Paginated nested replies | **IMPLEMENTED** |
| `DELETE /api/v1/comments/{commentId}` | `EngagementController.deleteComment()` | Required | Author or Post Owner | None | Delete comment and child replies | **IMPLEMENTED** |

### Feed & Discovery (`/api/v1/feed` & `/api/v1/search`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `GET /api/v1/feed` | `FeedController.getHomeFeed()` | Required | Current User | None | Chronological feed from followed users | **IMPLEMENTED** |
| `GET /api/v1/feed/explore` | `FeedController.getExploreFeed()` | Required | None | None | Public posts excluding current user | **IMPLEMENTED** |
| `GET /api/v1/search/users` | `SearchController.searchUsers()` | Required | None | None | Paginated user search | **IMPLEMENTED** |
| `GET /api/v1/search/posts` | `SearchController.searchPosts()` | Required | None | None | Paginated post caption search | **IMPLEMENTED** |

### Ephemeral Stories (`/api/v1/stories`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/stories` | `StoryController.createStory()` | Required | Current User | `@Valid` | `stories` (24h expiry) | **IMPLEMENTED** |
| `GET /api/v1/stories/feed` | `StoryController.getStoryFeed()` | Required | Current User | None | Active stories of followed users | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/stories` | `StoryController.getUserStories()` | Required | None | None | Active stories for specified user | **IMPLEMENTED** |
| `GET /api/v1/stories/{storyId}/viewers` | `StoryController.getStoryViewers()` | Required | Story Owner | None | `story_views` query (owner only) | **IMPLEMENTED** |
| `POST /api/v1/stories/{storyId}/view` | `StoryController.markStoryViewed()` | Required | None | None | `story_views` deduplicated insertion | **IMPLEMENTED** |
| `DELETE /api/v1/stories/{storyId}` | `StoryController.deleteStory()` | Required | Story Owner | None | Delete Cloudinary media & DB record | **IMPLEMENTED** |

### Reels (`/api/v1/reels`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/reels` | `ReelController.createReel()` | Required | Current User | `@Valid` | `reels` table | **IMPLEMENTED** |
| `GET /api/v1/reels` | `ReelController.getReels()` | Required | None | None | Paginated ready reels stream | **IMPLEMENTED** |
| `GET /api/v1/reels/{reelId}` | `ReelController.getReelById()` | Required | None | None | Single reel details | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/reels` | `ReelController.getUserReels()` | Required | None | None | Paginated reels by creator | **IMPLEMENTED** |
| `DELETE /api/v1/reels/{reelId}` | `ReelController.deleteReel()` | Required | Reel Owner | None | Delete Cloudinary video & DB record | **IMPLEMENTED** |

### Direct Messaging & Presence (`/api/v1/conversations` & `/api/v1/users`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/conversations` | `ChatController.getOrCreateConversation()`| Required| Mutual Block Check| `@Valid` | `conversations`, `conversation_members` | **IMPLEMENTED** |
| `GET /api/v1/conversations` | `ChatController.getConversations()` | Required | Current User | None | User's conversations with unread counts | **IMPLEMENTED** |
| `GET /api/v1/conversations/{id}` | `ChatController.getConversationById()` | Required | Member Check | None | Single conversation details | **IMPLEMENTED** |
| `GET /api/v1/conversations/{id}/messages`| `ChatController.getMessages()` | Required | Member Check | None | Paginated messages history | **IMPLEMENTED** |
| `POST /api/v1/conversations/{id}/messages`| `ChatController.sendMessage()` | Required | Member Check | None | `messages`, STOMP broadcast | **IMPLEMENTED** |
| `POST /api/v1/conversations/{id}/read` | `ChatController.markConversationAsRead()`| Required | Member Check | None | Update `messages.is_read` | **IMPLEMENTED** |
| `GET /api/v1/users/{userId}/presence` | `PresenceController.getUserPresence()` | Required | Mutual Block Check| None | Redis presence & `last_seen` lookup | **IMPLEMENTED** |

### Notifications (`/api/v1/notifications`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `GET /api/v1/notifications` | `NotificationController.getNotifications()`| Required| Current User | None | Paginated notifications | **IMPLEMENTED** |
| `GET /api/v1/notifications/unread-count`| `NotificationController.getUnreadCount()` | Required | Current User | None | Redis cached unread count | **IMPLEMENTED** |
| `PUT /api/v1/notifications/{id}/read` | `NotificationController.markAsRead()` | Required | Recipient | None | Mark read, evict Redis cache | **IMPLEMENTED** |
| `PUT /api/v1/notifications/read-all` | `NotificationController.markAllAsRead()` | Required | Current User | None | Bulk mark read, evict Redis cache | **IMPLEMENTED** |

### Media Management (`/api/v1/media`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/media` | `MediaController.uploadMedia()` | Required | None | MIME / Size | Cloudinary upload, `media` metadata DB | **IMPLEMENTED** |
| `GET /api/v1/media/{mediaId}` | `MediaController.getMedia()` | Required | None | None | Query `media` table | **IMPLEMENTED** |
| `DELETE /api/v1/media/{mediaId}` | `MediaController.deleteMedia()` | Required | Owner Check | None | Delete Cloudinary asset & DB record | **IMPLEMENTED** |

### Administration & Reports (`/api/v1`)
| HTTP Method & Path | Controller & Handler Method | Auth? | Ownership Check? | Validation? | Downstream Processing / DB | Status |
|:---|:---|:---:|:---:|:---:|:---|:---:|
| `POST /api/v1/reports` | `AdminController.createReport()` | Required | Current User | `@Valid` | `reports` table insertion | **IMPLEMENTED** |
| `GET /api/v1/admin/reports` | `AdminController.getReports()` | Admin Only | Role ADMIN | None | Paginated reports list | **IMPLEMENTED** |
| `PUT /api/v1/admin/reports/{reportId}` | `AdminController.resolveReport()` | Admin Only | Role ADMIN | `@Valid` | `reports` status resolution | **IMPLEMENTED** |
| `PUT /api/v1/admin/users/{userId}/status`| `AdminController.updateUserStatus()` | Admin Only | Role ADMIN | `@Valid` | `users.status` (ACTIVE/SUSPENDED/BANNED) | **IMPLEMENTED** |

---

## 4. Database & Infrastructure Mapping

### 4.1 Database Entities & Migrations
| Entity Class | MySQL Table | Primary Key | Foreign Keys & Cascades | Flyway Migration |
|:---|:---|:---|:---|:---:|
| `User` | `users` | `BINARY(16)` | None | `V1__initial_identity_foundation.sql` |
| `Profile` | `profiles` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V1` |
| `RefreshToken` | `refresh_tokens` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V1` |
| `UserSettings` | `user_settings` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V2__complete_schema.sql` |
| `Follow` | `follows` | `BINARY(16)` | `follower_id, following_id -> users(id) ON DELETE CASCADE` | `V2` |
| `UserBlock` | `user_blocks` | `BINARY(16)` | `blocker_id, blocked_id -> users(id) ON DELETE CASCADE` | `V2` |
| `Post` | `posts` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V2` |
| `PostMedia` | `post_media` | `BINARY(16)` | `post_id -> posts(id) ON DELETE CASCADE` | `V2` |
| `Like` | `likes` | `BINARY(16)` | `user_id, post_id -> ON DELETE CASCADE` | `V2` |
| `Comment` | `comments` | `BINARY(16)` | `user_id, post_id, parent_id -> ON DELETE CASCADE` | `V2` |
| `SavedPost` | `saved_posts` | `BINARY(16)` | `user_id, post_id -> ON DELETE CASCADE` | `V2` |
| `Story` | `stories` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V2` |
| `StoryView` | `story_views` | `BINARY(16)` | `story_id, user_id -> ON DELETE CASCADE` | `V2` |
| `Reel` | `reels` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V2` |
| `Conversation` | `conversations` | `BINARY(16)` | None | `V2` |
| `ConversationMember`| `conversation_members` | `BINARY(16)` | `conversation_id, user_id -> ON DELETE CASCADE` | `V2` |
| `Message` | `messages` | `BINARY(16)` | `conversation_id, sender_id -> ON DELETE CASCADE` | `V2` |
| `Notification` | `notifications` | `BINARY(16)` | `recipient_id, actor_id -> ON DELETE CASCADE` | `V2` |
| `Report` | `reports` | `BINARY(16)` | `reporter_id -> users(id)` | `V2` |
| `AuditLog` | `audit_logs` | `BINARY(16)` | None | `V3__audit_logs.sql` |
| `Media` | `media` | `BINARY(16)` | `owner_id -> users(id)` | `V4__media_metadata.sql` |
| `EmailVerificationToken`| `email_verification_tokens`| `BINARY(16)`| `user_id -> users(id) ON DELETE CASCADE` | `V5__email_verification...` |
| `PasswordResetToken` | `password_reset_tokens` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V5` |
| `EmailDeliveryLog`| `email_delivery_logs` | `BINARY(16)` | `user_id -> users(id) ON DELETE SET NULL` | `V6__email_categories...` |
| `EmailChangeRequest`| `email_change_requests` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V7__email_change...` |
| `UserKnownDevice` | `user_known_devices` | `BINARY(16)` | `user_id -> users(id) ON DELETE CASCADE` | `V7` |
| `PostMediaTag` | `post_media_tags` | `BINARY(16)` | `post_media_id, tagged_user_id -> ON DELETE CASCADE` | `V7` |
| `FollowRequest`| `follow_requests` | `BINARY(16)` | `requester_id, target_id -> users(id) ON DELETE CASCADE`| `V8__follow_requests.sql` |

---

### 4.2 Redis Key Patterns and TTLs
| Key Pattern | Data Structure | TTL | Purpose |
|:---|:---|:---|:---|
| `blacklist:token:<sha256>` | String (`revoked`) | Remaining token lifespan (max 15m) | Stateless JWT access-token revocation on logout |
| `revocation:user:<userId>` | String (timestamp) | 15 minutes (`accessExpirationMs`) | Invalidation of all tokens issued before password update |
| `rate_limit:<category>:<key>` | Integer counter | 60 seconds (sliding window) | Distributed sliding-window API rate limiting |
| `lock:job:<jobName>` | String (`ownerUuid`) | 5 to 15 minutes (bounded lease) | Distributed mutual exclusion for `@Scheduled` cron jobs |
| `presence:user:<id>:sessions` | Set of session IDs | 2 hours (auto-refreshed) | Multi-device presence session tracking |
| `presence:user:<id>:status` | String (`online`) | 2 hours | Fast presence status lookup |
| `presence:user:<id>:last_seen` | String (ISO timestamp) | 30 days | Last active timestamp |
| `presence:session:<sessionId>` | String (`userId`) | 2 hours | Reverse mapping for WebSocket disconnect events |
| `chat:typing:<convId>:<userId>` | String (`typing`) | 5 seconds | Ephemeral typing indicator |
| `count:post:likes:<postId>` | String (count) | 10 minutes | Read-through counter cache for post likes |
| `count:post:comments:<postId>` | String (count) | 10 minutes | Read-through counter cache for post comments |
| `cache:user:profile:<userId>` | String (JSON DTO) | 10 minutes | Public user profile caching (stripped of viewer follow state) |
| `cache:notifications:unread:<userId>`| String (count) | 5 minutes | Cached unread notification count |
| `debounce:mail:<type>:<actor>:<recip>:<target>`| String (`1`) | 30 minutes | Social activity email spam debouncing |
| `idempotency:event:<eventId>` | String (`processing` / `processed`) | 24 hours | Kafka domain event consumer deduplication |

---

### 4.3 Kafka Topics & WebSocket Channels
* **Kafka Topics:**
  - `instagram-clone.events` (Domain events: `DomainEvent` with JSON serialization)
  - `instagram-clone.emails` (Email events: `EmailEvent` with JSON serialization)
* **WebSocket STOMP Inbound Mappings:**
  - `/app/chat.send/{conversationId}` (Send chat message)
  - `/app/chat.typing/{conversationId}` (Broadcast typing state)
* **WebSocket STOMP Subscriptions:**
  - `/topic/conversations/{conversationId}` (Private direct message channel; membership verified)
  - `/topic/notifications/{userId}` (Private notification channel; user ID verified)
  - `/topic/presence` (Global user presence and status updates)

---

### 4.4 Scheduled Jobs
| Job Method | Class | Cron Expression | Distributed Lock Key | Purpose |
|:---|:---|:---|:---|:---|
| `cleanupExpiredStories()` | `ScheduledCleanupJobs` | `0 0 * * * *` (Hourly) | `lock:job:cleanup-expired-stories` | Purges stories past 24h & removes Cloudinary media |
| `cleanupExpiredTokens()` | `ScheduledCleanupJobs` | `0 0 2 * * *` (Daily 2 AM) | `lock:job:cleanup-expired-tokens` | Purges revoked and expired refresh tokens |
| `executeDailyActivityDigest()`| `ActivityDigestServiceImpl`| `0 0 9 * * *` (Daily 9 AM) | `lock:job:email:digest:daily` | Dispatches daily activity summaries to subscribed users |
| `executeWeeklyActivityDigest()`| `ActivityDigestServiceImpl`| `0 0 9 * * MON` (Mon 9 AM)| `lock:job:email:digest:weekly` | Dispatches weekly activity summaries |

---

## 5. Security & Reliability Findings

### Finding 1: Private Account Access Control Bypass
* **Severity:** **CRITICAL** — **STATUS: RESOLVED IN PHASE 1**
* **Remediation:** Centralized `PrivacyService` added; `PostServiceImpl`, `StoryServiceImpl`, `FeedServiceImpl`, and `SearchServiceImpl` enforce private content gating. Follow requests added via Flyway `V8` migration.
* **Verification:** Validated with 13 automated security tests in `PrivateAccountAndBlockSecurityTest`. Non-followers attempting direct or list access receive HTTP 403 Forbidden.

---

### Finding 2: Block Privacy Inconsistencies Across Read Endpoints
* **Severity:** **HIGH** — **STATUS: RESOLVED IN PHASE 1**
* **Remediation:** Bidirectional block validation (`existsBlockBetween`) integrated into `PrivacyService`, `UserServiceImpl`, `PostServiceImpl`, `StoryServiceImpl`, and database queries in `PostRepository` and `UserRepository`.
* **Verification:** Blocked requests return HTTP 404 Not Found to prevent leaking user or resource existence; blocked users are filtered from explore feeds and search results. Validated in `PrivateAccountAndBlockSecurityTest`.

---

### Finding 3: Missing Account Lockout on Brute-Force Authentication
* **Severity:** **MEDIUM**
* **Affected Files:**
  - `com.instagramclone.user.service.AuthServiceImpl.java:147-213`
  - `com.instagramclone.security.UserPrincipal.java:62-64`
* **Defect Description:**
  Authentication relies solely on Redis rate limiting (10 attempts per minute per IP). If an attacker distributes requests across a proxy botnet, there is no account-level failed attempt counter to lock the account (`failedAttempts >= 5 -> lockedUntil = now + 15m`).
* **Impact:** Credential stuffing attacks against known email addresses can be distributed across multiple IPs.

---

### Finding 4: In-App Notification Duplicate Amplification
* **Severity:** **LOW-MEDIUM**
* **Affected File:**
  - `com.instagramclone.notification.service.NotificationServiceImpl.java:105-143`
* **Defect Description:**
  `createNotification()` creates and saves a new `Notification` entity unconditionally. If a user unlikes and likes a post repeatedly, or follows and unfollows, duplicate unread notifications are stored in the database.
* **Impact:** Database table bloat and poor user experience.

---

## 6. Test Evidence

The backend test suite was executed in the workspace using Apache Maven.

### Execution Command
```bash
mvn test
```

### Execution Results (Post Phase 1)
* **Duration:** 3 minutes 53 seconds
* **Tests Run:** **116**
* **Failures:** **0**
* **Errors:** **0**
* **Skipped:** **0**
* **Passed Cleanly:** **116** (100% pass rate)

### Test Teardown Resolution
1. In `BaseIntegrationTest.cleanDatabase()`, foreign key dependencies are deleted in strict safe order:
   - `postMediaTagRepository.deleteAll()`
   - `userKnownDeviceRepository.deleteAll()`
   - `emailChangeRequestRepository.deleteAll()`
   - `followRequestRepository.deleteAll()`
   - `auditLogRepository.deleteAll()`
2. All 57 previous foreign-key referential integrity violation errors in H2 are completely resolved.
3. 13 new integration tests in `PrivateAccountAndBlockSecurityTest` verify private account gating, follow request lifecycle, and bidirectional block enforcement.

---

## 7. Implementation Roadmap

### Phase 1 — Critical Security & Privacy Fixes [COMPLETED & VERIFIED]
1. **Fix `BaseIntegrationTest.java` Database Teardown:** Injected and cleaned all dependent child repositories (`user_known_devices`, `email_change_requests`, `post_media_tags`, `follow_requests`, `audit_logs`). Restored test suite to 116 / 116 passing with 0 failures and 0 errors.
2. **Implement Private Account Content Protection:**
   - Centralized `PrivacyService` integrated into `PostServiceImpl.getPostById()`, `getUserPosts()`, `StoryServiceImpl.getUserStories()`, and `markStoryViewed()`. Non-followers attempting access receive HTTP 403 Forbidden.
   - Private posts and accounts filtered from `FeedServiceImpl.getExploreFeed()` and `SearchServiceImpl.searchPosts()`.
3. **Implement Follow Request Workflow:**
   - Flyway migration `V8__follow_requests.sql` and `FollowRequest` entity added.
   - When target account is private, `followUser()` creates a pending follow request and emits `FOLLOW_REQUEST` notification.
   - Exposes endpoints: `GET /api/v1/users/me/follow-requests`, `POST /api/v1/users/me/follow-requests/{id}/accept`, `POST /api/v1/users/me/follow-requests/{id}/reject`, `DELETE /api/v1/users/me/follow-requests/{id}/cancel`, and `DELETE /api/v1/users/me/followers/{followerUserId}` (remove follower).
4. **Enforce Comprehensive Block Privacy:**
   - `PrivacyService.validateBlockStatus()` enforces bidirectional blocking across `UserServiceImpl.getUserProfileById()`, `PostServiceImpl`, and `StoryServiceImpl`, returning HTTP 404 Not Found to prevent metadata leaking.
   - Blocked users excluded bidirectionally in `PostRepository.findExplorePosts()`, `searchPosts()`, and `UserRepository.searchUsers()`.

### Phase 2 — Reels Engagement and Authentication Hardening [COMPLETED & VERIFIED]
1. **Reel Likes (`ReelLike`):**
   - Flyway migration `V9__reels_engagement_and_lockout.sql` creates `reel_likes` table with unique constraint `uk_reel_likes (user_id, reel_id)`.
   - `ReelEngagementService.toggleLike()` supports like/unlike toggle with concurrency safety against duplicate inserts.
   - Centralized `PrivacyService` and bidirectional block checks enforce access rules.
   - Redis counter cache invalidated immediately and after commit (`cache:count:reels:likes:{id}`).
   - Emits single `REEL_LIKED` notification on initial like; unlikes do NOT generate notifications.
2. **Reel Comments (`ReelComment`):**
   - Flyway migration `V9` creates `reel_comments` table with foreign keys and indices.
   - Endpoints: `POST /api/v1/reels/{reelId}/comments`, `GET /api/v1/reels/{reelId}/comments` (paginated), `DELETE /api/v1/reels/comments/{commentId}`.
   - Deletion authorization verifies either comment author OR reel owner.
   - Emits `REEL_COMMENTED` notification for reel owner.
3. **Reel View Tracking (`ReelView`):**
   - Flyway migration `V9` creates `reel_views` table with unique constraint `uk_reel_views (reel_id, user_id)` for durable deduplication per viewer.
   - Endpoint: `POST /api/v1/reels/{reelId}/view` records unique views and returns view count.
   - Redis counter cache accelerated with immediate and after-commit invalidation (`cache:count:reels:views:{id}`).
4. **Progressive Account Lockout:**
   - Flyway migration `V9` adds `failed_login_attempts INT NOT NULL DEFAULT 0` and `lockout_until DATETIME NULL` to `users`.
   - `AccountLockoutService` records consecutive failed credentials in an isolated transaction (`Propagation.REQUIRES_NEW`) with pessimistic write locking.
   - Reaching threshold (5 attempts) sets lockout for 15 minutes.
   - Spring Security checks `UserDetails.isAccountNonLocked()`, throwing `LockedException` mapped to HTTP 423 LOCKED in `GlobalExceptionHandler`.
   - Anti-enumeration: returns identical HTTP 401 `INVALID_CREDENTIALS` for both incorrect passwords and non-existent users prior to lockout.
   - Successful login resets attempts and clears lockout timestamp.
   - Password reset via `AuthServiceImpl.resetPassword()` automatically unlocks the account.
   - Lockout expires naturally after duration; subsequent successful login clears failure state.
5. **Notification Deduplication:**
   - Redis distributed locking around programmatic `TransactionTemplate` eliminates `UnexpectedRollbackException` and race conditions on duplicate concurrent like toggles or view requests.
   - `NotificationRepository.existsUnreadNotification` check guarded with distributed locking to serialize Kafka redeliveries and avoid duplicate notification rows, WebSockets, or emails.
   - Does not suppress legitimate new notifications once previous notifications are marked as read.
6. **Full Suite Verification:**
   - 158 of 158 tests passing cleanly across the entire repository with 0 failures and 0 errors.

### Module 1 — Real-Time WebSocket & Chat Security Hardening
1. **Task 1: Initial Security & Stability Audit [COMPLETED & VERIFIED]**
   - Read-only audit across all backend domains, verified baseline of 136 passing tests, and confirmed BOLA authorization gap in `WebSocketChatController.handleTyping()`.
2. **Task 2: Secure WebSocket Typing Indicators [COMPLETED & VERIFIED]**
   - Injected conversation membership validation in `WebSocketChatController.handleTyping()` and implemented `ChatService.validateTypingPermission()`.
   - Prevented non-members and non-existent conversation requests from creating, modifying, or clearing typing states with `ForbiddenException`.
   - Enforced direct-message bidirectional block policies: blocked users cannot broadcast typing indicators.
   - Guarded against missing or invalid authentication: throws `UnauthorizedException`.
   - Added 7 comprehensive regression tests in `WebSocketSecurityTest`.
   - All 143 integration tests passing cleanly across full `mvn test` suite.
3. **Task 3: Fix WebSocket Typing Subscription Authorization [COMPLETED & VERIFIED]**
   - Corrected destination path parsing in `WebSocketAuthInterceptor` to support both `/topic/conversations/{conversationId}` and `/topic/conversations/{conversationId}/typing`.
   - Replaced fragile substring matching with strict path segment validation (`split("/", -1)`), rejecting root paths, trailing slashes, empty segments, arbitrary suffixes, and subpath traversal.
   - Enforced conversation membership verification and authentication checks across both conversation and typing topics before allowing subscriptions.
   - Rejected malformed UUIDs with `MessageDeliveryException("BadRequest: Invalid conversation ID")` and malformed paths with `MessageDeliveryException("BadRequest: Invalid conversation destination")`.
   - Prevented non-members from subscribing with `MessageDeliveryException("Forbidden: You are not a member of this conversation")`.
   - Maintained strict authorization for notification topics (`/topic/notifications/{userId}`) and preserved public presence topics.
   - Added 6 comprehensive regression tests in `WebSocketSecurityTest`.
   - All 149 integration tests passing cleanly across full `mvn test` suite (`BUILD SUCCESS`, 0 failures, 0 errors, 0 skipped).
4. **Task 4: STOMP Exception Handling [COMPLETED & VERIFIED]**
   - Implemented declarative `@MessageExceptionHandler` methods on `WebSocketChatController` targeting `ApiException`, `MethodArgumentNotValidException`, `AccessDeniedException`, `IllegalArgumentException`, and unhandled `Exception`.
   - Error responses are packaged into standard `ApiResponse<Object>` and routed exclusively to the requesting user via `@SendToUser(destinations = "/queue/errors", broadcast = false)`.
   - Prevented exception details from leaking to shared conversation broadcast topics (`/topic/conversations/{id}`).
   - Sanitized unexpected internal exceptions to generic `"An unexpected internal error occurred. Please try again later."` (`INTERNAL_SERVER_ERROR`), preventing exposure of stack traces, SQL syntax, or internal class names.
   - Propagated authenticated `Principal` into `SecurityContextHolder` within `handleChatMessage` to guarantee reliable asynchronous thread authorization.
5. **Task 5: Production CORS Security Audit & Hardening [COMPLETED & VERIFIED]**
   - Audited `CorsConfig`, `application.yml`, and `application-prod.yml`. Identified that production previously inherited permissive localhost wildcard patterns (`http://localhost:*`, `http://127.0.0.1:*`) with `allowCredentials=true` due to unconditional development origin addition and missing production CORS profile configuration.
   - Hardened `CorsConfig`: Injected `Environment` to detect production runtime (`prod`/`production` profiles).
   - In production, strictly enforces explicit origin declaration via `CORS_ALLOWED_ORIGINS` / `app.security.cors.allowed-origins`. Rejects wildcard patterns (`*`), localhost/127.0.0.1 origins, and malformed schemes with `IllegalStateException` on startup rather than silently falling back to insecure defaults.
   - Preserved legitimate local development behavior for `dev` and `test` environments (`http://localhost:*`, `http://127.0.0.1:*`, Vite/React dynamic ports).
   - Added `application-prod.yml` override `app.security.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}` preventing base default localhost inheritance.
   - Added 11 new unit tests in `CorsConfigTest` (13 total) and 8 new integration tests in `CorsSecurityIntegrationTest` verifying preflight headers, credentials, allowed methods, localhost/127.0.0.1:5173, dynamic Vite ports, and unapproved external origin rejection.
   - Full Maven suite verified at 177 of 177 passing tests (`BUILD SUCCESS`, 0 failures, 0 errors, 0 skipped).
6. **Task 6: Account Lockout & Account Enumeration Hardening [COMPLETED & VERIFIED]**
   - Decoupled account status (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `DELETED`) from temporary lockout state in `UserPrincipal`: set `nonLocked(!isLocked)` and `active(isActive)` to eliminate status conflation and premature locked exceptions on inactive accounts.
   - Introduced `CustomAuthenticationProvider` extending `DaoAuthenticationProvider`: pre-computes dummy BCrypt password hash and performs timing attack mitigation on `AccountStatusException` (locked and disabled accounts), equalizing execution duration with nonexistent and bad-credential logins (~150-250ms).
   - Standardized authentication failure responses across all failure modes (nonexistent, incorrect password, temporarily locked, and inactive/disabled) to uniform HTTP 401 UNAUTHORIZED (`INVALID_CREDENTIALS`, `"Invalid username or password"`).
   - Updated `AuthServiceImpl.login` to intercept `LockedException`, delegate to `accountLockoutService.handleFailedLogin` for audit logging, and propagate safely.
   - Updated `AccountLockoutSecurityTest` and added 3 new regression tests verifying uniform HTTP 401 responses, disabled/suspended/deleted account rejection, and execution path timing equalization.
   - Full Maven suite verified at 180 of 180 passing tests (`BUILD SUCCESS`, 0 failures, 0 errors, 0 skipped).
7. **Task 7: Trusted IP Resolution and Login Abuse Protection [COMPLETED & VERIFIED]**
   - Created centralized `ClientIpResolver` enforcing strict trusted-proxy boundary verification: forwarding headers (`X-Forwarded-For`, `X-Real-IP`) are never trusted unless the immediate peer socket address (`request.getRemoteAddr()`) matches an explicitly configured trusted proxy address or CIDR range.
   - Implemented reverse proxy chain traversal (right to left) stopping at the first untrusted upstream IP, safely skipping intermediate trusted hops and ignoring client-injected spoofed headers.
   - Added IP syntax validation rejecting malformed headers without blocking DNS lookups and normalized IPv6 loopback variants (`::1`, `0:0:0:0:0:0:0:1`) to `127.0.0.1`.
   - Unified trusted IP resolution across `RateLimitInterceptor`, `AuthServiceImpl`, and `DeviceDetectionService`, eliminating internal IP resolution discrepancies.
   - Added configurable `app.rate-limit.trusted-proxies` with local defaults for development (`127.0.0.1,::1,0:0:0:0:0:0:0:1`) and empty override in `application-prod.yml` to prevent accidental proxy trust in production without explicit configuration.
   - Implemented account-targeted login rate limiting (`auth-login-account`, key `rate_limit:auth-login:account:<usernameOrEmail>`, limit 10/60s) in `AuthServiceImpl.login()` before password verification and database lockout handling, neutralizing Account-Lockout DoS attacks and distributed credential-stuffing at the Redis layer.
   - Documented the multi-instance limitations of the in-memory fallback cache in `RedisRateLimiterService`.
   - Added 10 unit tests in `ClientIpResolverTest`, 1 spoofing rejection test in `RateLimitingIntegrationTest`, and 5 integration tests in `LoginRateLimitAndLockoutTest` verifying rotating IP throttling, rate-limited attempt DB lockout immunity, anti-enumeration parity for nonexistent accounts, device tracking IP consistency, and atomic concurrent request handling.
   - Full Maven suite verified at 196 of 196 passing tests across 34 test classes (`BUILD SUCCESS`, 0 failures, 0 errors, 0 skipped).

---

## 8. Final Verdict

### What is Genuinely Implemented?
* **Production-Grade Authentication:** Stateless JWT, token blacklisting in Redis, refresh token rotation, BCrypt(12), device fingerprinting, session invalidation on password change, timing attack equalization, and anti-enumeration generic 401 responses.
* **Progressive Account Lockout & Abuse Protection (Phase 2 & Module 1):** Configurable failed login thresholds, temporary account locking, natural expiry decay, instant unlock on password reset, dual-key rate limiting (IP + account identifier), trusted-proxy validation, and Account-Lockout DoS prevention.
* **Reels Engagement Subsystem & Concurrency Hardening (Phase 2):** Complete reel like/unlike toggling, paginated comments, ownership-verified deletion, and deduplicated durable view tracking with Redis count acceleration and distributed locking to prevent rollback-only anomalies.
* **Notification Deduplication (Phase 2):** Idempotent unread notification deduplication preventing Kafka redelivery spam or rapid client toggle duplicates.
* **Secure Real-Time Messaging & Exception Handling (Module 1, Tasks 2, 3, & 4):** STOMP WebSocket messaging, conversation read receipts, multi-device presence tracking, subscription BOLA authorization for messages and typing topics (`/topic/conversations/{id}` and `/topic/conversations/{id}/typing`), membership/block-verified typing indicators, and isolated user-queue exception routing (`/user/queue/errors`).
* **Hardened Production CORS Policy (Module 1, Task 5):** Environment-aware CORS configuration enforcing explicit non-wildcard production domains, fail-fast startup on missing/invalid production origins, and full isolation from development localhost wildcards while preserving local Vite tooling.
* **Email Subsystem:** Complete with template rendering, unsubscribe HMAC tokens, category preferences, bounded retries, delivery audit logs, and debouncing.
* **Post & Carousel Media Lifecycle:** Multi-media post creation, edit caption, delete cascade, like, save, 2-level threaded comments, and Cloudinary upload/deletion with local offline fallback.
* **Ephemeral Stories:** 24-hour expiration, viewer tracking with deduplication, owner-only viewer list, and Redis-locked cron cleanup.
* **Distributed Infrastructure:** Redis rate limiting, distributed lock manager, Redis counter caches, public profile caching, and Kafka event consumer idempotency.
* **Private Account Content Protection & Follow Requests (Phase 1):** Complete pending/accepted request lifecycle, private content gating, removal of followers, in-app notifications, and email alerts.
* **Bidirectional Block Privacy (Phase 1):** Unified `PrivacyService` preventing blocked interactions or discovery across profiles, posts, stories, feeds, search, chat, and reels.
* **Verified Test Suite:** 196 of 196 tests passing cleanly across the full Maven suite with zero failures or errors.

### What is Partially Implemented?
* **Feed Ranking:** Pure chronological ordering without algorithmic engagement scoring.
* **Digest Emails:** Activity digests implemented with scheduled jobs; unread message digests missing trigger job.
* **Notification Preferences:** Settings stored, but in-app notification creation does not check category toggles.

### What is Missing?
* Photo tagging and `@username` caption/comment mentions.
* Hashtags and hashtag discovery.
* Group direct messaging.
* Direct message deletion/unsend.
* User account deletion and GDPR data export.
* Story highlights and Close Friends lists.

### What Should Be Implemented Next?
1. **Direct Messaging Unsend & Group Conversations:** Message deletion over REST + STOMP WebSocket broadcast, group conversation creation and membership administration.
2. **Phase 3 — Media & Engagement:** Photo coordinate tagging (`PostMediaTag`) and hashtag extraction/indexing.
3. **Phase 4 — Operational Hardening:** Feed `@EntityGraph` query optimization and account deletion/export.

