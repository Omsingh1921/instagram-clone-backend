package com.instagramclone.user.service.impl;

import com.instagramclone.user.service.interfaces.*;


import java.util.UUID;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.user.constants.SettingsConstants;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.dto.response.UserSettingsResponse;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import com.instagramclone.audit.service.interfaces.IAuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventPublisher;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.security.JwtProperties;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements ISettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);

    private final UserSettingsRepository userSettingsRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final com.instagramclone.follow.repository.FollowRequestRepository followRequestRepository;
    private final com.instagramclone.utility.RedisUtility redisUtility;
    private final PasswordEncoder passwordEncoder;
    private final IAuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ITokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;
    private final EmailEventPublisher emailEventPublisher;

    @Override
    @Transactional
    public UserSettingsResponse getUserSettings() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        UserSettings settings = getOrCreateDefaultSettings(user);
        return toSettingsResponse(user, settings);
    }

    @Override
    @Transactional
    public UserSettingsResponse updateSettings(UpdateSettingsRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        UserSettings settings = getOrCreateDefaultSettings(user);

        boolean privacyChanged = false;
        boolean oldPrivate = settings.isPrivate();
        if (request.getPrivateAccount() != null && settings.isPrivate() != request.getPrivateAccount()) {
            settings.setPrivate(request.getPrivateAccount());
            privacyChanged = true;
        }
        if (request.getNotifyLikes() != null) {
            settings.setNotifyLikes(request.getNotifyLikes());
        }
        if (request.getNotifyComments() != null) {
            settings.setNotifyComments(request.getNotifyComments());
        }
        if (request.getNotifyFollows() != null) {
            settings.setNotifyFollows(request.getNotifyFollows());
        }
        if (request.getNotifyMessages() != null) {
            settings.setNotifyMessages(request.getNotifyMessages());
        }

        userSettingsRepository.save(settings);
        log.info("Settings updated for user {}", user.getUsername());

        if (privacyChanged) {
            emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("SECURITY_SETTINGS_CHANGED")
                    .emailType(EmailType.SECURITY_SETTINGS_CHANGED)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .variables(Map.of(
                            "username", user.getUsername(),
                            "changeDetails", "Account privacy changed from " + (oldPrivate ? "Private" : "Public") + " to " + (settings.isPrivate() ? "Private" : "Public")
                    ))
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return toSettingsResponse(user, settings);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException(SettingsConstants.ERR_PASSWORDS_DO_NOT_MATCH);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(SettingsConstants.ERR_CURRENT_PASSWORD_PROVIDED_INCORRECT);
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException(SettingsConstants.ERR_NEW_PASSWORD_SAME_AS_CURRENT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens in database
        refreshTokenRepository.revokeAllUserTokens(currentUserId, LocalDateTime.now());

        // Invalidate active session tokens in Redis / memory
        tokenBlacklistService.blacklistUser(currentUserId, jwtProperties.getAccessExpirationMs());

        auditLogService.record(currentUserId, "PASSWORD_CHANGED", "USER", currentUserId, null, "Password successfully updated");
        log.info("Password successfully changed and sessions invalidated for user {}", user.getUsername());

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaConstants.EVENT_PASSWORD_CHANGED_SECURITY_ALERT)
                .emailType(EmailType.PASSWORD_CHANGED)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(Map.of("username", user.getUsername()))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void blockUser(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException(SettingsConstants.ERR_CANNOT_BLOCK_SELF);
        }

        User blocker = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_CURRENT_USER_NOT_FOUND));
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_TARGET_USER_NOT_FOUND_ID + targetUserId));

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            UserBlock block = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build();
            userBlockRepository.save(block);

            // Clean up reciprocal follow relationships
            followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                    .ifPresent(followRepository::delete);
            followRepository.findByFollowerIdAndFollowingId(targetUserId, currentUserId)
                    .ifPresent(followRepository::delete);

            // Clean up reciprocal pending follow requests
            followRequestRepository.deleteByRequesterIdAndTargetId(currentUserId, targetUserId);
            followRequestRepository.deleteByRequesterIdAndTargetId(targetUserId, currentUserId);

            // Evict profile caches
            redisUtility.deleteAfterCommit(
                    com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId,
                    com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + targetUserId
            );

            log.info("User {} blocked user {}", blocker.getUsername(), blocked.getUsername());
        }
    }

    @Override
    @Transactional
    public void unblockUser(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        userBlockRepository.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);
        log.info("User {} unblocked user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getBlockedUsers(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<UserBlock> blocks = userBlockRepository.findByBlockerId(currentUserId, pageable);

        Page<FollowUserResponse> mapped = blocks.map(block -> {
            User u = block.getBlocked();
            return FollowUserResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .followedByCurrentUser(false)
                    .build();
        });

        return PageResponse.from(mapped);
    }

    private UserSettings getOrCreateDefaultSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings s = UserSettings.builder()
                            .user(user)
                            .isPrivate(false)
                            .notifyLikes(true)
                            .notifyComments(true)
                            .notifyFollows(true)
                            .notifyMessages(true)
                            .build();
                    return userSettingsRepository.save(s);
                });
    }

    private UserSettingsResponse toSettingsResponse(User user, UserSettings settings) {
        return UserSettingsResponse.builder()
                .username(user.getUsername())
                .privacy(UserSettingsResponse.PrivacySettings.builder()
                        .privateAccount(settings.isPrivate())
                        .build())
                .notifications(UserSettingsResponse.NotificationPreferences.builder()
                        .likes(settings.isNotifyLikes())
                        .comments(settings.isNotifyComments())
                        .followers(settings.isNotifyFollows())
                        .messages(settings.isNotifyMessages())
                        .build())
                .accountStatus(user.getStatus().name())
                .build();
    }
}
