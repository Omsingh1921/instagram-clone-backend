package com.instagramclone.post.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.response.MediaUploadResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.post.dto.request.UpdatePostRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IPostService {

    PostResponse createPost(CreatePostRequest request);

    PostResponse getPostById(UUID postId);

    PageResponse<PostResponse> getUserPosts(UUID userId, Pageable pageable);

    PageResponse<PostResponse> getUserTaggedPosts(UUID userId, Pageable pageable);

    PageResponse<PostResponse> getSavedPosts(Pageable pageable);

    PostResponse updatePost(UUID postId, UpdatePostRequest request);

    void deletePost(UUID postId);

    long getPostCount(UUID userId);

    MediaUploadResponse uploadMedia(MultipartFile file);
}
