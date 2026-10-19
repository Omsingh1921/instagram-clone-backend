package com.instagramclone.ratelimit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RedisRateLimiterService implements IRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);

    private final StringRedisTemplate stringRedisTemplate;

    // Redis Lua script for atomic sliding/fixed window rate limit with TTL
    private static final String RATE_LIMIT_LUA =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "end\n" +
            "local ttl = redis.call('TTL', KEYS[1])\n" +
            "if ttl < 0 then\n" +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "    ttl = tonumber(ARGV[1])\n" +
            "end\n" +
            "return {current, ttl}";

    private static final DefaultRedisScript<List> SCRIPT =
            new DefaultRedisScript<>(RATE_LIMIT_LUA, List.class);

    /**
     * In-memory fallback if Redis is temporarily unreachable.
     *
     * RESILIENCE NOTE & MULTI-INSTANCE LIMITATION:
     * This fallback bucket cache is scoped to this specific JVM instance. It guarantees thread-safe
     * local rate limiting to protect the individual server during transient Redis outages, but does
     * NOT provide distributed protection across multiple backend instances. When running in a clustered
     * deployment while Redis is down, effective system-wide limits will be multiplied by the number of
     * active instances.
     */
    private final ConcurrentHashMap<String, InMemoryBucket> fallbackCache = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult checkRateLimit(String key, int limit, int windowSeconds) {
        try {
            List<Long> result = stringRedisTemplate.execute(
                    SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds)
            );

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                long ttlSeconds = ((Number) result.get(1)).longValue();

                boolean allowed = currentCount <= limit;
                long remaining = Math.max(0, limit - currentCount);
                long retryAfter = allowed ? 0 : Math.max(1, ttlSeconds);

                return RateLimitResult.builder()
                        .allowed(allowed)
                        .currentCount(currentCount)
                        .limit(limit)
                        .remaining(remaining)
                        .retryAfterSeconds(retryAfter)
                        .resetSeconds(ttlSeconds)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable during rate limit check for key [{}]: {}. Using in-memory fallback.",
                    key, ex.getMessage());
            return checkFallback(key, limit, windowSeconds);
        }

        // Default allow if script returns null
        return RateLimitResult.builder()
                .allowed(true)
                .currentCount(1)
                .limit(limit)
                .remaining(limit - 1)
                .retryAfterSeconds(0)
                .resetSeconds(windowSeconds)
                .build();
    }

    private RateLimitResult checkFallback(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;

        InMemoryBucket bucket = fallbackCache.compute(key, (k, existing) -> {
            if (existing == null || now > existing.expiry) {
                return new InMemoryBucket(now + windowMillis, new AtomicLong(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        long currentCount = bucket.count.get();
        long ttlSeconds = Math.max(1, (bucket.expiry - now) / 1000L);
        boolean allowed = currentCount <= limit;
        long remaining = Math.max(0, limit - currentCount);
        long retryAfter = allowed ? 0 : ttlSeconds;

        return RateLimitResult.builder()
                .allowed(allowed)
                .currentCount(currentCount)
                .limit(limit)
                .remaining(remaining)
                .retryAfterSeconds(retryAfter)
                .resetSeconds(ttlSeconds)
                .build();
    }

    public void clearLimits() {
        fallbackCache.clear();
        if (stringRedisTemplate != null) {
            try {
                java.util.Set<String> keys = stringRedisTemplate.keys("rate_limit:*");
                if (keys != null && !keys.isEmpty()) {
                    stringRedisTemplate.delete(keys);
                }
            } catch (Exception ex) {
                log.warn("Error clearing rate limit keys in Redis: {}", ex.getMessage());
            }
        }
    }

    private static class InMemoryBucket {
        final long expiry;
        final AtomicLong count;

        InMemoryBucket(long expiry, AtomicLong count) {
            this.expiry = expiry;
            this.count = count;
        }
    }
}
