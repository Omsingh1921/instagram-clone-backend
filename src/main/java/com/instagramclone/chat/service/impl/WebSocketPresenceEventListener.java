package com.instagramclone.chat.service.impl;

import com.instagramclone.chat.service.interfaces.*;

import com.instagramclone.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

/**
 * Bridges WebSocket session lifecycle events with Redis IPresenceService.
 */
@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPresenceEventListener.class);

    private final IPresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            UUID userId = userPrincipal.getId();
            presenceService.userConnected(userId, sessionId);
            log.debug("Registered presence connect for user {} [session={}]", userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        UUID userId = null;
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            userId = userPrincipal.getId();
        }

        presenceService.userDisconnected(userId, sessionId);
        log.debug("Registered presence disconnect for session {} [userId={}]", sessionId, userId);
    }
}
