package com.instagramclone.media.service.interfaces;

import com.instagramclone.media.dto.response.MediaResponse;
import com.instagramclone.post.dto.response.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IMediaService {

    MediaResponse uploadMedia(MultipartFile file, String category);

    MediaUploadResponse uploadMediaAsUploadResponse(MultipartFile file, String category);

    MediaResponse getMedia(UUID mediaId);

    void deleteMedia(UUID mediaId);

    void deleteByPublicId(String publicId, String resourceType);

    void deleteByUrl(String url);

    String getThumbnailUrl(String publicId, String resourceType);

    String getAvatarTransformedUrl(String publicId);
}
