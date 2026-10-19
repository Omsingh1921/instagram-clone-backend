package com.instagramclone.post.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.post.enums.MediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.instagramclone.common.constants.ValidationConstants;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class PostMediaRequest {

    @NotBlank(message = ValidationConstants.MEDIA_URL_REQUIRED)
    @Pattern(regexp = ValidationConstants.MEDIA_URL_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.MEDIA_URL_MAX_LENGTH, message = ValidationConstants.MEDIA_URL_MAX_LENGTH_MESSAGE)
    private String mediaUrl;

    @NotNull(message = ValidationConstants.MEDIA_TYPE_REQUIRED)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @NotNull(message = ValidationConstants.DISPLAY_ORDER_REQUIRED)
    @Min(value = 0, message = ValidationConstants.DISPLAY_ORDER_MIN_MESSAGE)
    @Builder.Default
    private Integer displayOrder = 0;

    @Min(value = 1, message = ValidationConstants.WIDTH_MIN_MESSAGE)
    private Integer width;

    @Min(value = 1, message = ValidationConstants.HEIGHT_MIN_MESSAGE)
    private Integer height;

    @Valid
    @Builder.Default
    private List<PostMediaTagRequest> tags = new ArrayList<>();
}
