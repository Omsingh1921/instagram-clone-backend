package com.instagramclone.post.dto.response;

import com.instagramclone.post.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private UUID id;
    private PostAuthorResponse author;
    private String caption;
    private PostVisibility visibility;

    @Builder.Default
    private List<PostMediaResponse> media = new ArrayList<>();

    private long likeCount;
    private long commentCount;
    private Boolean likedByCurrentUser;
    private Boolean savedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
