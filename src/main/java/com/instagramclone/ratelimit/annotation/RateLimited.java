package com.instagramclone.ratelimit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation configuring rate limiting parameters for a controller endpoint.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {

    int maxRequests() default 100;

    int windowSeconds() default 60;

    String keyPrefix() default "api";
}
