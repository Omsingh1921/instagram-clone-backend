package com.instagramclone.feed.service.impl;

import com.instagramclone.feed.service.interfaces.*;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.engagement.service.interfaces.IEngagementService;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.post.dto.response.PostMediaResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.entity.PostMedia;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements IFeedService {

    private final PostRepository postRepository;
    private final IEngagementService engagementService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getHomeFeed(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<Post> posts = postRepository.findTimelinePosts(currentUserId, pageable);
        return PageResponse.from(posts.map(p -> toPostResponse(p, currentUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getExploreFeed(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        Page<Post> posts = postRepository.findExplorePosts(PostVisibility.PUBLIC, currentUserId, pageable);
        return PageResponse.from(posts.map(p -> toPostResponse(p, currentUserId)));
    }

    private PostResponse toPostResponse(Post post, UUID viewerId) {
        User u = post.getUser();
        List<PostMediaResponse> mediaResponses = new ArrayList<>();
        if (post.getMediaList() != null) {
            for (PostMedia pm : post.getMediaList()) {
                mediaResponses.add(PostMediaResponse.builder()
                        .id(pm.getId())
                        .mediaUrl(pm.getMediaUrl())
                        .mediaType(pm.getMediaType())
                        .displayOrder(pm.getDisplayOrder())
                        .width(pm.getWidth())
                        .height(pm.getHeight())
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
