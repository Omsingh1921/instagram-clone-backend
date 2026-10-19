package com.instagramclone.chat.service.interfaces;

import com.instagramclone.chat.dto.response.PresenceResponse;

import java.util.UUID;

public interface IPresenceService {

    void userConnected(UUID userId, String sessionId);

    void userDisconnected(UUID userId, String sessionId);

    PresenceResponse getPresence(UUID userId);

    void setTyping(UUID conversationId, UUID userId, boolean typing);

    boolean isTyping(UUID conversationId, UUID userId);
 
    void heartbeat(UUID userId, String sessionId);

    void cleanupStaleSessions(UUID userId);
}
