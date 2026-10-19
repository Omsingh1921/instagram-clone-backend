package com.instagramclone.chat.service.impl;

import com.instagramclone.chat.service.interfaces.*;

import com.instagramclone.chat.dto.response.PresenceResponse;
import com.instagramclone.chat.dto.TypingEvent;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production-grade Redis Presence & Typing Service adhering to Section 4.G.
 * Supports multi-device presence leases, auto-expiring typing indicators,
 * and resilient in-memory fallback.
 */
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements IPresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceImpl.class);

    private final RedisUtility redisUtility;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    // Resilient fallback storage when Redis is unavailable
    private final ConcurrentHashMap<UUID, Set<String>> fallbackSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> fallbackLastSeen = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> fallbackTyping = new ConcurrentHashMap<>();

    private static final Duration SESSION_LEASE = Duration.ofHours(2);
    private static final Duration LAST_SEEN_TTL = Duration.ofDays(30);
    private static final Duration TYPING_TTL = Duration.ofSeconds(5);

    @Override
    public void userConnected(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) return;

        boolean wasOnline = isUserOnline(userId);

        if (redisUtility.isAvailable()) {
            String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":sessions";
            String sessionMappingKey = "presence:session:" + sessionId;
            String statusKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":status";

            redisUtility.sAdd(sessionsKey, sessionId, SESSION_LEASE);
            redisUtility.set(sessionMappingKey, userId.toString(), SESSION_LEASE);
            redisUtility.set(statusKey, "online", SESSION_LEASE);
        } else {
            fallbackSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }

        log.debug("User [{}] connected session [{}]. Was online before: {}", userId, sessionId, wasOnline);

        if (!wasOnline) {
            broadcastPresence(userId, true, null);
        }
    }

    @Override
    public void userDisconnected(UUID userId, String sessionId) {
        if (sessionId == null) return;

        UUID targetUserId = userId;
        if (targetUserId == null && redisUtility.isAvailable()) {
            Optional<String> mappedUser = redisUtility.get("presence:session:" + sessionId);
            if (mappedUser.isPresent()) {
                try {
                    targetUserId = UUID.fromString(mappedUser.get());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (targetUserId == null) {
            // Check fallback session map
            for (var entry : fallbackSessions.entrySet()) {
                if (entry.getValue().contains(sessionId)) {
                    targetUserId = entry.getKey();
                    break;
                }
            }
        }

        if (targetUserId == null) return;

        boolean isStillOnline;
        String now = Instant.now().toString();

        if (redisUtility.isAvailable()) {
            String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + targetUserId + ":sessions";
            String sessionMappingKey = "presence:session:" + sessionId;
            String statusKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + targetUserId + ":status";
            String lastSeenKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + targetUserId + ":last_seen";

            redisUtility.sRem(sessionsKey, sessionId);
            redisUtility.delete(sessionMappingKey);

            Long remainingSessions = redisUtility.sCard(sessionsKey);
            isStillOnline = remainingSessions != null && remainingSessions > 0;

            if (!isStillOnline) {
                redisUtility.set(statusKey, "offline", LAST_SEEN_TTL);
                redisUtility.set(lastSeenKey, now, LAST_SEEN_TTL);
            }
        } else {
            Set<String> sessions = fallbackSessions.get(targetUserId);
            if (sessions != null) {
                sessions.remove(sessionId);
                isStillOnline = !sessions.isEmpty();
            } else {
                isStillOnline = false;
            }
            if (!isStillOnline) {
                fallbackLastSeen.put(targetUserId, now);
            }
        }

        log.debug("User [{}] disconnected session [{}]. Still online: {}", targetUserId, sessionId, isStillOnline);

        if (!isStillOnline) {
            broadcastPresence(targetUserId, false, now);
        }
    }

    @Override
    public PresenceResponse getPresence(UUID userId) {
        if (userId == null) {
            return PresenceResponse.builder().online(false).build();
        }

        boolean online = isUserOnline(userId);
        String lastSeen = null;

        if (!online) {
            if (redisUtility.isAvailable()) {
                String lastSeenKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":last_seen";
                lastSeen = redisUtility.get(lastSeenKey).orElse(null);
            } else {
                lastSeen = fallbackLastSeen.get(userId);
            }
        }

        return PresenceResponse.builder()
                .userId(userId)
                .online(online)
                .lastSeen(lastSeen)
                .build();
    }

    @Override
    public void setTyping(UUID conversationId, UUID userId, boolean typing) {
        if (conversationId == null || userId == null) return;

        String typingKey = CacheConstants.CACHE_TYPING_PREFIX + conversationId + ":" + userId;

        if (typing) {
            if (redisUtility.isAvailable()) {
                redisUtility.set(typingKey, "typing", TYPING_TTL);
            } else {
                fallbackTyping.put(typingKey, System.currentTimeMillis() + 5000L);
            }
        } else {
            if (redisUtility.isAvailable()) {
                redisUtility.delete(typingKey);
            } else {
                fallbackTyping.remove(typingKey);
            }
        }

        // Broadcast typing state to conversation participants
        if (messagingTemplate != null) {
            try {
                TypingEvent event = TypingEvent.builder()
                        .conversationId(conversationId)
                        .userId(userId)
                        .typing(typing)
                        .build();
                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing", event);
            } catch (Exception ex) {
                log.warn("Failed to broadcast typing event: {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean isTyping(UUID conversationId, UUID userId) {
        if (conversationId == null || userId == null) return false;
        String typingKey = CacheConstants.CACHE_TYPING_PREFIX + conversationId + ":" + userId;

        if (redisUtility.isAvailable()) {
            return redisUtility.hasKey(typingKey);
        }

        Long expiry = fallbackTyping.get(typingKey);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    @Override
    public void heartbeat(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) return;
        if (redisUtility.isAvailable()) {
            String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":sessions";
            String sessionMappingKey = "presence:session:" + sessionId;
            String statusKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":status";

            redisUtility.sAdd(sessionsKey, sessionId, SESSION_LEASE);
            redisUtility.set(sessionMappingKey, userId.toString(), SESSION_LEASE);
            redisUtility.set(statusKey, "online", SESSION_LEASE);
        } else {
            fallbackSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    @Override
    public void cleanupStaleSessions(UUID userId) {
        if (userId == null) return;
        if (redisUtility.isAvailable()) {
            String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":sessions";
            Set<String> activeSessions = redisUtility.sMembers(sessionsKey);
            boolean modified = false;
            for (String sId : activeSessions) {
                if (!redisUtility.hasKey("presence:session:" + sId)) {
                    redisUtility.sRem(sessionsKey, sId);
                    modified = true;
                }
            }
            if (modified) {
                Long remaining = redisUtility.sCard(sessionsKey);
                if (remaining == null || remaining == 0) {
                    String statusKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":status";
                    String lastSeenKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":last_seen";
                    String now = Instant.now().toString();
                    redisUtility.set(statusKey, "offline", LAST_SEEN_TTL);
                    redisUtility.set(lastSeenKey, now, LAST_SEEN_TTL);
                    broadcastPresence(userId, false, now);
                }
            }
        }
    }

    private boolean isUserOnline(UUID userId) {
        if (redisUtility.isAvailable()) {
            cleanupStaleSessions(userId);
            String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":sessions";
            Long sessionsCount = redisUtility.sCard(sessionsKey);
            return sessionsCount != null && sessionsCount > 0;
        }

        Set<String> sessions = fallbackSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    private void broadcastPresence(UUID userId, boolean online, String lastSeen) {
        if (messagingTemplate == null) return;
        try {
            PresenceResponse response = PresenceResponse.builder()
                    .userId(userId)
                    .online(online)
                    .lastSeen(lastSeen)
                    .build();
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/presence", response);
        } catch (Exception ex) {
            log.warn("Failed to broadcast presence update: {}", ex.getMessage());
        }
    }
}
