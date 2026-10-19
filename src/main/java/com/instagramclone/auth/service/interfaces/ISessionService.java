package com.instagramclone.auth.service.interfaces;

import com.instagramclone.auth.dto.response.SessionResponse;

import java.util.List;
import java.util.UUID;

public interface ISessionService {

    /**
     * Lists active logical sessions for the authenticated caller.
     *
     * @return list of active sessions with device and activity metadata
     */
    List<SessionResponse> getActiveSessions();

    /**
     * Revokes a specific session belonging to the authenticated user.
     *
     * @param sessionId the session UUID
     */
    void revokeSession(UUID sessionId);

    /**
     * Revokes all active sessions for the authenticated caller except the current session.
     *
     * @return number of sessions revoked
     */
    int revokeOtherSessions();
}
