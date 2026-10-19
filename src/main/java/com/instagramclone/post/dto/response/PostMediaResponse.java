package com.instagramclone.post.dto.response;

import com.instagramclone.post.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMediaResponse {

    private UUID id;
    private String mediaUrl;
    private MediaType mediaType;
    private Integer displayOrder;
    private Integer width;
    private Integer height;

    @Builder.Default
    private List<PostMediaTagResponse> tags = new ArrayList<>();
}
