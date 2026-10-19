package com.instagramclone.media.constants;

import java.util.Set;

/**
 * Domain constants for media storage limits, allowed MIME types, and dimensions.
 */
public final class MediaConstants {

    private MediaConstants() {
        // Prevent instantiation
    }

    public static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    public static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024; // 100 MB

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    public static final int MAX_FILENAME_LENGTH = 255;

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/media";
    public static final String MEDIA_BY_ID_PATH = "/{mediaId}";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_MEDIA_UPLOAD = "media-upload";

    // ==================== REQUEST PARAMETERS & DEFAULTS ====================
    public static final String PARAM_FILE = "file";
    public static final String PARAM_CATEGORY = "category";
    public static final String DEFAULT_CATEGORY = "posts";

    // ==================== SWAGGER & OPENAPI TAGS ====================
    public static final String TAG_MEDIA = "Media";
    public static final String TAG_MEDIA_DESCRIPTION = "Cloudinary Media Upload & Lifecycle Management";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_MEDIA_UPLOADED = "Media uploaded successfully";
    public static final String MSG_MEDIA_DELETED = "Media deleted successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_MEDIA_NOT_FOUND = "Media not found: ";
    public static final String ERR_FILE_EMPTY = "Uploaded file cannot be empty";
    public static final String ERR_PROCESS_FILE_BYTES = "Failed to process uploaded file bytes";
    public static final String ERR_UNAUTHORIZED_DELETE = "You are not authorized to delete this media asset";
    public static final String MSG_INVALID_MEDIA_URL = "Media URL is invalid or exceeds permissible length";
}
