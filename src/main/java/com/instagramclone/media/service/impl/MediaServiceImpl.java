package com.instagramclone.media.service.impl;

import com.instagramclone.media.service.interfaces.*;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.integration.cloudinary.CloudinaryService;
import com.instagramclone.media.constants.MediaConstants;
import com.instagramclone.media.dto.response.MediaResponse;
import com.instagramclone.media.entity.Media;
import com.instagramclone.media.repository.MediaRepository;
import com.instagramclone.post.dto.response.MediaUploadResponse;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.utility.FileUtility;
import com.instagramclone.utility.SecurityUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates media upload, metadata persistence, and lifecycle adhering to Sections 6, 10, 14, 23 & 34.
 */
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements IMediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final CloudinaryService cloudinaryService;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, String category) {
        UUID currentUserId = SecurityUtility.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND));

        // 1. Validation adhering to Sections 7, 8 & 28
        FileUtility.validateFile(file, true);

        String contentType = file.getContentType();
        boolean isVideo = FileUtility.ALLOWED_VIDEO_TYPES.contains(contentType != null ? contentType.toLowerCase() : "");
        MediaType mediaType = isVideo ? MediaType.VIDEO : MediaType.IMAGE;
        String resourceType = isVideo ? "video" : "image";

        // 2. Predictable UUID-partitioned Cloudinary folder adhering to Section 10
        String safeCategory = (category != null && !category.isBlank()) ? category.toLowerCase().trim() : "general";
        String folder;
        switch (safeCategory) {
            case "avatar", "profile" -> folder = String.format("instagram-clone/users/%s/avatar", currentUserId);
            case "stories", "story" -> folder = String.format("instagram-clone/stories/%s", currentUserId);
            case "reels", "reel" -> folder = String.format("instagram-clone/reels/%s", currentUserId);
            case "posts", "post" -> folder = String.format("instagram-clone/posts/%s", currentUserId);
            default -> folder = String.format("instagram-clone/%s/%s", safeCategory, currentUserId);
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            log.error("Failed to read upload file bytes: {}", ex.getMessage());
            throw new BadRequestException(MediaConstants.ERR_PROCESS_FILE_BYTES);
        }

        // 3. Upload to Cloudinary adhering to Section 6
        Map<String, Object> uploadResult = cloudinaryService.upload(fileBytes, folder, resourceType, file.getOriginalFilename());

        String publicId = (String) uploadResult.get("public_id");
        String secureUrl = (String) uploadResult.getOrDefault("secure_url", uploadResult.get("url"));
        String format = (String) uploadResult.get("format");
        Integer width = uploadResult.get("width") instanceof Number n ? n.intValue() : null;
        Integer height = uploadResult.get("height") instanceof Number n ? n.intValue() : null;
        Double duration = uploadResult.get("duration") instanceof Number n ? n.doubleValue() : null;
        Long fileSize = uploadResult.get("bytes") instanceof Number n ? n.longValue() : file.getSize();

        // 4. Persist Media metadata to MySQL adhering to Sections 11 & 12
        Media media = Media.builder()
                .owner(currentUser)
                .publicId(publicId)
                .secureUrl(secureUrl)
                .resourceType(resourceType)
                .mediaType(mediaType)
                .format(format)
                .width(width)
                .height(height)
                .duration(duration)
                .fileSize(fileSize)
                .folder(folder)
                .status("READY")
                .build();

        Media saved;
        try {
            saved = mediaRepository.save(media);
            log.info("Media metadata persisted: id={} publicId={} owner={}", saved.getId(), publicId, currentUserId);
        } catch (Exception ex) {
            log.error("Database save failed for media publicId={}. Attempting Cloudinary cleanup...", publicId, ex);
            try {
                cloudinaryService.delete(publicId, resourceType);
                log.info("Successfully cleaned up orphaned Cloudinary asset publicId={}", publicId);
            } catch (Exception cleanEx) {
                log.error("Failed to clean up orphaned Cloudinary asset publicId={}: {}", publicId, cleanEx.getMessage());
            }
            throw ex;
        }

        String thumbnailUrl = isVideo
                ? cloudinaryService.getVideoThumbnailUrl(publicId)
                : ("avatar".equalsIgnoreCase(safeCategory) || "profile".equalsIgnoreCase(safeCategory)
                        ? cloudinaryService.getAvatarUrl(publicId)
                        : secureUrl);

        return MediaResponse.builder()
                .id(saved.getId())
                .url(secureUrl)
                .secureUrl(secureUrl)
                .mediaUrl(secureUrl)
                .publicId(publicId)
                .mediaType(mediaType)
                .resourceType(resourceType)
                .width(width)
                .height(height)
                .duration(duration)
                .format(format)
                .fileSize(fileSize)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    @Override
    @Transactional
    public MediaUploadResponse uploadMediaAsUploadResponse(MultipartFile file, String category) {
        MediaResponse res = uploadMedia(file, category);
        return MediaUploadResponse.builder()
                .url(res.getSecureUrl())
                .mediaType(res.getMediaType())
                .width(res.getWidth())
                .height(res.getHeight())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(MediaConstants.ERR_MEDIA_NOT_FOUND + mediaId));

        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getSecureUrl())
                .secureUrl(media.getSecureUrl())
                .mediaUrl(media.getSecureUrl())
                .publicId(media.getPublicId())
                .mediaType(media.getMediaType())
                .resourceType(media.getResourceType())
                .width(media.getWidth())
                .height(media.getHeight())
                .duration(media.getDuration())
                .format(media.getFormat())
                .fileSize(media.getFileSize())
                .build();
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId) {
        UUID currentUserId = SecurityUtility.getCurrentUserId();
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(MediaConstants.ERR_MEDIA_NOT_FOUND + mediaId));

        if (!media.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException(MediaConstants.ERR_UNAUTHORIZED_DELETE);
        }

        // 1. Delete Cloudinary asset adhering to Section 23
        cloudinaryService.delete(media.getPublicId(), media.getResourceType());

        // 2. Update status and delete record adhering to Section 34
        media.setStatus("DELETED");
        mediaRepository.delete(media);
        log.info("Media deleted: id={} publicId={}", mediaId, media.getPublicId());
    }

    @Override
    @Transactional
    public void deleteByPublicId(String publicId, String resourceType) {
        if (publicId != null && !publicId.isBlank()) {
            cloudinaryService.delete(publicId, resourceType);
            mediaRepository.findByPublicId(publicId).ifPresent(mediaRepository::delete);
        }
    }

    @Override
    @Transactional
    public void deleteByUrl(String url) {
        if (url != null && !url.isBlank()) {
            Optional<Media> mediaOpt = mediaRepository.findBySecureUrl(url);
            if (mediaOpt.isPresent()) {
                Media media = mediaOpt.get();
                cloudinaryService.delete(media.getPublicId(), media.getResourceType());
                mediaRepository.delete(media);
            }
        }
    }

    @Override
    public String getThumbnailUrl(String publicId, String resourceType) {
        if ("video".equalsIgnoreCase(resourceType)) {
            return cloudinaryService.getTransformedUrl(publicId, "so_0,w_600,c_limit,q_auto,f_auto.jpg");
        }
        return cloudinaryService.getTransformedUrl(publicId, "w_600,c_limit,q_auto,f_auto");
    }

    @Override
    public String getAvatarTransformedUrl(String publicId) {
        return cloudinaryService.getTransformedUrl(publicId, "w_150,h_150,c_fill,g_face,q_auto,f_auto");
    }
}
