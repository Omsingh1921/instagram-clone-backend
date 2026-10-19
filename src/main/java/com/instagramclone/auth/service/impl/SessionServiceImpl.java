package com.instagramclone.auth.service.impl;

import com.instagramclone.auth.service.interfaces.ISessionService;
import com.instagramclone.user.service.interfaces.*;

import com.instagramclone.audit.service.interfaces.IAuditLogService;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.security.JwtProperties;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.auth.constants.SessionConstants;
import com.instagramclone.auth.dto.response.SessionResponse;
import com.instagramclone.auth.entity.RefreshToken;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements ISessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ITokenBlacklistService tokenBlacklistService;
    private final IAuditLogService auditLogService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID currentSessionId = SecurityUtils.getCurrentSessionId().orElse(null);
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActivityAtDesc(currentUserId, now);

        // Deduplicate by sessionId, retaining the most recent active token per session
        Map<UUID, RefreshToken> latestSessionMap = new LinkedHashMap<>();
        for (RefreshToken token : activeTokens) {
            UUID sid = token.getSessionId();
            if (sid != null && !latestSessionMap.containsKey(sid)) {
                latestSessionMap.put(sid, token);
            }
        }

        List<SessionResponse> responses = new ArrayList<>();
        for (RefreshToken token : latestSessionMap.values()) {
            boolean isCurrent = currentSessionId != null && currentSessionId.equals(token.getSessionId());
            responses.add(SessionResponse.builder()
                    .id(token.getSessionId())
                    .deviceName(token.getDeviceName() != null ? token.getDeviceName() : "Web Browser (Direct Client)")
                    .ipAddress(maskIp(token.getIpAddress()))
                    .createdAt(token.getCreatedAt())
                    .lastActivityAt(token.getLastActivityAt())
                    .expiresAt(token.getExpiresAt())
                    .currentSession(isCurrent)
                    .build());
        }

        return responses;
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BadRequestException(SessionConstants.ERR_SESSION_ID_NULL);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        boolean existsAndOwned = refreshTokenRepository.existsBySessionIdAndUserId(sessionId, currentUserId);
        if (!existsAndOwned) {
            log.warn("Unauthorized or nonexistent session revocation attempt: session [{}] for user [{}]",
                    sessionId, currentUserId);
            throw new ResourceNotFoundException(SessionConstants.ERR_SESSION_NOT_FOUND_ID + sessionId);
        }

        // Atomically revoke all refresh tokens associated with this session
        refreshTokenRepository.revokeSession(sessionId, currentUserId, now);

        // Blacklist session ID in Redis and local fallback
        tokenBlacklistService.blacklistSession(sessionId, jwtProperties.getAccessExpirationMs());

        log.info("Successfully revoked session [{}] for user [{}]", sessionId, currentUserId);
        auditLogService.record(currentUserId, "SESSION_REVOKED", "SESSION", currentUserId, null,
                "Revoked session: " + sessionId);
    }

    @Override
    @Transactional
    public int revokeOtherSessions() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID currentSessionId = SecurityUtils.getCurrentSessionId()
                .orElseThrow(() -> new BadRequestException(SessionConstants.ERR_CURRENT_SESSION_NOT_VERIFIED));
        LocalDateTime now = LocalDateTime.now();

        // Find all other active sessions belonging to this user
        List<RefreshToken> allTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(currentUserId);
        Set<UUID> otherSessionIds = new HashSet<>();
        for (RefreshToken t : allTokens) {
            if (t.getSessionId() != null && !t.getSessionId().equals(currentSessionId)) {
                otherSessionIds.add(t.getSessionId());
            }
        }

        // Blacklist each other session ID in Redis / memory
        for (UUID otherSid : otherSessionIds) {
            tokenBlacklistService.blacklistSession(otherSid, jwtProperties.getAccessExpirationMs());
        }

        int revokedTokens = refreshTokenRepository.revokeOtherSessions(currentUserId, currentSessionId, now);

        log.info("Revoked {} other session records across {} sessions for user [{}]",
                revokedTokens, otherSessionIds.size(), currentUserId);
        auditLogService.record(currentUserId, "OTHER_SESSIONS_REVOKED", "USER", currentUserId, null,
                "Revoked all other active sessions (" + otherSessionIds.size() + " sessions)");

        return otherSessionIds.size();
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "Unknown";
        }
        String trimmed = ip.trim();
        if ("127.0.0.1".equals(trimmed) || "::1".equals(trimmed) || "0:0:0:0:0:0:0:1".equals(trimmed)) {
            return "127.0.0.***";
        }
        if (trimmed.contains(".")) {
            int lastDot = trimmed.lastIndexOf('.');
            if (lastDot > 0) {
                return trimmed.substring(0, lastDot) + ".***";
            }
        } else if (trimmed.contains(":")) {
            String[] segments = trimmed.split(":");
            if (segments.length >= 3) {
                return segments[0] + ":" + segments[1] + ":" + segments[2] + ":***";
            }
        }
        return "***";
    }
}
