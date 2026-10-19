package com.instagramclone.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryLikeResponse {
    private UUID storyId;
    private boolean liked;
    private long likeCount;
}
