package com.instagramclone.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryMentionResponse {

    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;

    @JsonProperty("xPercent")
    private Double xPercent;

    @JsonProperty("yPercent")
    private Double yPercent;
}
