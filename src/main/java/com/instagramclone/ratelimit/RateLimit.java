package com.instagramclone.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce Redis-backed distributed rate limiting on controller methods or classes.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Category name for grouping limits (e.g., "auth-login", "content-create", "search").
     */
    String category() default "default";

    /**
     * Maximum number of allowed requests in the time window (override if > 0).
     */
    int limit() default -1;

    /**
     * Time window in seconds for the rate limit (override if > 0).
     */
    int windowSeconds() default -1;
}
