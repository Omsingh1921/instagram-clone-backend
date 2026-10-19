package com.instagramclone.post.constants;

/**
 * Domain constants for the Post module.
 */
public final class PostConstants {

    private PostConstants() {
        // Prevent instantiation
    }

    public static final int MAX_CAPTION_LENGTH = 2200;
    public static final int MIN_MEDIA_COUNT = 1;
    public static final int MAX_MEDIA_COUNT = 10;
    public static final int MAX_HASHTAGS_PER_POST = 30;
    public static final int MAX_USER_TAGS_PER_POST = 20;

    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 50;

    public static final int MAX_LOCATION_NAME_LENGTH = 150;
    public static final String CACHE_POST_PREFIX = "post:";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1";
    public static final String POSTS_PATH = "/posts";
    public static final String SAVED_POSTS_PATH = "/posts/saved";
    public static final String POST_BY_ID_PATH = "/posts/{postId}";
    public static final String USER_POSTS_PATH = "/users/{userId}/posts";
    public static final String USER_TAGGED_POSTS_PATH = "/users/{userId}/posts/tagged";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_CONTENT_CREATE = "content-create";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_POST_CREATED = "Post created successfully";
    public static final String MSG_SAVED_POSTS_RETRIEVED = "Saved posts retrieved successfully";
    public static final String MSG_POST_RETRIEVED = "Post retrieved successfully";
    public static final String MSG_USER_POSTS_RETRIEVED = "User posts retrieved successfully";
    public static final String MSG_USER_TAGGED_POSTS_RETRIEVED = "User tagged posts retrieved successfully";
    public static final String MSG_POST_UPDATED = "Post updated successfully";
    public static final String MSG_POST_DELETED = "Post deleted successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_POST_NOT_FOUND = "Post not found with ID: ";
    public static final String ERR_AUTHOR_NOT_FOUND = "Author not found with ID: ";
    public static final String ERR_MEDIA_REQUIRED = "At least one media item is required to create a post";
    public static final String ERR_USER_NOT_FOUND = "User not found with ID: ";
    public static final String ERR_UNAUTHORIZED_UPDATE = "You are not authorized to update this post";
    public static final String ERR_UNAUTHORIZED_DELETE = "You are not authorized to delete this post";
    public static final String MSG_INVALID_CAPTION = "Caption cannot exceed 2200 characters";
}
