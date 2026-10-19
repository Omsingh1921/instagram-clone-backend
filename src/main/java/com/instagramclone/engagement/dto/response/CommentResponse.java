package com.instagramclone.engagement.dto.response;

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
public class CommentResponse {

    private UUID id;
    private UUID postId;
    private CommentAuthorResponse author;
    private UUID parentCommentId;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
