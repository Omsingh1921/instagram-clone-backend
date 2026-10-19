package com.instagramclone.search.constants;

/**
 * Domain constants for user, hashtag, and explore search.
 */
public final class SearchConstants {

    private SearchConstants() {
        // Prevent instantiation
    }

    public static final int MIN_QUERY_LENGTH = 1;
    public static final int MAX_QUERY_LENGTH = 100;
    public static final int DEFAULT_SEARCH_LIMIT = 20;
    public static final int MAX_SEARCH_LIMIT = 50;

    public static final String CACHE_SEARCH_PREFIX = "search:recent:";

    // ==================== QUERY PARAMETERS ====================
    public static final String PARAM_Q = "q";
    public static final String PARAM_QUERY = "query";
    public static final String DEFAULT_EMPTY_QUERY = "";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/search";
    public static final String USERS_SEARCH_PATH = "/users";
    public static final String POSTS_SEARCH_PATH = "/posts";
    public static final String EXPLORE_PATH = "/explore";
    public static final String TAGS_SEARCH_PATH = "/tags";
    public static final String TAG_POSTS_PATH = "/tags/{tag}/posts";
    public static final String TRENDING_TAGS_PATH = "/tags/trending";
    public static final String RECENT_SEARCH_PATH = "/recent";
    public static final String RECENT_SEARCH_BY_ID_PATH = "/recent/{id}";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_SEARCH = "search";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_USER_SEARCH_RESULTS = "User search results retrieved";
    public static final String MSG_POST_SEARCH_RESULTS = "Post search results retrieved";
    public static final String MSG_EXPLORE_RETRIEVED = "Explore feed retrieved successfully";
    public static final String MSG_HASHTAGS_RETRIEVED = "Hashtags retrieved successfully";
    public static final String MSG_RECENT_SEARCHES_RETRIEVED = "Recent searches retrieved successfully";
    public static final String MSG_SEARCH_RECORDED = "Search query recorded successfully";
    public static final String MSG_SEARCH_CLEARED = "Recent search cleared successfully";
    public static final String MSG_INVALID_SEARCH_QUERY = "Search query must be between 1 and 100 characters";
}
