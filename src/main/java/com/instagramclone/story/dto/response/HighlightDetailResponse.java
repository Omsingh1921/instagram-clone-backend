package com.instagramclone.story.dto.response;

import com.instagramclone.post.dto.response.PostAuthorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightDetailResponse {

    private UUID id;
    private String title;
    private String coverImageUrl;
    private PostAuthorResponse author;
    @Builder.Default
    private List<StoryResponse> stories = new java.util.ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
