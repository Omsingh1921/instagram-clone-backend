package com.instagramclone.ratelimit;

import com.instagramclone.common.exception.RateLimitExceededException;
import com.instagramclone.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final IRateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final com.instagramclone.security.ClientIpResolver clientIpResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Look for @RateLimit on method or class
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null) {
            return true;
        }

        String category = rateLimit.category();
        RateLimitProperties.CategoryLimit configLimit = rateLimitProperties.getCategoryLimit(category);

        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : configLimit.getLimit();
        int windowSeconds = rateLimit.windowSeconds() > 0 ? rateLimit.windowSeconds() : configLimit.getWindowSeconds();

        String clientIp = clientIpResolver.resolveClientIp(request);
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null);

        // Key building strategy
        String key;
        if (currentUserId != null) {
            key = String.format("rate_limit:%s:user:%s", category, currentUserId);
        } else {
            key = String.format("rate_limit:%s:ip:%s", category, clientIp);
        }

        RateLimitResult result = rateLimiterService.checkRateLimit(key, limit, windowSeconds);

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for [{}] on endpoint [{} {}]. Limit: {}, Count: {}, RetryAfter: {}s",
                    key, request.getMethod(), request.getRequestURI(), limit, result.getCurrentCount(), result.getRetryAfterSeconds());

            throw new RateLimitExceededException(
                    String.format("Too many requests for %s. Please try again in %d seconds.",
                            category, result.getRetryAfterSeconds()),
                    result.getRetryAfterSeconds(),
                    result.getLimit()
            );
        }

        // Inject standard RateLimit headers on allowed requests
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));

        return true;
    }
}
