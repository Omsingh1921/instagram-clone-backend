package com.instagramclone.post.dto.response;

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
public class PostMediaTagResponse {

    private UUID id;
    private UUID taggedUserId;
    private String taggedUsername;
    private String displayName;

    @JsonProperty("xPercent")
    private Double xPercent;

    @JsonProperty("yPercent")
    private Double yPercent;
}
