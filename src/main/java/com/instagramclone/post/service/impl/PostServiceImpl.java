package com.instagramclone.post.service.impl;

import com.instagramclone.post.service.interfaces.*;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.engagement.service.interfaces.IEngagementService;
import com.instagramclone.engagement.repository.SavedPostRepository;
import com.instagramclone.engagement.entity.SavedPost;
import com.instagramclone.media.service.interfaces.IMediaService;
import com.instagramclone.post.constants.PostConstants;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.response.MediaUploadResponse;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.dto.response.PostMediaResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.post.dto.request.UpdatePostRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.entity.PostMedia;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.post.repository.PostMediaRepository;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.audit.service.interfaces.IAuditLogService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/quicktime", "video/webm"
    );

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final com.instagramclone.post.repository.PostMediaTagRepository postMediaTagRepository;
    private final UserRepository userRepository;
    private final IEngagementService engagementService;
    private final SavedPostRepository savedPostRepository;
    private final com.instagramclone.audit.service.interfaces.IAuditLogService auditLogService;
    private final IMediaService mediaService;
    private final com.instagramclone.notification.service.interfaces.INotificationService notificationService;
    private final com.instagramclone.common.event.DomainEventPublisher domainEventPublisher;
    private final com.instagramclone.security.IPrivacyService privacyService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_AUTHOR_NOT_FOUND + currentUserId));

        if (request.getMedia() == null || request.getMedia().isEmpty()) {
            throw new BadRequestException(PostConstants.ERR_MEDIA_REQUIRED);
        }

        Post post = Post.builder()
                .user(author)
                .caption(request.getCaption())
                .visibility(request.getVisibility() != null ? request.getVisibility() : PostVisibility.PUBLIC)
                .build();

        int order = 0;
        for (PostMediaRequest mr : request.getMedia()) {
            PostMedia media = PostMedia.builder()
                    .mediaUrl(mr.getMediaUrl())
                    .mediaType(mr.getMediaType() != null ? mr.getMediaType() : MediaType.IMAGE)
                    .displayOrder(mr.getDisplayOrder() != null ? mr.getDisplayOrder() : order++)
                    .width(mr.getWidth())
                    .height(mr.getHeight())
                    .build();
            post.addMedia(media);
        }

        Post savedPost = postRepository.save(post);
        log.info("Created post {} by user {}", savedPost.getId(), author.getUsername());

        // Process media user tags if provided
        for (int i = 0; i < request.getMedia().size(); i++) {
            PostMediaRequest mr = request.getMedia().get(i);
            if (mr.getTags() != null && !mr.getTags().isEmpty() && i < savedPost.getMediaList().size()) {
                PostMedia savedMedia = savedPost.getMediaList().get(i);
                for (com.instagramclone.post.dto.request.PostMediaTagRequest tr : mr.getTags()) {
                    if (tr.getTaggedUserId() != null) {
                        userRepository.findById(tr.getTaggedUserId()).ifPresent(taggedUser -> {
                            if (!taggedUser.isDeletedOrDeactivated()) {
                                com.instagramclone.post.entity.PostMediaTag tag = com.instagramclone.post.entity.PostMediaTag.builder()
                                        .postMedia(savedMedia)
                                        .taggedUser(taggedUser)
                                        .xPercent(tr.getXPercent() != null ? tr.getXPercent() : 50.0)
                                        .yPercent(tr.getYPercent() != null ? tr.getYPercent() : 50.0)
                                        .build();
                                postMediaTagRepository.save(tag);

                                if (!taggedUser.getId().equals(currentUserId)) {
                                    notificationService.createNotification(
                                            taggedUser.getId(),
                                            currentUserId,
                                            com.instagramclone.notification.enums.NotificationType.USER_TAGGED,
                                            savedPost.getId(),
                                            null
                                    );
                                }
                            }
                        });
                    }
                }
            }
        }

        return toPostResponse(savedPost, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId) {
        Post post = postRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_POST_NOT_FOUND + postId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // When account is deleted or deactivated (or post is deleted), only ADMIN can fetch data!
        if (!isAdmin) {
            if (post.getUser().isDeletedOrDeactivated() || Boolean.TRUE.equals(post.getIsDeleted())) {
                throw new ResourceNotFoundException(PostConstants.ERR_POST_NOT_FOUND + postId);
            }
            privacyService.validateCanViewUserContent(currentUserId, post.getUser().getId());
        }

        return toPostResponse(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getSavedPosts(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<SavedPost> savedPage = savedPostRepository.findByUserId(currentUserId, pageable);
        return PageResponse.from(savedPage.map(sp -> toPostResponse(sp.getPost(), currentUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(UUID userId, Pageable pageable) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_USER_NOT_FOUND + userId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // When author account is deleted or deactivated, only ADMIN can fetch data!
        if (!isAdmin && author.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(PostConstants.ERR_USER_NOT_FOUND + userId);
        }

        if (!isAdmin) {
            privacyService.validateCanViewUserContent(currentUserId, userId);
        }

        Page<Post> posts = isAdmin
                ? postRepository.findByUserId(userId, pageable)
                : postRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        return PageResponse.from(posts.map(post -> toPostResponse(post, currentUserId)));
    }

    @Override
    @Transactional
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_POST_NOT_FOUND + postId));

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(PostConstants.ERR_UNAUTHORIZED_UPDATE);
        }

        if (request.getCaption() != null) {
            post.setCaption(request.getCaption());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        Post updated = postRepository.save(post);
        log.info("Post {} updated by user {}", postId, currentUserId);

        return toPostResponse(updated, currentUserId);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_POST_NOT_FOUND + postId));

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(PostConstants.ERR_UNAUTHORIZED_DELETE);
        }

        post.softDelete();
        postRepository.save(post);
        auditLogService.record(currentUserId, "POST_DELETED", "POST", postId, null, "Deleted post");
        log.info("Post {} soft deleted by user {}", postId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPostCount(UUID userId) {
        return postRepository.countByUserId(userId);
    }

    @Override
    public MediaUploadResponse uploadMedia(MultipartFile file) {
        return mediaService.uploadMediaAsUploadResponse(file, "posts");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserTaggedPosts(UUID userId, Pageable pageable) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(PostConstants.ERR_USER_NOT_FOUND + userId));

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && targetUser.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(PostConstants.ERR_USER_NOT_FOUND + userId);
        }

        if (!isAdmin) {
            privacyService.validateCanViewUserContent(currentUserId, userId);
        }

        Page<Post> taggedPosts = postRepository.findTaggedPostsByUserId(userId, pageable);
        return PageResponse.from(taggedPosts.map(post -> toPostResponse(post, currentUserId)));
    }

    private PostResponse toPostResponse(Post post, UUID viewerId) {
        User u = post.getUser();
        List<PostMediaResponse> mediaResponses = new ArrayList<>();
        if (post.getMediaList() != null) {
            for (PostMedia pm : post.getMediaList()) {
                List<com.instagramclone.post.dto.response.PostMediaTagResponse> tagResponses = new ArrayList<>();
                List<com.instagramclone.post.entity.PostMediaTag> mediaTags = postMediaTagRepository.findByPostMediaId(pm.getId());
                if (mediaTags != null) {
                    for (com.instagramclone.post.entity.PostMediaTag mt : mediaTags) {
                        User tu = mt.getTaggedUser();
                        tagResponses.add(com.instagramclone.post.dto.response.PostMediaTagResponse.builder()
                                .id(mt.getId())
                                .taggedUserId(tu.getId())
                                .taggedUsername(tu.getUsername())
                                .displayName(tu.getProfile() != null ? tu.getProfile().getDisplayName() : null)
                                .xPercent(mt.getXPercent())
                                .yPercent(mt.getYPercent())
                                .build());
                    }
                }

                mediaResponses.add(PostMediaResponse.builder()
                        .id(pm.getId())
                        .mediaUrl(pm.getMediaUrl())
                        .mediaType(pm.getMediaType())
                        .displayOrder(pm.getDisplayOrder())
                        .width(pm.getWidth())
                        .height(pm.getHeight())
                        .tags(tagResponses)
                        .build());
            }
        }

        long likeCount = engagementService.getLikeCount(post.getId());
        long commentCount = engagementService.getCommentCount(post.getId());
        boolean liked = viewerId != null && engagementService.isLikedByUser(post.getId(), viewerId);
        boolean saved = viewerId != null && engagementService.isSavedByUser(post.getId(), viewerId);

        return PostResponse.builder()
                .id(post.getId())
                .author(PostAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .caption(post.getCaption())
                .visibility(post.getVisibility())
                .media(mediaResponses)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByCurrentUser(liked)
                .savedByCurrentUser(saved)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
