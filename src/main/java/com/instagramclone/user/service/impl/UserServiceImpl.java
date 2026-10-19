package com.instagramclone.user.service.impl;

import com.instagramclone.user.service.interfaces.*;


import java.time.LocalDateTime;
import java.util.UUID;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.media.service.interfaces.IMediaService;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.dto.request.UpdateAvatarRequest;
import com.instagramclone.user.dto.request.UpdateProfileRequest;
import com.instagramclone.user.dto.response.UserProfileResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.mapper.UserMapper;
import com.instagramclone.user.repository.ProfileRepository;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final IMediaService mediaService;
    private final com.instagramclone.media.repository.MediaRepository mediaRepository;
    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final UserMapper userMapper;
    private final com.instagramclone.utility.RedisUtility redisUtility;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.instagramclone.security.IPrivacyService privacyService;
    private final com.instagramclone.user.repository.UserSettingsRepository userSettingsRepository;
    private final com.instagramclone.follow.repository.FollowRequestRepository followRequestRepository;
    private final com.instagramclone.auth.repository.RefreshTokenRepository refreshTokenRepository;
    private final com.instagramclone.security.ITokenBlacklistService tokenBlacklistService;
    private final com.instagramclone.security.JwtProperties jwtProperties;
    private final com.instagramclone.audit.service.interfaces.IAuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_PROFILE_NOT_FOUND_ID + currentUserId));

        long followerCount = followRepository.countByFollowingId(currentUserId);
        long followingCount = followRepository.countByFollowerId(currentUserId);
        long postCount = postRepository.countByUserId(currentUserId);

        return userMapper.toUserProfileResponse(user, postCount, followerCount, followingCount, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId));

        // When account is deleted or deactivated, only ADMIN can fetch data!
        if (!isAdmin && user.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId);
        }

        if (!isAdmin) {
            privacyService.validateBlockStatus(currentUserId, userId);
        }

        Boolean isSelf = (currentUserId != null && currentUserId.equals(userId));
        Boolean isFollowed = (currentUserId != null && !isSelf)
                ? followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId)
                : null;

        boolean isPrivate = privacyService.isAccountPrivate(userId);
        Boolean followRequested = (currentUserId != null && !isSelf && !Boolean.TRUE.equals(isFollowed) && isPrivate)
                ? followRequestRepository.existsByRequesterIdAndTargetIdAndStatus(currentUserId, userId, com.instagramclone.follow.enums.FollowRequestStatus.PENDING)
                : null;

        String cacheKey = com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + userId;
        if (redisUtility.isAvailable() && !isAdmin) {
            java.util.Optional<String> cached = redisUtility.get(cacheKey);
            if (cached.isPresent()) {
                try {
                    UserProfileResponse cachedProfile = objectMapper.readValue(cached.get(), UserProfileResponse.class);
                    cachedProfile.setFollowedByCurrentUser(isFollowed);
                    cachedProfile.setIsPrivate(isPrivate);
                    cachedProfile.setFollowRequestedByCurrentUser(followRequested);
                    return cachedProfile;
                } catch (Exception ex) {
                    log.debug("Cache deserialization failed for user profile {}: {}", userId, ex.getMessage());
                }
            }
        }

        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long postCount = postRepository.countByUserId(userId);

        UserProfileResponse response = userMapper.toUserProfileResponse(user, postCount, followerCount, followingCount, isFollowed, isPrivate, followRequested);

        if (redisUtility.isAvailable() && !user.isDeletedOrDeactivated()) {
            try {
                // Cache public profile structure without viewer-specific follow state
                UserProfileResponse toCache = userMapper.toUserProfileResponse(user, postCount, followerCount, followingCount, null, isPrivate, null);
                redisUtility.set(cacheKey, objectMapper.writeValueAsString(toCache), java.time.Duration.ofMinutes(10));
            } catch (Exception ex) {
                log.debug("Failed to cache user profile {}: {}", userId, ex.getMessage());
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND);
        }
        String normalized = username.trim().toLowerCase(java.util.Locale.ROOT);
        User user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return getUserProfileById(user.getId());
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        // Handle username change with uniqueness and normalization check
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim().toLowerCase(java.util.Locale.ROOT);
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new com.instagramclone.common.exception.ConflictException(
                            String.format(com.instagramclone.auth.constants.AuthConstants.ERR_USERNAME_TAKEN_FORMAT, newUsername));
                }
                String oldUsername = user.getUsername();
                user.setUsername(newUsername);
                userRepository.save(user);
                auditLogService.record(currentUserId, "USERNAME_CHANGED", "USER", currentUserId, null,
                        "Username changed from " + oldUsername + " to " + newUsername);
                log.info("User ID {} changed username from {} to {}", currentUserId, oldUsername, newUsername);
            }
        }

        Profile profile = user.getProfile();
        if (profile == null) {
            profile = Profile.builder().user(user).build();
            user.setProfile(profile);
        }

        if (request.getDisplayName() != null) {
            String trimmed = request.getDisplayName().trim();
            profile.setDisplayName(trimmed.isEmpty() ? null : trimmed);
        }
        if (request.getBio() != null) {
            String trimmed = request.getBio().trim();
            profile.setBio(trimmed.isEmpty() ? null : trimmed);
        }
        if (request.getWebsiteUrl() != null) {
            String trimmed = request.getWebsiteUrl().trim();
            profile.setWebsiteUrl(trimmed.isEmpty() ? null : trimmed);
        }

        profileRepository.save(profile);
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId);
        log.info("Profile updated successfully for user ID: {}", currentUserId);

        return userMapper.toUserProfileResponse(user, null);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAvatar(UpdateAvatarRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        String newAvatarUrl = request.getAvatarUrl().trim();

        // Enforce media ownership: if media asset exists in our repository, user must own it
        mediaRepository.findBySecureUrl(newAvatarUrl).ifPresent(media -> {
            if (media.getOwner() != null && !media.getOwner().getId().equals(currentUserId)) {
                throw new com.instagramclone.common.exception.ForbiddenException("Cannot use media asset owned by another user as avatar");
            }
        });

        Profile profile = user.getProfile();
        if (profile == null) {
            profile = Profile.builder().user(user).build();
            user.setProfile(profile);
        }

        String oldAvatarUrl = profile.getAvatarUrl();

        profile.setAvatarUrl(newAvatarUrl);
        profileRepository.save(profile);
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId);
        log.info("Avatar updated successfully for user ID: {}", currentUserId);

        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank() && !oldAvatarUrl.equalsIgnoreCase(newAvatarUrl)) {
            try {
                mediaService.deleteByUrl(oldAvatarUrl);
                log.info("Successfully cleaned up previous avatar asset for user: {}", currentUserId);
            } catch (Exception ex) {
                log.warn("Failed to delete previous avatar asset from Cloudinary: {}", ex.getMessage());
            }
        }

        return userMapper.toUserProfileResponse(user, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable) {
        String searchQuery = (query != null) ? query.trim() : "";
        Page<User> usersPage;
        if (SecurityUtils.isCurrentUserAdmin()) {
            usersPage = userRepository.searchAllUsersForAdmin(searchQuery, pageable);
        } else {
            UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
            usersPage = userRepository.searchUsers(searchQuery, currentUserId, pageable);
        }
        Page<UserSummaryResponse> summaryPage = usersPage.map(userMapper::toUserSummaryResponse);
        return PageResponse.from(summaryPage);
    }

    @Override
    @Transactional
    public void deleteCurrentUser() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        deleteUser(currentUserId);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new com.instagramclone.common.exception.BadRequestException("User account is already deleted");
        }

        user.softDelete();
        userRepository.save(user);

        // Immediate session revocation and token blacklisting
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        tokenBlacklistService.blacklistUser(userId, jwtProperties.getAccessExpirationMs());

        auditLogService.record(userId, "ACCOUNT_SOFT_DELETED", "USER", userId, null,
                "User account soft deleted with 15-day recovery window");

        // Evict profile cache
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + userId);
        log.info("User {} successfully soft deleted and active sessions revoked", userId);
    }

    @Override
    @Transactional
    public void deactivateCurrentUser() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        deactivateUser(currentUserId);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId));

        if (user.isDeletedOrDeactivated()) {
            throw new com.instagramclone.common.exception.BadRequestException("User account is already deleted or deactivated");
        }

        user.deactivate();
        userRepository.save(user);

        // Immediate session revocation and token blacklisting
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        tokenBlacklistService.blacklistUser(userId, jwtProperties.getAccessExpirationMs());

        auditLogService.record(userId, "ACCOUNT_DEACTIVATED", "USER", userId, null,
                "User account deactivated");

        // Evict profile cache
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_USER_PROFILE_PREFIX + userId);
        log.info("User {} successfully deactivated and active sessions revoked", userId);
    }
}
