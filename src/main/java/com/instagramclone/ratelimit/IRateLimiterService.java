package com.instagramclone.ratelimit;

public interface IRateLimiterService {

    /**
     * Atomically checks and increments the rate limit counter for the given key.
     *
     * @param key           Redis cache key identifying the client/resource
     * @param limit         Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return RateLimitResult indicating whether the request is allowed, remaining tokens, and TTL
     */
    RateLimitResult checkRateLimit(String key, int limit, int windowSeconds);
}
