package com.instagramclone.reel.service.impl;

import com.instagramclone.reel.service.interfaces.*;

import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.reel.constants.ReelConstants;
import com.instagramclone.reel.dto.request.CreateReelCommentRequest;
import com.instagramclone.reel.dto.response.ReelCommentResponse;
import com.instagramclone.reel.dto.response.ReelLikeResponse;
import com.instagramclone.reel.dto.response.ReelViewResponse;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.entity.ReelComment;
import com.instagramclone.reel.entity.ReelLike;
import com.instagramclone.reel.entity.ReelView;
import com.instagramclone.reel.repository.ReelCommentRepository;
import com.instagramclone.reel.repository.ReelLikeRepository;
import com.instagramclone.reel.repository.ReelRepository;
import com.instagramclone.reel.repository.ReelViewRepository;
import com.instagramclone.security.IPrivacyService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.utility.RedisDistributedLockService;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReelEngagementServiceImpl implements IReelEngagementService {

    private static final Logger log = LoggerFactory.getLogger(ReelEngagementServiceImpl.class);

    private final ReelRepository reelRepository;
    private final ReelLikeRepository reelLikeRepository;
    private final ReelCommentRepository reelCommentRepository;
    private final ReelViewRepository reelViewRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final IPrivacyService privacyService;
    private final INotificationService notificationService;
    private final RedisUtility redisUtility;
    private final RedisDistributedLockService lockService;
    private final PlatformTransactionManager transactionManager;

    private void validateCanEngageWithReel(UUID currentUserId, Reel reel) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isAdmin) {
            if (reel.getUser().isDeletedOrDeactivated() || reel.getStatus() != com.instagramclone.reel.enums.ReelStatus.READY) {
                throw new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reel.getId());
            }
            privacyService.validateCanViewUserContent(currentUserId, reel.getUser().getId());
        }
    }

    private boolean acquireLockWithRetry(String lockKey, String lockOwner, Duration lease, int maxRetries, long sleepMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (lockService.tryLock(lockKey, lockOwner, lease)) {
                return true;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public ReelLikeResponse toggleLike(UUID reelId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String lockKey = "lock:reel:like:" + currentUserId + ":" + reelId;
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = acquireLockWithRetry(lockKey, lockOwner, Duration.ofSeconds(5), 20, 50);

        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            return txTemplate.execute(status -> executeToggleLike(currentUserId, reelId));
        } finally {
            if (locked) {
                lockService.releaseLock(lockKey, lockOwner);
            }
        }
    }

    private ReelLikeResponse executeToggleLike(UUID currentUserId, UUID reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        validateCanEngageWithReel(currentUserId, reel);

        Optional<ReelLike> existingLike = reelLikeRepository.findByUserIdAndReelId(currentUserId, reelId);
        boolean liked;

        if (existingLike.isPresent()) {
            reelLikeRepository.delete(existingLike.get());
            liked = false;
            log.info("User {} unliked reel {}", currentUserId, reelId);
        } else {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_CURRENT_USER_NOT_FOUND));

            ReelLike like = ReelLike.builder()
                    .reel(reel)
                    .user(user)
                    .build();
            try {
                reelLikeRepository.save(like);
                liked = true;
                log.info("User {} liked reel {}", currentUserId, reelId);

                notificationService.createNotification(
                        reel.getUser().getId(),
                        currentUserId,
                        NotificationType.REEL_LIKED,
                        null,
                        null,
                        reelId
                );
            } catch (DataIntegrityViolationException ex) {
                // Concurrent like already recorded
                liked = true;
            }
        }

        redisUtility.delete(CacheConstants.CACHE_COUNT_REEL_LIKES_PREFIX + reelId);
        redisUtility.deleteAfterCommit(CacheConstants.CACHE_COUNT_REEL_LIKES_PREFIX + reelId);
        long likeCount = getLikeCount(reelId);

        return ReelLikeResponse.builder()
                .reelId(reelId)
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(UUID reelId, UUID userId) {
        if (reelId == null || userId == null) return false;
        return reelLikeRepository.existsByUserIdAndReelId(userId, reelId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(UUID reelId) {
        String cacheKey = CacheConstants.CACHE_COUNT_REEL_LIKES_PREFIX + reelId;
        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return Long.parseLong(cached.get());
            } catch (NumberFormatException ignored) {}
        }

        long count = reelLikeRepository.countByReelId(reelId);
        redisUtility.set(cacheKey, String.valueOf(count), Duration.ofMinutes(10));
        return count;
    }

    @Override
    @Transactional
    public ReelCommentResponse addComment(UUID reelId, CreateReelCommentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        validateCanEngageWithReel(currentUserId, reel);

        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_CURRENT_USER_NOT_FOUND));

        ReelComment comment = ReelComment.builder()
                .reel(reel)
                .user(author)
                .text(request.getText().trim())
                .build();

        ReelComment saved = reelCommentRepository.save(comment);
        log.info("Comment {} added to reel {} by user {}", saved.getId(), reelId, currentUserId);

        redisUtility.delete(CacheConstants.CACHE_COUNT_REEL_COMMENTS_PREFIX + reelId);
        redisUtility.deleteAfterCommit(CacheConstants.CACHE_COUNT_REEL_COMMENTS_PREFIX + reelId);

        notificationService.createNotification(
                reel.getUser().getId(),
                currentUserId,
                NotificationType.REEL_COMMENTED,
                null,
                null,
                reelId
        );

        return toReelCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReelCommentResponse> getComments(UUID reelId, Pageable pageable) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null);
        validateCanEngageWithReel(currentUserId, reel);

        Page<ReelComment> page = reelCommentRepository.findByReelIdOrderByCreatedAtAsc(reelId, pageable);
        return PageResponse.from(page.map(this::toReelCommentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getReelLikes(UUID reelId, Pageable pageable) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        validateCanEngageWithReel(currentUserId, reel);

        Page<ReelLike> likesPage = reelLikeRepository.findByReelId(reelId, pageable);
        return PageResponse.from(likesPage.map(like -> {
            User u = like.getUser();
            boolean isFollowed = currentUserId != null
                    && !currentUserId.equals(u.getId())
                    && followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId());
            return FollowUserResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .following(isFollowed)
                    .followedByCurrentUser(isFollowed)
                    .build();
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(UUID reelId) {
        String cacheKey = CacheConstants.CACHE_COUNT_REEL_COMMENTS_PREFIX + reelId;
        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return Long.parseLong(cached.get());
            } catch (NumberFormatException ignored) {}
        }

        long count = reelCommentRepository.countByReelId(reelId);
        redisUtility.set(cacheKey, String.valueOf(count), Duration.ofMinutes(10));
        return count;
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReelComment comment = reelCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_COMMENT_NOT_FOUND + commentId));

        UUID commentAuthorId = comment.getUser().getId();
        UUID reelAuthorId = comment.getReel().getUser().getId();

        // Either author of comment or author of reel can delete comment
        if (!commentAuthorId.equals(currentUserId) && !reelAuthorId.equals(currentUserId)) {
            throw new ForbiddenException(ReelConstants.ERR_UNAUTHORIZED_COMMENT_DELETE);
        }

        UUID reelId = comment.getReel().getId();
        reelCommentRepository.delete(comment);
        log.info("Comment {} deleted from reel {} by user {}", commentId, reelId, currentUserId);

        redisUtility.delete(CacheConstants.CACHE_COUNT_REEL_COMMENTS_PREFIX + reelId);
        redisUtility.deleteAfterCommit(CacheConstants.CACHE_COUNT_REEL_COMMENTS_PREFIX + reelId);
    }

    @Override
    public ReelViewResponse recordView(UUID reelId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String lockKey = "lock:reel:view:" + currentUserId + ":" + reelId;
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = acquireLockWithRetry(lockKey, lockOwner, Duration.ofSeconds(5), 20, 50);

        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            return txTemplate.execute(status -> executeRecordView(currentUserId, reelId));
        } finally {
            if (locked) {
                lockService.releaseLock(lockKey, lockOwner);
            }
        }
    }

    private ReelViewResponse executeRecordView(UUID currentUserId, UUID reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        validateCanEngageWithReel(currentUserId, reel);

        // Distinct viewer tracking: Deduplicate repeated playback loop calls by user
        if (!reelViewRepository.existsByReelIdAndUserId(reelId, currentUserId)) {
            User viewer = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_CURRENT_USER_NOT_FOUND));

            ReelView view = ReelView.builder()
                    .reel(reel)
                    .user(viewer)
                    .build();
            try {
                reelViewRepository.save(view);
                redisUtility.delete(CacheConstants.CACHE_COUNT_REEL_VIEWS_PREFIX + reelId);
                redisUtility.deleteAfterCommit(CacheConstants.CACHE_COUNT_REEL_VIEWS_PREFIX + reelId);
                log.info("User {} viewed reel {}", currentUserId, reelId);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Concurrent view deduplicated for reel {} by user {}", reelId, currentUserId);
            }
        }

        long viewCount = getViewCount(reelId);
        return ReelViewResponse.builder()
                .reelId(reelId)
                .viewCount(viewCount)
                .viewed(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getViewCount(UUID reelId) {
        String cacheKey = CacheConstants.CACHE_COUNT_REEL_VIEWS_PREFIX + reelId;
        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return Long.parseLong(cached.get());
            } catch (NumberFormatException ignored) {}
        }

        long count = reelViewRepository.countByReelId(reelId);
        redisUtility.set(cacheKey, String.valueOf(count), Duration.ofMinutes(10));
        return count;
    }

    private ReelCommentResponse toReelCommentResponse(ReelComment comment) {
        User u = comment.getUser();
        return ReelCommentResponse.builder()
                .id(comment.getId())
                .reelId(comment.getReel().getId())
                .author(PostAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
