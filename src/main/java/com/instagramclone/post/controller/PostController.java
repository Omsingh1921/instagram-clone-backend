package com.instagramclone.post.controller;



import com.instagramclone.post.constants.PostConstants;
import com.instagramclone.ratelimit.RateLimit;
import java.util.UUID;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.post.dto.request.UpdatePostRequest;
import com.instagramclone.post.service.interfaces.IPostService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PostConstants.BASE_PATH)
@RequiredArgsConstructor
public class PostController {

    private final IPostService postService;

    @RateLimit(category = PostConstants.RATE_LIMIT_CONTENT_CREATE, limit = 15, windowSeconds = 60)
    @PostMapping(PostConstants.POSTS_PATH)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostConstants.MSG_POST_CREATED, response));
    }

    @GetMapping(PostConstants.SAVED_POSTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getSavedPosts(
            @PageableDefault(size = PostConstants.DEFAULT_PAGE_SIZE, sort = PostConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> savedPosts = postService.getSavedPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_SAVED_POSTS_RETRIEVED, savedPosts));
    }

    @GetMapping(PostConstants.POST_BY_ID_PATH)
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable UUID postId) {
        PostResponse response = postService.getPostById(postId);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_POST_RETRIEVED, response));
    }

    @GetMapping(PostConstants.USER_POSTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getUserPosts(
            @PathVariable UUID userId,
            @PageableDefault(size = PostConstants.DEFAULT_PAGE_SIZE, sort = PostConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> posts = postService.getUserPosts(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_USER_POSTS_RETRIEVED, posts));
    }

    @GetMapping(PostConstants.USER_TAGGED_POSTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getUserTaggedPosts(
            @PathVariable UUID userId,
            @PageableDefault(size = PostConstants.DEFAULT_PAGE_SIZE, sort = PostConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> posts = postService.getUserTaggedPosts(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_USER_TAGGED_POSTS_RETRIEVED, posts));
    }

    @PutMapping(PostConstants.POST_BY_ID_PATH)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        PostResponse response = postService.updatePost(postId, request);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_POST_UPDATED, response));
    }

    @DeleteMapping(PostConstants.POST_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success(PostConstants.MSG_POST_DELETED, null));
    }

    }
