package com.instagramclone.story.dto.response;

import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.post.enums.MediaType;
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
public class StoryResponse {

    private UUID id;
    private PostAuthorResponse author;
    private String mediaUrl;
    private MediaType mediaType;
    private String caption;
    @Builder.Default
    private java.util.List<StoryMentionResponse> mentions = new java.util.ArrayList<>();
    private LocalDateTime expiresAt;
    private boolean viewed;
    private long viewCount;
    private boolean liked;
    private long likeCount;
    private boolean closeFriendsOnly;
    private StoryPollResponse poll;
    private LocalDateTime createdAt;
}
