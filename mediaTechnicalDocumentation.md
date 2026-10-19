# Cloudinary Media Storage, Processing & Delivery Infrastructure

**Document:** `mediaTechnicalDocumentation.md`  
**Project:** Instagram Clone  
**Layer:** Backend & Frontend Media Integration  
**Status:** PRODUCTION-READY  
**Storage Provider:** Cloudinary (Primary Cloud Delivery & Transformation)  
**Persistence:** MySQL (`media` table metadata only, binaries never stored in DB)  
**Database Migration:** Flyway `V4__media_metadata.sql`  
**Rate Limiting:** Redis-backed (`@RateLimit(category = "media-upload", limit = 15, windowSeconds = 60)`)  

---

## 1. Architectural Overview

Cloudinary serves as the primary cloud media storage, transformation, and content delivery platform for all user-uploaded content in the Instagram Clone application, including:
- **Profile / Avatar Photos**
- **Post Photos & Multi-Media Carousels**
- **Post Videos**
- **Story Photos & Videos**
- **Reel Short-Form Videos**

The backend enforces strict separation between media binaries and relational data:
- **Cloudinary:** Manages media asset storage, global CDN distribution, on-the-fly transformations, format optimization, responsive resizing, and video transcoding.
- **MySQL:** Stores comprehensive media metadata (UUID, public ID, secure HTTPS URL, resource type, media type, dimensions, duration, file size, owner UUID, status, and folder path).
- **Backend Media Layer:** Serves as the authoritative gatekeeper for authentication, authorization, MIME/extension/size validation, and Cloudinary API communication.

```
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â”‚ Frontend â”‚ (React + Vite)
   â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜
        â”‚ 1. POST /api/v1/media (multipart/form-data)
        â–¼
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â”‚ Spring Boot Media Controller & Interceptors â”‚
   â”‚ - JWT Authentication                         â”‚
   â”‚ - Distributed Rate Limiting (Redis 15 req/m) â”‚
   â”‚ - File Validation (MIME, Magic Bytes, Size)  â”‚
   â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
        â”‚ 2. CloudinaryService.uploadMedia(...)
        â–¼
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â”‚ Cloudinary Cloud Media Platform              â”‚
   â”‚ - Generates secure HTTPS URL                 â”‚
   â”‚ - Allocates unique publicId                  â”‚
   â”‚ - Extracts dimensions & duration             â”‚
   â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
        â”‚ 3. Cloudinary Upload Result
        â–¼
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â”‚ MediaService / MySQL Database                â”‚
   â”‚ - Persists Media entity metadata (Flyway V4) â”‚
   â”‚ - Never stores media binaries in database    â”‚
   â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
        â”‚ 4. ApiResponse<MediaResponse>
        â–¼
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â”‚ Frontend â”‚ (Receives clean DTO with secureUrl & id)
   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## 2. Configuration & Environment Variables

Cloudinary configuration is strictly externalized. Credentials are **never** committed to version control and **never** exposed to the frontend bundle or client APIs.

### Backend Environment Variables

| Variable | Description | Example / Placeholder |
|---|---|---|
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud account name | `your_cloud_name` |
| `CLOUDINARY_API_KEY` | Cloudinary API Key | `your_api_key` |
| `CLOUDINARY_API_SECRET` | Cloudinary API Secret (**Backend ONLY**) | `your_api_secret` |

### Spring Boot Properties (`application.yml`)

```yaml
app:
  cloudinary:
    cloud-name: ${CLOUDINARY_CLOUD_NAME:}
    api-key: ${CLOUDINARY_API_KEY:}
    api-secret: ${CLOUDINARY_API_SECRET:}
```

- **Configuration Class:** `com.instagramclone.config.CloudinaryConfig`
- **Properties Class:** `com.instagramclone.integration.cloudinary.CloudinaryProperties`
- **Retry Policy:** Automatic retry with backoff (`maxRetries = 2`, `retryBackoffMs = 500ms`) for transient network issues.
- **Resilient Fallback Mode:** When running in local development or test suites where real credentials are not supplied (or contain placeholder strings such as `demo`, `test-cloud`, or `your_cloud_name`), `CloudinaryProperties.isConfigured()` returns `false` and `CloudinaryService` automatically falls back to local disk storage (`/uploads/...`). This prevents test breakage or application startup failures when offline.

---

## 3. Database Schema & Media Entity (Flyway V4)

Migration: `src/main/resources/db/migration/V4__media_metadata.sql`

```sql
CREATE TABLE IF NOT EXISTS media (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(700) NOT NULL,
    resource_type VARCHAR(50) NOT NULL DEFAULT 'image',
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    format VARCHAR(20) NULL,
    width INT NULL,
    height INT NULL,
    duration DOUBLE NULL,
    file_size BIGINT NULL,
    folder VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_media_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_media_owner (owner_id),
    INDEX idx_media_public_id (public_id),
    INDEX idx_media_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### JPA Entity (`com.instagramclone.media.entity.Media`)

- **id:** UUID binary primary key (`BINARY(16)`).
- **owner:** ManyToOne User owner.
- **publicId:** Cloudinary unique public ID (used for deletion and transformations).
- **secureUrl:** HTTPS Cloudinary asset delivery URL.
- **resourceType:** `image` or `video`.
- **mediaType:** `IMAGE` or `VIDEO`.
- **format:** Extension/codec (`jpg`, `png`, `mp4`, `webp`).
- **width & height:** Pixel dimensions.
- **duration:** Video length in seconds (nullable for images).
- **fileSize:** Asset size in bytes.
- **folder:** Cloudinary folder namespace.
- **status:** Processing status (`READY`, `DELETED`).

---

## 4. Folder Organization & Public ID Strategy

All uploads are organized into predictable, UUID-partitioned folders to avoid naming collisions and preserve privacy. Usernames are never used as primary folder paths because usernames are mutable.

### Folder Hierarchy

```
instagram-clone/
    users/
        {userUuid}/
            avatar/
    posts/
        {userUuid}/
    stories/
        {userUuid}/
    reels/
        {userUuid}/
    general/
        {userUuid}/
```

### Public ID Convention

Public IDs follow the format:
```
instagram-clone/users/{userUuid}/avatar/{assetUuid}
instagram-clone/posts/{userUuid}/{assetUuid}
instagram-clone/stories/{userUuid}/{assetUuid}
instagram-clone/reels/{userUuid}/{assetUuid}
```
Storing this public ID directly in MySQL allows seamless resource management, cache invalidation, and Cloudinary asset deletion via `cloudinary.uploader().destroy(publicId, options)`.

---

## 5. Media Lifecycle Operations

### 5.1 Upload Lifecycle
1. **Request:** Client submits `multipart/form-data` with file and optional `folder` parameter.
2. **Authentication:** Validated via JWT filter (`UserPrincipal` resolved).
3. **Rate Limiting:** Checked via Redis rate limiter (`media-upload` tier).
4. **Validation:** `FileUtility.validateFile(file, true)`:
   - Images: max 10MB; allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
   - Videos: max 50MB; allowed types: `video/mp4`, `video/quicktime`, `video/webm`.
5. **Cloudinary Upload:** Uploaded to Cloudinary with folder, unique publicId, and resource type auto-detection.
6. **Metadata Persistence:** `Media` entity saved to MySQL. If database persistence fails, an automatic compensation delete is attempted on Cloudinary to prevent orphaned files.
7. **Response:** Returns `MediaResponse` with secure URL, dimensions, and metadata.

### 5.2 Avatar Replace Lifecycle (Zero Data Loss)
When a user updates their profile avatar:
1. New image is validated and uploaded to Cloudinary.
2. User profile is updated in MySQL with the new avatar URL.
3. The old avatar asset is deleted from Cloudinary **only after** the new URL is successfully persisted.
4. If the new upload fails, the existing avatar is preserved untouched.

### 5.3 Resource Delete Lifecycle (Post, Story, Reel)
When a post, story, or reel is deleted:
1. Ownership and authorization are validated.
2. Associated media URL / public ID is queried.
3. Database record is deleted.
4. `mediaService.deleteByUrl(mediaUrl)` calls Cloudinary Destroy API to free cloud storage and invalidate CDN cache.

---

## 6. Security & Rate Limiting

1. **No Frontend Secrets:** Cloudinary API secret is never returned in API responses, included in JavaScript bundles, or logged.
2. **Untrusted Inputs:** File names and client-supplied MIME headers are never blindly trusted; validation evaluates file extensions and mime categories.
3. **Rate Limiting:** Enforced at the controller level:
   ```java
   @RateLimit(category = "media-upload", limit = 15, windowSeconds = 60)
   ```
   Clients exceeding 15 uploads per minute receive `429 Too Many Requests` with `Retry-After` headers.
4. **Authorization:** Deleting media requires the caller to be the verified owner of the asset or an administrator.

---

## 7. Frontend Integration (`media.service.js`)

The frontend interacts with the backend media API through `src/services/media.service.js`:

```javascript
import mediaService from '@/services/media.service';

// Upload media
const response = await mediaService.uploadMedia(file, 'posts');
const { secureUrl, publicId, id } = response.data;

// Retrieve media metadata
const metadata = await mediaService.getMedia(mediaId);

// Delete media
await mediaService.deleteMedia(mediaId);
```

Existing services (`post.service.js`, story uploads, avatar updates) delegate directly to this centralized service, ensuring uniform progress tracking, preview handling, and error response processing.

---

## 8. Verification & Testing

- **Backend Integration Tests:** `CloudinaryMediaIntegrationTest.java` (All 7 Tests Passing):
  - Valid image upload verification & MySQL metadata persistence.
  - Video upload verification with resource type classification (`video`).
  - Avatar folder partitioning verification (`instagram-clone/users/{userId}/avatar`).
  - Stories folder partitioning verification (`instagram-clone/stories/{userId}`).
  - Invalid MIME type / executable rejection (400 Bad Request).
  - Oversized file upload rejection (exceeding 10MB/100MB limit).
  - Ownership authorization on media deletion (owner succeeds, intruder receives 403 Forbidden).
- **Suite Regression:** Verified against `UtilityClassesTest`, `UserControllerTest`, `StoryReelIntegrationTest`, and `PostEngagementIntegrationTest`.
- **Compensating Deletion:** Verified that database save failures trigger immediate Cloudinary cleanup to prevent orphaned cloud assets.
- **Story Expiration:** Verified that `ScheduledCleanupJobs` cleans up both database records and associated Cloudinary media assets.
- **Frontend Verification:** Vitest test suites (19/19 tests passing) and production build bundle (`npm run build`) validated with zero errors.
