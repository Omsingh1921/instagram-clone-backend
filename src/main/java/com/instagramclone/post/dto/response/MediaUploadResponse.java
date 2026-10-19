package com.instagramclone.post.dto.response;

import com.instagramclone.post.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    private String url;
    private MediaType mediaType;
    private Integer width;
    private Integer height;
}
