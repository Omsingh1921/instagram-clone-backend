package com.instagramclone.security;

import java.util.Date;
import java.util.UUID;

/**
 * Contract for managing stateless JWT revocation and user session blacklisting.
 */
public interface ITokenBlacklistService {

    /**
     * Blacklists an individual access token until its expiration.
     *
     * @param token the raw JWT token string
     * @param ttlMillis time-to-live in milliseconds
     */
    void blacklistToken(String token, long ttlMillis);

    /**
     * Checks if an individual token has been revoked.
     *
     * @param token the raw JWT token string
     * @return true if blacklisted, false otherwise
     */
    boolean isBlacklisted(String token);

    /**
     * Invalidates all active sessions for a user issued before or at revocation time.
     *
     * @param userId user identifier
     * @param durationMillis duration to preserve the revocation marker
     */
    void blacklistUser(UUID userId, long durationMillis);

    /**
     * Checks if a user's tokens issued before a revocation timestamp are invalid.
     *
     * @param userId user identifier
     * @param issuedAt token issue timestamp
     * @return true if revoked, false otherwise
     */
    boolean isUserRevoked(UUID userId, Date issuedAt);

    /**
     * Blacklists an individual session ID until its access tokens have expired.
     *
     * @param sessionId the stable session UUID
     * @param durationMillis duration to preserve the session revocation marker
     */
    void blacklistSession(UUID sessionId, long durationMillis);

    /**
     * Checks if a session has been revoked.
     *
     * @param sessionId the session UUID
     * @return true if revoked, false otherwise
     */
    boolean isSessionRevoked(UUID sessionId);

    /**
     * Clears user revocation blacklist upon account reactivation or reinstatement.
     *
     * @param userId user identifier
     */
    void clearUserBlacklist(UUID userId);
}
