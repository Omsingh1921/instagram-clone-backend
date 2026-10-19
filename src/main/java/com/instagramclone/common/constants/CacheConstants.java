package com.instagramclone.common.constants;

/**
 * Shared Redis cache keys and prefixes adhering to Section 50.4 (Common Constants).
 */
public final class CacheConstants {

    private CacheConstants() {}

    public static final String CACHE_USER_PROFILE = "user-profile";
    public static final String CACHE_USER_PROFILE_PREFIX = "cache:user:profile:";
    public static final String CACHE_UNREAD_NOTIFICATIONS_PREFIX = "cache:notifications:unread:";
    public static final String CACHE_RATE_LIMIT_PREFIX = "rate_limit:";
    public static final String CACHE_LOCK_JOB_PREFIX = "lock:job:";
    public static final String CACHE_PRESENCE_USER_PREFIX = "presence:user:";
    public static final String CACHE_TYPING_PREFIX = "chat:typing:";
    public static final String CACHE_COUNT_POST_LIKES_PREFIX = "count:post:likes:";
    public static final String CACHE_COUNT_POST_COMMENTS_PREFIX = "count:post:comments:";
    public static final String CACHE_COUNT_REEL_LIKES_PREFIX = "count:reel:likes:";
    public static final String CACHE_COUNT_REEL_COMMENTS_PREFIX = "count:reel:comments:";
    public static final String CACHE_COUNT_REEL_VIEWS_PREFIX = "count:reel:views:";
    public static final String CACHE_IDEMPOTENCY_EVENT_PREFIX = "idempotency:event:";
}
