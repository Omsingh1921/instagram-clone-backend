package com.instagramclone.feed.constants;

/**
 * Domain constants for user Home Feed and Explore algorithmic Feed.
 */
public final class FeedConstants {

    private FeedConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_FEED_SIZE = 15;
    public static final int DEFAULT_HOME_PAGE_SIZE = 10;
    public static final int DEFAULT_EXPLORE_PAGE_SIZE = 20;
    public static final int MAX_FEED_SIZE = 50;
    public static final double EXPLORE_REEL_RATIO = 0.35; // 35% reels in explore feed
    public static final int FEED_CACHE_TTL_MINUTES = 10;

    public static final String CACHE_HOME_FEED_PREFIX = "feed:home:";
    public static final String CACHE_EXPLORE_FEED_KEY = "feed:explore";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/feed";
    public static final String EXPLORE_PATH = "/explore";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_HOME_FEED_RETRIEVED = "Home feed retrieved successfully";
    public static final String MSG_EXPLORE_FEED_RETRIEVED = "Explore feed retrieved successfully";
}
