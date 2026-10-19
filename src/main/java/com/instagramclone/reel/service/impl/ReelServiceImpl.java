package com.instagramclone.reel.service.impl;

import com.instagramclone.reel.service.interfaces.*;


import java.util.UUID;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.media.service.interfaces.IMediaService;
import com.instagramclone.reel.constants.ReelConstants;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.reel.dto.response.ReelResponse;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.enums.ReelStatus;
import com.instagramclone.reel.repository.ReelRepository;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.entity.User;
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
public class ReelServiceImpl implements IReelService {

    private static final Logger log = LoggerFactory.getLogger(ReelServiceImpl.class);

    private final ReelRepository reelRepository;
    private final UserRepository userRepository;
    private final IMediaService mediaService;
    private final IReelEngagementService reelEngagementService;
    private final com.instagramclone.security.IPrivacyService privacyService;
    private final com.instagramclone.reel.repository.ReelLikeRepository reelLikeRepository;
    private final com.instagramclone.reel.repository.ReelCommentRepository reelCommentRepository;
    private final com.instagramclone.reel.repository.ReelViewRepository reelViewRepository;

    @Override
    @Transactional
    public ReelResponse createReel(CreateReelRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        if (request.getDurationMs() != null && request.getDurationMs() > 0) {
            long durationSec = request.getDurationMs() / 1000;
            if (durationSec < ReelConstants.MIN_DURATION_SECONDS || durationSec > ReelConstants.MAX_DURATION_SECONDS) {
                throw new BadRequestException(ReelConstants.MSG_INVALID_REEL_DURATION);
            }
        }

        Reel reel = Reel.builder()
                .user(author)
                .videoUrl(request.getVideoUrl().trim())
                .thumbnailUrl(request.getThumbnailUrl())
                .caption(request.getCaption())
                .durationMs(request.getDurationMs() != null ? request.getDurationMs() : 0L)
                .status(ReelStatus.READY)
                .build();

        Reel saved = reelRepository.save(reel);
        log.info("Reel {} created by user {}", saved.getId(), author.getUsername());

        return toReelResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReelResponse> getReels(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        Page<Reel> page = reelRepository.findFeedReels(ReelStatus.READY, currentUserId, pageable);
        return PageResponse.from(page.map(this::toReelResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReelResponse getReelById(UUID reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isAdmin) {
            if (reel.getUser().isDeletedOrDeactivated() || reel.getStatus() != ReelStatus.READY) {
                throw new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId);
            }
            UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
            privacyService.validateCanViewUserContent(currentUserId, reel.getUser().getId());
        }

        return toReelResponse(reel);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReelResponse> getUserReels(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        privacyService.validateCanViewUserContent(currentUserId, userId);

        Page<Reel> page = reelRepository.findByUserIdAndStatus(userId, ReelStatus.READY, pageable);
        return PageResponse.from(page.map(this::toReelResponse));
    }

    @Override
    @Transactional
    public void deleteReel(UUID reelId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException(ReelConstants.ERR_REEL_NOT_FOUND + reelId));

        if (!reel.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(ReelConstants.ERR_UNAUTHORIZED_DELETE);
        }

        try {
            mediaService.deleteByUrl(reel.getVideoUrl());
            if (reel.getThumbnailUrl() != null && !reel.getThumbnailUrl().equals(reel.getVideoUrl())) {
                mediaService.deleteByUrl(reel.getThumbnailUrl());
            }
        } catch (Exception ex) {
            log.warn("Failed to delete reel media assets from Cloudinary: {}", ex.getMessage());
        }

        reelLikeRepository.deleteByReelId(reelId);
        reelCommentRepository.deleteByReelId(reelId);
        reelViewRepository.deleteByReelId(reelId);

        reelRepository.delete(reel);
        log.info("Reel {} deleted by user {}", reelId, currentUserId);
    }

    private ReelResponse toReelResponse(Reel reel) {
        User u = reel.getUser();
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);

        long likeCount = reelEngagementService.getLikeCount(reel.getId());
        long commentCount = reelEngagementService.getCommentCount(reel.getId());
        long viewCount = reelEngagementService.getViewCount(reel.getId());
        boolean liked = currentUserId != null && reelEngagementService.isLikedByUser(reel.getId(), currentUserId);

        return ReelResponse.builder()
                .id(reel.getId())
                .author(PostAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .caption(reel.getCaption())
                .status(reel.getStatus())
                .durationMs(reel.getDurationMs())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .viewCount(viewCount)
                .likedByCurrentUser(liked)
                .createdAt(reel.getCreatedAt())
                .updatedAt(reel.getUpdatedAt())
                .build();
    }
}
