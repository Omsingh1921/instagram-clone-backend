package com.instagramclone.follow.service.impl;

import com.instagramclone.follow.service.interfaces.*;

import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.event.DomainEventPublisher;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.constants.FollowConstants;
import com.instagramclone.follow.dto.response.FollowRequestResponse;
import com.instagramclone.follow.dto.response.FollowResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.follow.entity.Follow;
import com.instagramclone.follow.entity.FollowRequest;
import com.instagramclone.follow.enums.FollowRequestStatus;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.follow.repository.FollowRequestRepository;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.security.IPrivacyService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final INotificationService notificationService;
    private final DomainEventPublisher domainEventPublisher;
    private final RedisUtility redisUtility;
    private final IPrivacyService privacyService;

    @Override
    @Transactional
    public FollowResponse followUser(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException(FollowConstants.ERR_USERS_CANNOT_FOLLOW_SELF);
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, currentUserId)) {
            throw new BadRequestException(FollowConstants.ERR_CANNOT_FOLLOW_USER);
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            throw new BadRequestException(FollowConstants.ERR_BLOCKED_USER);
        }

        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_CURRENT_USER_NOT_FOUND));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_TARGET_USER_NOT_FOUND_ID + targetUserId));

        // If already following, return current status
        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            return FollowResponse.builder()
                    .followingId(targetUserId)
                    .following(true)
                    .requested(false)
                    .followerCount(followRepository.countByFollowingId(targetUserId))
                    .followingCount(followRepository.countByFollowerId(targetUserId))
                    .build();
        }

        // Check if target account is private
        boolean isPrivate = privacyService.isAccountPrivate(targetUserId);

        if (isPrivate) {
            Optional<FollowRequest> existingReq = followRequestRepository.findByRequesterIdAndTargetId(currentUserId, targetUserId);
            if (existingReq.isPresent() && existingReq.get().getStatus() == FollowRequestStatus.PENDING) {
                return FollowResponse.builder()
                        .followingId(targetUserId)
                        .following(false)
                        .requested(true)
                        .followerCount(followRepository.countByFollowingId(targetUserId))
                        .followingCount(followRepository.countByFollowerId(targetUserId))
                        .build();
            }

            FollowRequest request = existingReq.orElseGet(() -> FollowRequest.builder()
                    .requester(follower)
                    .target(following)
                    .build());
            request.setStatus(FollowRequestStatus.PENDING);
            try {
                followRequestRepository.save(request);
            } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                log.warn("Concurrent follow request caught for requester {} and target {}", follower.getUsername(), following.getUsername());
            }

            notificationService.createNotification(
                    targetUserId,
                    currentUserId,
                    NotificationType.FOLLOW_REQUEST,
                    null,
                    null
            );

            redisUtility.deleteAfterCommit(
                    CacheConstants.CACHE_USER_PROFILE_PREFIX + targetUserId,
                    CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId
            );

            log.info("User {} requested to follow private user {}", follower.getUsername(), following.getUsername());

            return FollowResponse.builder()
                    .followingId(targetUserId)
                    .following(false)
                    .requested(true)
                    .followerCount(followRepository.countByFollowingId(targetUserId))
                    .followingCount(followRepository.countByFollowerId(targetUserId))
                    .build();
        }

        // Target account is public: create follow immediately
        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        try {
            followRepository.save(follow);
            log.info("User {} is now following user {}", follower.getUsername(), following.getUsername());
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("Concurrent follow race caught for user {} following {}", follower.getUsername(), following.getUsername());
        }

        // Clean up any stale request
        followRequestRepository.deleteByRequesterIdAndTargetId(currentUserId, targetUserId);

        notificationService.createNotification(
                targetUserId,
                currentUserId,
                NotificationType.FOLLOW,
                null,
                null
        );

        redisUtility.deleteAfterCommit(
                CacheConstants.CACHE_USER_PROFILE_PREFIX + targetUserId,
                CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId
        );

        long followerCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        return FollowResponse.builder()
                .followingId(targetUserId)
                .following(true)
                .requested(false)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional
    public void unfollowUser(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    redisUtility.deleteAfterCommit(
                            CacheConstants.CACHE_USER_PROFILE_PREFIX + targetUserId,
                            CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId
                    );
                    log.info("User ID {} unfollowed user ID {}", currentUserId, targetUserId);
                    domainEventPublisher.publish("USER_UNFOLLOWED", currentUserId, targetUserId, "USER", "Unfollowed user");
                });

        // Also cancel any pending follow request
        followRequestRepository.deleteByRequesterIdAndTargetId(currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void cancelFollowRequest(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        followRequestRepository.deleteByRequesterIdAndTargetId(currentUserId, targetUserId);
        redisUtility.deleteAfterCommit(
                CacheConstants.CACHE_USER_PROFILE_PREFIX + targetUserId,
                CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId
        );
        log.info("User ID {} cancelled follow request to user ID {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowRequestResponse> getPendingFollowRequests(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<FollowRequest> page = followRequestRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(
                currentUserId, FollowRequestStatus.PENDING, pageable
        );

        Page<FollowRequestResponse> mapped = page.map(req -> {
            User u = req.getRequester();
            return FollowRequestResponse.builder()
                    .id(req.getId())
                    .requesterId(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .createdAt(req.getCreatedAt())
                    .build();
        });

        return PageResponse.from(mapped);
    }

    @Override
    @Transactional
    public FollowResponse acceptFollowRequest(UUID requestId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_FOLLOW_REQUEST_NOT_FOUND_ID + requestId));

        if (!request.getTarget().getId().equals(currentUserId)) {
            throw new ForbiddenException(FollowConstants.ERR_UNAUTHORIZED_ACCEPT_REQUEST);
        }

        if (request.getStatus() != FollowRequestStatus.PENDING) {
            throw new BadRequestException(FollowConstants.ERR_REQUEST_NOT_PENDING);
        }

        UUID requesterId = request.getRequester().getId();

        if (!followRepository.existsByFollowerIdAndFollowingId(requesterId, currentUserId)) {
            Follow follow = Follow.builder()
                    .follower(request.getRequester())
                    .following(request.getTarget())
                    .build();
            followRepository.save(follow);
            log.info("User {} accepted follow request from user {}", request.getTarget().getUsername(), request.getRequester().getUsername());

            notificationService.createNotification(
                    requesterId,
                    currentUserId,
                    NotificationType.FOLLOW_REQUEST_ACCEPTED,
                    null,
                    null
            );
        }

        followRequestRepository.delete(request);

        redisUtility.deleteAfterCommit(
                CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId,
                CacheConstants.CACHE_USER_PROFILE_PREFIX + requesterId
        );

        long followerCount = followRepository.countByFollowingId(currentUserId);
        long followingCount = followRepository.countByFollowerId(currentUserId);

        return FollowResponse.builder()
                .followingId(currentUserId)
                .following(true)
                .requested(false)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional
    public void rejectFollowRequest(UUID requestId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_FOLLOW_REQUEST_NOT_FOUND_ID + requestId));

        if (!request.getTarget().getId().equals(currentUserId)) {
            throw new ForbiddenException(FollowConstants.ERR_UNAUTHORIZED_REJECT_REQUEST);
        }

        UUID requesterId = request.getRequester().getId();
        followRequestRepository.delete(request);

        redisUtility.deleteAfterCommit(
                CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId,
                CacheConstants.CACHE_USER_PROFILE_PREFIX + requesterId
        );

        log.info("User {} rejected follow request from user {}", currentUserId, requesterId);
    }

    @Override
    @Transactional
    public void removeFollower(UUID followerUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        followRepository.findByFollowerIdAndFollowingId(followerUserId, currentUserId)
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    redisUtility.deleteAfterCommit(
                            CacheConstants.CACHE_USER_PROFILE_PREFIX + currentUserId,
                            CacheConstants.CACHE_USER_PROFILE_PREFIX + followerUserId
                    );
                    log.info("User ID {} removed follower user ID {}", currentUserId, followerUserId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID targetUserId) {
        if (followerId == null || targetUserId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowers(UUID userId, Pageable pageable) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_USER_NOT_FOUND_ID + userId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // When target user is deleted or deactivated, only ADMIN can fetch data!
        if (!isAdmin && targetUser.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(FollowConstants.ERR_USER_NOT_FOUND_ID + userId);
        }

        if (!isAdmin) {
            privacyService.validateCanViewUserContent(currentUserId, userId);
        }

        Page<Follow> page = isAdmin
                ? followRepository.findAllByFollowingId(userId, pageable)
                : followRepository.findByFollowingId(userId, pageable);
        Page<FollowUserResponse> mapped = page.map(follow -> {
            User u = follow.getFollower();
            boolean isFollowed = currentUserId != null && followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId());
            return FollowUserResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .followedByCurrentUser(isFollowed)
                    .build();
        });

        return PageResponse.from(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowing(UUID userId, Pageable pageable) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(FollowConstants.ERR_USER_NOT_FOUND_ID + userId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // When target user is deleted or deactivated, only ADMIN can fetch data!
        if (!isAdmin && targetUser.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(FollowConstants.ERR_USER_NOT_FOUND_ID + userId);
        }

        if (!isAdmin) {
            privacyService.validateCanViewUserContent(currentUserId, userId);
        }

        Page<Follow> page = isAdmin
                ? followRepository.findAllByFollowerId(userId, pageable)
                : followRepository.findByFollowerId(userId, pageable);
        Page<FollowUserResponse> mapped = page.map(follow -> {
            User u = follow.getFollowing();
            boolean isFollowed = currentUserId != null && followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId());
            return FollowUserResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .followedByCurrentUser(isFollowed)
                    .build();
        });

        return PageResponse.from(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowerCount(UUID userId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        return isAdmin ? followRepository.countAllByFollowingId(userId) : followRepository.countByFollowingId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowingCount(UUID userId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        return isAdmin ? followRepository.countAllByFollowerId(userId) : followRepository.countByFollowerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFollowingIds(UUID followerId) {
        return followRepository.findFollowingIdsByFollowerId(followerId);
    }
}
