package com.instagramclone.story.dto.response;

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
public class StoryViewerResponse {

    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime viewedAt;
    private boolean hasLiked;
}
