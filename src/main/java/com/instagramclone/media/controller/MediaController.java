package com.instagramclone.media.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.media.constants.MediaConstants;
import com.instagramclone.media.dto.response.MediaResponse;
import com.instagramclone.media.service.interfaces.IMediaService;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.utility.ApiUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Dedicated Media REST controller adhering to Section 33 (/api/v1/media).
 */
@RestController
@RequestMapping(MediaConstants.BASE_PATH)
@RequiredArgsConstructor
@Tag(name = MediaConstants.TAG_MEDIA, description = MediaConstants.TAG_MEDIA_DESCRIPTION)
public class MediaController {

    private final IMediaService mediaService;

    @RateLimit(category = MediaConstants.RATE_LIMIT_MEDIA_UPLOAD, limit = 15, windowSeconds = 60)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media", description = "Uploads an image or video to Cloudinary with secure metadata storage")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestParam(MediaConstants.PARAM_FILE) MultipartFile file,
            @RequestParam(value = MediaConstants.PARAM_CATEGORY, required = false, defaultValue = MediaConstants.DEFAULT_CATEGORY) String category) {
        MediaResponse response = mediaService.uploadMedia(file, category);
        return ApiUtility.ok(MediaConstants.MSG_MEDIA_UPLOADED, response);
    }

    @GetMapping(MediaConstants.MEDIA_BY_ID_PATH)
    @Operation(summary = "Get media metadata", description = "Retrieves stored Cloudinary media metadata by UUID")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(@PathVariable UUID mediaId) {
        MediaResponse response = mediaService.getMedia(mediaId);
        return ApiUtility.ok(response);
    }

    @DeleteMapping(MediaConstants.MEDIA_BY_ID_PATH)
    @Operation(summary = "Delete media", description = "Deletes media asset from Cloudinary and removes database record")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ApiUtility.ok(MediaConstants.MSG_MEDIA_DELETED, null);
    }
}
