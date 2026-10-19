package com.instagramclone.chat.controller;

import com.instagramclone.chat.constants.ChatConstants;
import com.instagramclone.chat.dto.response.PresenceResponse;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.user.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exposes real-time user online/offline presence adhering to Section 4.G and privacy rules.
 */
@RestController
@RequestMapping(ChatConstants.PRESENCE_BASE_PATH)
@RequiredArgsConstructor
public class PresenceController {

    private final IPresenceService presenceService;
    private final UserBlockRepository userBlockRepository;

    @GetMapping(ChatConstants.USER_PRESENCE_PATH)
    public ResponseEntity<ApiResponse<PresenceResponse>> getUserPresence(@PathVariable UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal()
                .map(com.instagramclone.security.UserPrincipal::getId)
                .orElse(null);

        // Privacy boundary: if blocked in either direction, report offline with no timestamp
        if (currentUserId != null &&
                (userBlockRepository.existsByBlockerIdAndBlockedId(userId, currentUserId) ||
                 userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, userId))) {
            return ResponseEntity.ok(ApiResponse.success(
                    PresenceResponse.builder().userId(userId).online(false).lastSeen(null).build()
            ));
        }

        PresenceResponse presence = presenceService.getPresence(userId);
        return ResponseEntity.ok(ApiResponse.success(presence));
    }
}
