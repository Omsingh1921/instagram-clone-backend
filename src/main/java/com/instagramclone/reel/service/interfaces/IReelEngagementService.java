package com.instagramclone.reel.service.interfaces;

import com.instagramclone.common.response.PageResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.reel.dto.request.CreateReelCommentRequest;
import com.instagramclone.reel.dto.response.ReelCommentResponse;
import com.instagramclone.reel.dto.response.ReelLikeResponse;
import com.instagramclone.reel.dto.response.ReelViewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IReelEngagementService {

    ReelLikeResponse toggleLike(UUID reelId);

    PageResponse<FollowUserResponse> getReelLikes(UUID reelId, Pageable pageable);

    boolean isLikedByUser(UUID reelId, UUID userId);

    long getLikeCount(UUID reelId);

    ReelCommentResponse addComment(UUID reelId, CreateReelCommentRequest request);

    PageResponse<ReelCommentResponse> getComments(UUID reelId, Pageable pageable);

    long getCommentCount(UUID reelId);

    void deleteComment(UUID commentId);

    ReelViewResponse recordView(UUID reelId);

    long getViewCount(UUID reelId);
}
