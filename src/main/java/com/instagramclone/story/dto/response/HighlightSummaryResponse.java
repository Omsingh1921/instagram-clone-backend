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
public class HighlightSummaryResponse {

    private UUID id;
    private String title;
    private String coverImageUrl;
    private int storyCount;
    private LocalDateTime createdAt;
}
