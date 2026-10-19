package com.instagramclone.reel.dto.response;

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
public class ReelCommentResponse {

    private UUID id;
    private UUID reelId;
    private PostAuthorResponse author;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
