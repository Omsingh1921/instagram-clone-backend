package com.instagramclone.auth.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.auth.constants.SessionConstants;
import com.instagramclone.auth.dto.response.SessionResponse;
import com.instagramclone.auth.service.interfaces.ISessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(SessionConstants.SESSIONS_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = SessionConstants.TAG_SESSION_MANAGEMENT, description = SessionConstants.TAG_SESSION_MANAGEMENT_DESCRIPTION)
public class SessionController {

    private final ISessionService sessionService;

    @RateLimit(category = SessionConstants.RATE_LIMIT_SESSION_LIST, limit = 30, windowSeconds = 60)
    @GetMapping
    @Operation(summary = "List active sessions", description = "Returns active login sessions for the authenticated user")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions() {
        List<SessionResponse> sessions = sessionService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(SessionConstants.MSG_ACTIVE_SESSIONS_RETRIEVED, sessions));
    }

    @RateLimit(category = SessionConstants.RATE_LIMIT_SESSION_REVOKE, limit = 10, windowSeconds = 60)
    @DeleteMapping(SessionConstants.PATH_SESSION_BY_ID)
    @Operation(summary = "Revoke single session", description = "Revokes the specified session belonging to the authenticated user")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID sessionId) {
        sessionService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(SessionConstants.MSG_SESSION_REVOKED_SUCCESS, null));
    }

    @RateLimit(category = SessionConstants.RATE_LIMIT_SESSION_REVOKE_OTHER, limit = 5, windowSeconds = 60)
    @DeleteMapping(SessionConstants.PATH_SESSIONS_OTHER)
    @Operation(summary = "Revoke other sessions", description = "Revokes all sessions for the authenticated user except the current session")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> revokeOtherSessions() {
        int count = sessionService.revokeOtherSessions();
        return ResponseEntity.ok(ApiResponse.success(SessionConstants.MSG_OTHER_SESSIONS_REVOKED, Map.of(SessionConstants.FIELD_REVOKED_COUNT, count)));
    }
}
