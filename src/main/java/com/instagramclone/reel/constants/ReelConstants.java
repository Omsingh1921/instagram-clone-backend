package com.instagramclone.reel.constants;

/**
 * Domain constants for the Reel video module.
 */
public final class ReelConstants {

    private ReelConstants() {
        // Prevent instantiation
    }

    public static final int MIN_DURATION_SECONDS = 3;
    public static final int MAX_DURATION_SECONDS = 90;
    public static final int MAX_CAPTION_LENGTH = 2200;
    public static final int MAX_AUDIO_TITLE_LENGTH = 100;
    public static final String DEFAULT_AUDIO_TITLE = "Original Audio";

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_USER_REELS_PAGE_SIZE = 12;
    public static final int DEFAULT_COMMENTS_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 30;

    public static final String CACHE_REEL_PREFIX = "reel:";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1";
    public static final String REELS_PATH = "/reels";
    public static final String REEL_BY_ID_PATH = "/reels/{reelId}";
    public static final String USER_REELS_PATH = "/users/{userId}/reels";
    public static final String REEL_COMMENTS_PATH = "/reels/{reelId}/comments";
    public static final String REEL_LIKE_PATH = "/reels/{reelId}/like";
    public static final String REEL_LIKES_PATH = "/reels/{reelId}/likes";
    public static final String REEL_VIEW_PATH = "/reels/{reelId}/view";
    public static final String REEL_VIEWS_PATH = "/reels/{reelId}/views";
    public static final String REEL_COMMENT_BY_ID_PATH = "/reels/comments/{commentId}";
    public static final String REEL_COMMENT_WITH_REEL_ID_PATH = "/reels/{reelId}/comments/{commentId}";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_CONTENT_CREATE = "content-create";
    public static final String RATE_LIMIT_CONTENT_ENGAGEMENT = "content-engagement";
    public static final String RATE_LIMIT_CONTENT_COMMENT = "content-comment";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_REEL_CREATED = "Reel created successfully";
    public static final String MSG_REELS_RETRIEVED = "Reels retrieved successfully";
    public static final String MSG_REEL_RETRIEVED = "Reel retrieved successfully";
    public static final String MSG_USER_REELS_RETRIEVED = "User reels retrieved successfully";
    public static final String MSG_REEL_DELETED = "Reel deleted successfully";
    public static final String MSG_REEL_LIKED = "Reel liked successfully";
    public static final String MSG_REEL_UNLIKED = "Reel unliked successfully";
    public static final String MSG_REEL_LIKES_RETRIEVED = "Reel likes retrieved successfully";
    public static final String MSG_COMMENT_ADDED = "Comment added to reel successfully";
    public static final String MSG_COMMENTS_RETRIEVED = "Reel comments retrieved successfully";
    public static final String MSG_COMMENT_DELETED = "Reel comment deleted successfully";
    public static final String MSG_VIEW_RECORDED = "Reel view recorded";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_REEL_NOT_FOUND = "Reel not found with ID: ";
    public static final String ERR_UNAUTHORIZED_DELETE = "You are not authorized to delete this reel";
    public static final String ERR_COMMENT_NOT_FOUND = "Comment not found with ID: ";
    public static final String ERR_UNAUTHORIZED_COMMENT_DELETE = "You are not authorized to delete this comment";
    public static final String ERR_CURRENT_USER_NOT_FOUND = "Current user not found";
    public static final String MSG_INVALID_REEL_DURATION = "Reel duration must be between 3 and 90 seconds";
}
