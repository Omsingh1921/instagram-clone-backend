package com.instagramclone.media.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common descriptor for stored or uploaded media items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDescriptor {

    private String url;
    private String publicId;
    private String contentType;
    private long sizeBytes;
    private Integer width;
    private Integer height;
}
