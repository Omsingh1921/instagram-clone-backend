package com.instagramclone.security;

import com.instagramclone.chat.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Date;
import java.util.UUID;

/**
 * Enforces JWT Authentication, token blacklist verification, and destination authorization on STOMP WebSocket connections.
 * Follows Section 14 (WebSocket Authentication):
 * CONNECT -> JWT & blacklist validation -> Authentication -> SUBSCRIBE (conversation membership check) -> SEND -> DISCONNECT
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        // Propagate authentication to SecurityContext for downstream STOMP handlers
        if (accessor.getUser() instanceof Authentication auth) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        SecurityContextHolder.clearContext();
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: missing or malformed Authorization header");
            throw new MessageDeliveryException("Unauthorized: Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("STOMP CONNECT rejected: invalid or expired JWT token");
            throw new MessageDeliveryException("Unauthorized: Invalid or expired JWT token");
        }

        if (tokenBlacklistService.isBlacklisted(token)) {
            log.warn("STOMP CONNECT rejected: token has been blacklisted/revoked");
            throw new MessageDeliveryException("Unauthorized: Token has been revoked");
        }

        try {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            Date issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
            UUID sessionId = jwtTokenProvider.getSessionIdFromToken(token);

            if (tokenBlacklistService.isUserRevoked(userId, issuedAt)) {
                log.warn("STOMP CONNECT rejected: user session has been revoked [userId={}]", userId);
                throw new MessageDeliveryException("Unauthorized: User session has been revoked");
            }

            if (sessionId != null && tokenBlacklistService.isSessionRevoked(sessionId)) {
                log.warn("STOMP CONNECT rejected: session has been revoked [sessionId={}]", sessionId);
                throw new MessageDeliveryException("Unauthorized: Session has been revoked");
            }

            UserDetails userDetails = userDetailsService.loadUserById(userId);
            if (userDetails instanceof UserPrincipal principal && sessionId != null) {
                userDetails = principal.withSessionId(sessionId);
            }

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                log.warn("STOMP CONNECT rejected: user account is locked/disabled [userId={}]", userId);
                throw new MessageDeliveryException("Unauthorized: User account is inactive");
            }

            if (userDetails instanceof UserPrincipal principal && !principal.isEmailVerified()) {
                log.warn("STOMP CONNECT rejected: user email is unverified [userId={}]", userId);
                throw new MessageDeliveryException("Unauthorized: Email address is not verified");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            try {
                accessor.setUser(authentication);
            } catch (IllegalStateException ise) {
                log.debug("Accessor was immutable: {}", ise.getMessage());
            }
            log.info("STOMP CONNECT authenticated successfully for user: {} [id={}]",
                    userDetails.getUsername(), userId);
        } catch (MessageDeliveryException mde) {
            throw mde;
        } catch (Exception ex) {
            log.error("STOMP CONNECT failed during authentication verification: {}", ex.getMessage());
            throw new MessageDeliveryException("Unauthorized: Authentication failed");
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("STOMP SUBSCRIBE rejected: unauthenticated session");
            throw new MessageDeliveryException("Unauthorized: Must be authenticated to subscribe");
        }

        String destination = accessor.getDestination();
        if (destination != null) {
            // Protect private user notification channels: /topic/notifications/{userId}
            if (destination.startsWith("/topic/notifications/")) {
                String targetIdStr = destination.substring("/topic/notifications/".length());
                if (principal instanceof UsernamePasswordAuthenticationToken authToken &&
                        authToken.getPrincipal() instanceof UserPrincipal userPrincipal) {
                    if (!userPrincipal.getId().toString().equalsIgnoreCase(targetIdStr)) {
                        log.warn("STOMP SUBSCRIBE forbidden: user {} tried to subscribe to notifications for {}",
                                userPrincipal.getUsername(), targetIdStr);
                        throw new MessageDeliveryException("Forbidden: Cannot subscribe to another user's notifications");
                    }
                }
            }

            // Protect private direct conversations: /topic/conversations/{conversationId} and /topic/conversations/{conversationId}/typing
            if (destination.equals("/topic/conversations") || destination.startsWith("/topic/conversations/")) {
                if (destination.equals("/topic/conversations")) {
                    log.warn("STOMP SUBSCRIBE rejected: root conversation destination not allowed");
                    throw new MessageDeliveryException("BadRequest: Invalid conversation destination");
                }
                String subPath = destination.substring("/topic/conversations/".length());
                String[] segments = subPath.split("/", -1);

                // Supported destinations:
                // 1 segment:  /topic/conversations/{conversationId} -> [conversationId]
                // 2 segments: /topic/conversations/{conversationId}/typing -> [conversationId, "typing"]
                if (segments.length == 0 || segments.length > 2 || segments[0].isEmpty()
                        || (segments.length == 2 && !"typing".equals(segments[1]))) {
                    log.warn("STOMP SUBSCRIBE rejected: invalid conversation destination pattern {}", destination);
                    throw new MessageDeliveryException("BadRequest: Invalid conversation destination");
                }

                String conversationIdStr = segments[0];
                try {
                    UUID conversationId = UUID.fromString(conversationIdStr);
                    if (principal instanceof UsernamePasswordAuthenticationToken authToken &&
                            authToken.getPrincipal() instanceof UserPrincipal userPrincipal) {
                        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(
                                conversationId, userPrincipal.getId()
                        );
                        if (!isMember) {
                            log.warn("STOMP SUBSCRIBE forbidden: user {} is not a member of conversation {}",
                                    userPrincipal.getUsername(), conversationId);
                            throw new MessageDeliveryException("Forbidden: You are not a member of this conversation");
                        }
                    } else {
                        log.warn("STOMP SUBSCRIBE unauthorized: invalid principal type for conversation");
                        throw new MessageDeliveryException("Unauthorized: Must be authenticated to subscribe");
                    }
                } catch (IllegalArgumentException ex) {
                    log.warn("STOMP SUBSCRIBE rejected: malformed conversation ID {}", conversationIdStr);
                    throw new MessageDeliveryException("BadRequest: Invalid conversation ID");
                }
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("STOMP SEND rejected: unauthenticated session");
            throw new MessageDeliveryException("Unauthorized: Must be authenticated to send messages");
        }
    }
}
