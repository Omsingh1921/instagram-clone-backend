package com.instagramclone.user.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.user.annotation.ValidBio;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class UpdateProfileRequest {

    @Pattern(regexp = ValidationConstants.USERNAME_REGEX, message = ValidationConstants.USERNAME_PATTERN_MESSAGE)
    @Size(min = ValidationConstants.USERNAME_MIN_LENGTH, max = ValidationConstants.USERNAME_MAX_LENGTH, message = ValidationConstants.USERNAME_LENGTH_MESSAGE)
    private String username;

    @Pattern(regexp = ValidationConstants.DISPLAY_NAME_REGEX, message = ValidationConstants.DISPLAY_NAME_PATTERN_MESSAGE)
    @Size(max = ValidationConstants.DISPLAY_NAME_MAX_LENGTH, message = ValidationConstants.DISPLAY_NAME_MAX_LENGTH_MESSAGE)
    private String displayName;

    @ValidBio
    private String bio;

    @Pattern(regexp = ValidationConstants.WEBSITE_URL_REGEX, message = ValidationConstants.URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.WEBSITE_URL_MAX_LENGTH, message = ValidationConstants.WEBSITE_URL_MAX_LENGTH_MESSAGE)
    private String websiteUrl;
}
