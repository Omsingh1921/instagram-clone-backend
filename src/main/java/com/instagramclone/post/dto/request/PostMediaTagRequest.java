package com.instagramclone.post.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class PostMediaTagRequest {

    @NotNull(message = "Tagged user ID is required")
    private UUID taggedUserId;

    @DecimalMin(value = "0.0", message = "X coordinate percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "X coordinate percentage cannot exceed 100")
    @JsonProperty("xPercent")
    private Double xPercent;

    @DecimalMin(value = "0.0", message = "Y coordinate percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Y coordinate percentage cannot exceed 100")
    @JsonProperty("yPercent")
    private Double yPercent;
}
