package com.instagramclone.media.dto.response;

import com.instagramclone.post.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Standard Media API response adhering to Section 32 (Media Response Contract).
 * Exposes safe metadata while never leaking Cloudinary secrets or credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private UUID id;
    private String url;
    private String secureUrl;
    private String mediaUrl;
    private String publicId;
    private MediaType mediaType;
    private String resourceType;
    private Integer width;
    private Integer height;
    private Double duration;
    private String format;
    private Long fileSize;
    private String thumbnailUrl;
}
