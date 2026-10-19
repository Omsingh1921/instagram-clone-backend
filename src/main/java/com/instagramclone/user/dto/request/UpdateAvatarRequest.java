package com.instagramclone.user.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
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
public class UpdateAvatarRequest {

    @NotBlank(message = ValidationConstants.MEDIA_URL_REQUIRED)
    @Pattern(regexp = ValidationConstants.MEDIA_URL_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.WEBSITE_URL_MAX_LENGTH, message = ValidationConstants.WEBSITE_URL_MAX_LENGTH_MESSAGE)
    private String avatarUrl;
}
