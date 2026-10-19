package com.instagramclone.engagement.service.impl;

import com.instagramclone.engagement.service.interfaces.*;


import java.util.UUID;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.engagement.constants.EngagementConstants;
import com.instagramclone.engagement.dto.response.CommentAuthorResponse;
import com.instagramclone.engagement.dto.response.CommentResponse;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.engagement.dto.response.LikeResponse;
import com.instagramclone.engagement.dto.response.SavePostResponse;
import com.instagramclone.engagement.entity.Comment;
import com.instagramclone.engagement.entity.Like;
import com.instagramclone.engagement.entity.SavedPost;
import com.instagramclone.engagement.repository.CommentRepository;
import com.instagramclone.engagement.repository.LikeRepository;
import com.instagramclone.engagement.repository.SavedPostRepository;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements IEngagementService {

    private static final Logger log = LoggerFactory.getLogger(EngagementServiceImpl.class);

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final com.instagramclone.notification.service.interfaces.INotificationService notificationService;
    private final DomainEventPublisher domainEventPublisher;
    private final com.instagramclone.utility.RedisUtility redisUtility;
    private final com.instagramclone.security.IPrivacyService privacyService;

    private void validateCanEngageWithPost(UUID currentUserId, Post post) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isAdmin) {
            if (post.getUser().isDeletedOrDeactivated() || Boolean.TRUE.equals(post.getIsDeleted())) {
                throw new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + post.getId());
            }
            privacyService.validateCanViewUserContent(currentUserId, post.getUser().getId());
        }
    }

    @Override
    @Transactional
    public LikeResponse toggleLike(UUID postId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId));
        validateCanEngageWithPost(currentUserId, post);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_CURRENT_USER_NOT_FOUND));

        Optional<Like> existingLike = likeRepository.findByUserIdAndPostId(currentUserId, postId);
        boolean liked;

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            liked = false;
            log.info("User {} unliked post {}", currentUserId, postId);
        } else {
            Like like = Like.builder()
                    .post(post)
                    .user(user)
                    .build();
            likeRepository.save(like);
            liked = true;
            log.info("User {} liked post {}", currentUserId, postId);

            notificationService.createNotification(
                    post.getUser().getId(),
                    currentUserId,
                    com.instagramclone.notification.enums.NotificationType.POST_LIKED,
                    postId,
                    null
            );
        }

        // Invalidate cached counter after commit to ensure consistency with MySQL
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_COUNT_POST_LIKES_PREFIX + postId);
        long likeCount = likeRepository.countByPostId(postId);

        return LikeResponse.builder()
                .postId(postId)
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(UUID postId, UUID userId) {
        if (postId == null || userId == null) return false;
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(UUID postId) {
        if (postId == null) return 0L;
        String cacheKey = com.instagramclone.common.constants.CacheConstants.CACHE_COUNT_POST_LIKES_PREFIX + postId;
        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return Long.parseLong(cached.get());
            } catch (NumberFormatException ignored) {}
        }
        long count = likeRepository.countByPostId(postId);
        redisUtility.set(cacheKey, String.valueOf(count), java.time.Duration.ofMinutes(10));
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(UUID postId) {
        if (postId == null) return 0L;
        String cacheKey = com.instagramclone.common.constants.CacheConstants.CACHE_COUNT_POST_COMMENTS_PREFIX + postId;
        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return Long.parseLong(cached.get());
            } catch (NumberFormatException ignored) {}
        }
        long count = commentRepository.countByPostId(postId);
        redisUtility.set(cacheKey, String.valueOf(count), java.time.Duration.ofMinutes(10));
        return count;
    }

    @Override
    @Transactional
    public CommentResponse addComment(UUID postId, CreateCommentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId));
        validateCanEngageWithPost(currentUserId, post);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_CURRENT_USER_NOT_FOUND));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_PARENT_COMMENT_NOT_FOUND + request.getParentCommentId()));
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parentComment(parentComment)
                .text(request.getText().trim())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("User {} added comment {} to post {}", currentUserId, saved.getId(), postId);

        notificationService.createNotification(
                post.getUser().getId(),
                currentUserId,
                com.instagramclone.notification.enums.NotificationType.POST_COMMENTED,
                postId,
                saved.getId()
        );

        if (parentComment != null && !parentComment.getUser().getId().equals(post.getUser().getId())) {
            notificationService.createNotification(
                    parentComment.getUser().getId(),
                    currentUserId,
                    com.instagramclone.notification.enums.NotificationType.COMMENT_REPLIED,
                    postId,
                    saved.getId()
            );
        }

        // Invalidate comment counter cache
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_COUNT_POST_COMMENTS_PREFIX + postId);

        return toCommentResponse(saved);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_COMMENT_NOT_FOUND + commentId));

        UUID commentAuthorId = comment.getUser().getId();
        UUID postAuthorId = comment.getPost().getUser().getId();

        // IDOR check: only comment author or post owner can delete
        if (!currentUserId.equals(commentAuthorId) && !currentUserId.equals(postAuthorId)) {
            throw new ForbiddenException(EngagementConstants.ERR_UNAUTHORIZED_COMMENT_DELETE);
        }

        UUID postId = comment.getPost().getId();
        comment.softDelete();
        commentRepository.save(comment);
        redisUtility.deleteAfterCommit(com.instagramclone.common.constants.CacheConstants.CACHE_COUNT_POST_COMMENTS_PREFIX + postId);
        log.info("Comment {} soft deleted by user {}", commentId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(UUID postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // When post owner is deleted or deactivated (or post deleted), only ADMIN can fetch data!
        if (!isAdmin) {
            if (post.getUser().isDeletedOrDeactivated() || Boolean.TRUE.equals(post.getIsDeleted())) {
                throw new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId);
            }
            privacyService.validateCanViewUserContent(currentUserId, post.getUser().getId());
        }

        Page<Comment> comments = commentRepository.findRootCommentsByPostId(postId, pageable);
        return PageResponse.from(comments.map(this::toCommentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getReplies(UUID commentId, Pageable pageable) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_COMMENT_NOT_FOUND + commentId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin) {
            if (comment.getUser().isDeletedOrDeactivated() || Boolean.TRUE.equals(comment.getIsDeleted())
                    || comment.getPost().getUser().isDeletedOrDeactivated() || Boolean.TRUE.equals(comment.getPost().getIsDeleted())) {
                throw new ResourceNotFoundException(EngagementConstants.ERR_COMMENT_NOT_FOUND + commentId);
            }
            privacyService.validateCanViewUserContent(currentUserId, comment.getPost().getUser().getId());
        }

        Page<Comment> replies = commentRepository.findRepliesByParentCommentId(commentId, pageable);
        return PageResponse.from(replies.map(this::toCommentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getPostLikes(UUID postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        validateCanEngageWithPost(currentUserId, post);

        Page<Like> likesPage = likeRepository.findByPostId(postId, pageable);
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
    @Transactional
    public SavePostResponse toggleSave(UUID postId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_POST_NOT_FOUND + postId));
        validateCanEngageWithPost(currentUserId, post);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(EngagementConstants.ERR_CURRENT_USER_NOT_FOUND));

        Optional<SavedPost> existing = savedPostRepository.findByUserIdAndPostId(currentUserId, postId);
        boolean saved;

        if (existing.isPresent()) {
            savedPostRepository.delete(existing.get());
            saved = false;
            log.info("User {} unsaved post {}", currentUserId, postId);
        } else {
            SavedPost savedPost = SavedPost.builder()
                    .user(user)
                    .post(post)
                    .build();
            savedPostRepository.save(savedPost);
            saved = true;
            log.info("User {} saved post {}", currentUserId, postId);
        }

        return SavePostResponse.builder()
                .postId(postId)
                .saved(saved)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSavedByUser(UUID postId, UUID userId) {
        if (postId == null || userId == null) return false;
        return savedPostRepository.existsByUserIdAndPostId(userId, postId);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        User u = comment.getUser();
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .author(CommentAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
