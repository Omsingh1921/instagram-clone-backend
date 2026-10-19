package com.instagramclone.reel.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class CreateReelRequest {

    @NotBlank(message = ValidationConstants.VIDEO_URL_REQUIRED)
    @Pattern(regexp = ValidationConstants.MEDIA_URL_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.MEDIA_URL_MAX_LENGTH, message = ValidationConstants.VIDEO_URL_MAX_LENGTH_MESSAGE)
    private String videoUrl;

    @Pattern(regexp = ValidationConstants.MEDIA_URL_OR_EMPTY_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.MEDIA_URL_MAX_LENGTH, message = ValidationConstants.THUMBNAIL_URL_MAX_LENGTH_MESSAGE)
    private String thumbnailUrl;

    @Size(max = ValidationConstants.CAPTION_MAX_LENGTH, message = ValidationConstants.CAPTION_MAX_LENGTH_MESSAGE)
    private String caption;

    @Min(value = 0, message = ValidationConstants.DURATION_MIN_MESSAGE)
    @Builder.Default
    private Long durationMs = 0L;
}
