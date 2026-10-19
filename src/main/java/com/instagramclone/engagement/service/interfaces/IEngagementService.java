package com.instagramclone.engagement.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.engagement.dto.response.CommentResponse;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.engagement.dto.response.LikeResponse;
import com.instagramclone.engagement.dto.response.SavePostResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import org.springframework.data.domain.Pageable;

public interface IEngagementService {

    LikeResponse toggleLike(UUID postId);

    PageResponse<FollowUserResponse> getPostLikes(UUID postId, Pageable pageable);

    boolean isLikedByUser(UUID postId, UUID userId);

    long getLikeCount(UUID postId);

    long getCommentCount(UUID postId);

    CommentResponse addComment(UUID postId, CreateCommentRequest request);

    void deleteComment(UUID commentId);

    PageResponse<CommentResponse> getComments(UUID postId, Pageable pageable);

    PageResponse<CommentResponse> getReplies(UUID commentId, Pageable pageable);

    SavePostResponse toggleSave(UUID postId);

    boolean isSavedByUser(UUID postId, UUID userId);
}
