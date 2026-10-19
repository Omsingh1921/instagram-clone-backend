package com.instagramclone.ratelimit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketRateLimitInterceptor.class);

    private final IRateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;

    private static final String WS_CATEGORY = "messaging";
    private static final int WS_DEFAULT_LIMIT = 60; // 60 messages per 60 seconds
    private static final int WS_DEFAULT_WINDOW = 60;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (!rateLimitProperties.isEnabled()) {
            return message;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.SEND.equals(command)) {
            Principal user = accessor.getUser();
            String clientKey;
            if (user != null && user.getName() != null) {
                clientKey = "rate_limit:ws:user:" + user.getName();
            } else {
                String sessionId = accessor.getSessionId();
                clientKey = "rate_limit:ws:session:" + (sessionId != null ? sessionId : "anon");
            }

            RateLimitProperties.CategoryLimit limitConfig = rateLimitProperties.getCategoryLimit(WS_CATEGORY);
            int limit = limitConfig != null ? limitConfig.getLimit() : WS_DEFAULT_LIMIT;
            int window = limitConfig != null ? limitConfig.getWindowSeconds() : WS_DEFAULT_WINDOW;

            RateLimitResult result = rateLimiterService.checkRateLimit(clientKey, limit, window);

            if (!result.isAllowed()) {
                log.warn("WebSocket message rate limit exceeded for key [{}]. Limit: {}, Retry-After: {}s",
                        clientKey, limit, result.getRetryAfterSeconds());
                throw new MessageDeliveryException(
                        "Rate limit exceeded for real-time messaging. Please wait " + result.getRetryAfterSeconds() + " seconds."
                );
            }
        }

        return message;
    }
}
