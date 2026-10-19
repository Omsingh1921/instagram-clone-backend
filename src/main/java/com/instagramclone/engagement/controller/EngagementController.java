package com.instagramclone.engagement.controller;



import com.instagramclone.engagement.constants.EngagementConstants;
import com.instagramclone.ratelimit.RateLimit;
import java.util.UUID;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.engagement.dto.response.CommentResponse;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.engagement.dto.response.LikeResponse;
import com.instagramclone.engagement.dto.response.SavePostResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.engagement.service.interfaces.IEngagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EngagementConstants.BASE_PATH)
@RequiredArgsConstructor
public class EngagementController {

    private final IEngagementService engagementService;

    @RateLimit(category = EngagementConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(EngagementConstants.POST_LIKE_PATH)
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(@PathVariable UUID postId) {
        LikeResponse response = engagementService.toggleLike(postId);
        return ResponseEntity.ok(ApiResponse.success(
                response.isLiked() ? EngagementConstants.MSG_POST_LIKED : EngagementConstants.MSG_POST_UNLIKED,
                response
        ));
    }

    @GetMapping(EngagementConstants.POST_LIKES_PATH)
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getPostLikes(
            @PathVariable UUID postId,
            @PageableDefault(size = EngagementConstants.DEFAULT_PAGE_SIZE, sort = EngagementConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowUserResponse> likes = engagementService.getPostLikes(postId, pageable);
        return ResponseEntity.ok(ApiResponse.success(EngagementConstants.MSG_POST_LIKES_RETRIEVED, likes));
    }

    @RateLimit(category = EngagementConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(EngagementConstants.POST_SAVE_PATH)
    public ResponseEntity<ApiResponse<SavePostResponse>> toggleSave(@PathVariable UUID postId) {
        SavePostResponse response = engagementService.toggleSave(postId);
        return ResponseEntity.ok(ApiResponse.success(
                response.isSaved() ? EngagementConstants.MSG_POST_SAVED : EngagementConstants.MSG_POST_UNSAVED,
                response
        ));
    }

    @RateLimit(category = EngagementConstants.RATE_LIMIT_CONTENT_COMMENT, limit = 20, windowSeconds = 60)
    @PostMapping(EngagementConstants.POST_COMMENTS_PATH)
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = engagementService.addComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(EngagementConstants.MSG_COMMENT_ADDED, response));
    }

    @GetMapping(EngagementConstants.POST_COMMENTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable UUID postId,
            @PageableDefault(size = EngagementConstants.DEFAULT_PAGE_SIZE, sort = EngagementConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CommentResponse> comments = engagementService.getComments(postId, pageable);
        return ResponseEntity.ok(ApiResponse.success(EngagementConstants.MSG_COMMENTS_RETRIEVED, comments));
    }

    @GetMapping(EngagementConstants.COMMENT_REPLIES_PATH)
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getCommentReplies(
            @PathVariable UUID commentId,
            @PageableDefault(size = EngagementConstants.DEFAULT_PAGE_SIZE, sort = EngagementConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CommentResponse> replies = engagementService.getReplies(commentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(EngagementConstants.MSG_COMMENT_REPLIES_RETRIEVED, replies));
    }

    @DeleteMapping(EngagementConstants.COMMENT_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID commentId) {
        engagementService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(EngagementConstants.MSG_COMMENT_DELETED, null));
    }
}
