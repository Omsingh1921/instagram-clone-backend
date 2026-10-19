package com.instagramclone.search.dto.response;

import com.instagramclone.post.dto.response.PostAuthorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreItemResponse {

    private UUID id;
    private String type; // "POST" or "REEL"
    private String mediaUrl;
    private String thumbnailUrl;
    private String caption;
    private long likeCount;
    private long commentCount;
    private PostAuthorResponse author;
    private LocalDateTime createdAt;
}
