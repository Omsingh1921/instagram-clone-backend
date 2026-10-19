package com.instagramclone.reel.controller;



import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.reel.constants.ReelConstants;
import java.util.UUID;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.reel.dto.response.ReelResponse;
import com.instagramclone.reel.service.interfaces.IReelService;
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
@RequestMapping(ReelConstants.BASE_PATH)
@RequiredArgsConstructor
public class ReelController {

    private final IReelService reelService;
    private final com.instagramclone.reel.service.interfaces.IReelEngagementService reelEngagementService;

    @RateLimit(category = ReelConstants.RATE_LIMIT_CONTENT_CREATE, limit = 10, windowSeconds = 60)
    @PostMapping(ReelConstants.REELS_PATH)
    public ResponseEntity<ApiResponse<ReelResponse>> createReel(@Valid @RequestBody CreateReelRequest request) {
        ReelResponse response = reelService.createReel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ReelConstants.MSG_REEL_CREATED, response));
    }

    @GetMapping(ReelConstants.REELS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<ReelResponse>>> getReels(
            @PageableDefault(size = ReelConstants.DEFAULT_PAGE_SIZE, sort = ReelConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ReelResponse> response = reelService.getReels(pageable);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_REELS_RETRIEVED, response));
    }

    @GetMapping(ReelConstants.REEL_BY_ID_PATH)
    public ResponseEntity<ApiResponse<ReelResponse>> getReelById(@PathVariable UUID reelId) {
        ReelResponse response = reelService.getReelById(reelId);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_REEL_RETRIEVED, response));
    }

    @GetMapping(ReelConstants.USER_REELS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<ReelResponse>>> getUserReels(
            @PathVariable UUID userId,
            @PageableDefault(size = ReelConstants.DEFAULT_USER_REELS_PAGE_SIZE, sort = ReelConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ReelResponse> response = reelService.getUserReels(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_USER_REELS_RETRIEVED, response));
    }

    @DeleteMapping(ReelConstants.REEL_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteReel(@PathVariable UUID reelId) {
        reelService.deleteReel(reelId);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_REEL_DELETED, null));
    }

    @RateLimit(category = ReelConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping({ReelConstants.REEL_LIKE_PATH, ReelConstants.REEL_LIKES_PATH})
    public ResponseEntity<ApiResponse<com.instagramclone.reel.dto.response.ReelLikeResponse>> toggleLike(
            @PathVariable UUID reelId) {
        com.instagramclone.reel.dto.response.ReelLikeResponse response = reelEngagementService.toggleLike(reelId);
        return ResponseEntity.ok(ApiResponse.success(
                response.isLiked() ? ReelConstants.MSG_REEL_LIKED : ReelConstants.MSG_REEL_UNLIKED,
                response
        ));
    }

    @GetMapping(ReelConstants.REEL_LIKES_PATH)
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getReelLikes(
            @PathVariable UUID reelId,
            @PageableDefault(size = ReelConstants.DEFAULT_PAGE_SIZE, sort = ReelConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowUserResponse> response = reelEngagementService.getReelLikes(reelId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_REEL_LIKES_RETRIEVED, response));
    }

    @RateLimit(category = ReelConstants.RATE_LIMIT_CONTENT_COMMENT, limit = 20, windowSeconds = 60)
    @PostMapping(ReelConstants.REEL_COMMENTS_PATH)
    public ResponseEntity<ApiResponse<com.instagramclone.reel.dto.response.ReelCommentResponse>> addComment(
            @PathVariable UUID reelId,
            @Valid @RequestBody com.instagramclone.reel.dto.request.CreateReelCommentRequest request) {
        com.instagramclone.reel.dto.response.ReelCommentResponse response = reelEngagementService.addComment(reelId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ReelConstants.MSG_COMMENT_ADDED, response));
    }

    @GetMapping(ReelConstants.REEL_COMMENTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<com.instagramclone.reel.dto.response.ReelCommentResponse>>> getComments(
            @PathVariable UUID reelId,
            @PageableDefault(size = ReelConstants.DEFAULT_COMMENTS_PAGE_SIZE, sort = ReelConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<com.instagramclone.reel.dto.response.ReelCommentResponse> comments = reelEngagementService.getComments(reelId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_COMMENTS_RETRIEVED, comments));
    }

    @DeleteMapping({ReelConstants.REEL_COMMENT_BY_ID_PATH, ReelConstants.REEL_COMMENT_WITH_REEL_ID_PATH})
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID commentId) {
        reelEngagementService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_COMMENT_DELETED, null));
    }

    @RateLimit(category = ReelConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping({ReelConstants.REEL_VIEW_PATH, ReelConstants.REEL_VIEWS_PATH})
    public ResponseEntity<ApiResponse<com.instagramclone.reel.dto.response.ReelViewResponse>> recordView(
            @PathVariable UUID reelId) {
        com.instagramclone.reel.dto.response.ReelViewResponse response = reelEngagementService.recordView(reelId);
        return ResponseEntity.ok(ApiResponse.success(ReelConstants.MSG_VIEW_RECORDED, response));
    }
}
