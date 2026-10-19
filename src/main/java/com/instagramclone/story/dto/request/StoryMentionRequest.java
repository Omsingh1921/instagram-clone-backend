package com.instagramclone.story.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryMentionRequest {

    @NotNull(message = "User ID is required for mention")
    private UUID userId;

    @JsonProperty("xPercent")
    private Double xPercent;

    @JsonProperty("yPercent")
    private Double yPercent;
}
