package com.instagramclone.auth.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.Email;
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
public class RegisterRequest {

    @Size(max = ValidationConstants.TOKEN_MAX_LENGTH, message = ValidationConstants.SIGNUP_TOKEN_MAX_LENGTH_MESSAGE)
    private String signupToken;

    @NotBlank(message = ValidationConstants.USERNAME_REQUIRED)
    @Size(min = ValidationConstants.USERNAME_MIN_LENGTH, max = ValidationConstants.USERNAME_MAX_LENGTH, message = ValidationConstants.USERNAME_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.USERNAME_REGEX,
        message = ValidationConstants.USERNAME_PATTERN_MESSAGE
    )
    private String username;

    @Email(message = ValidationConstants.EMAIL_INVALID_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_INVALID_MESSAGE
    )
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_MAX_LENGTH_MESSAGE)
    private String email;

    @NotBlank(message = ValidationConstants.PASSWORD_REQUIRED)
    @Size(min = ValidationConstants.PASSWORD_MIN_LENGTH, max = ValidationConstants.PASSWORD_MAX_LENGTH, message = ValidationConstants.PASSWORD_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_PATTERN_MESSAGE
    )
    private String password;

    @Pattern(
        regexp = ValidationConstants.DISPLAY_NAME_REGEX,
        message = ValidationConstants.DISPLAY_NAME_PATTERN_MESSAGE
    )
    @Size(max = ValidationConstants.DISPLAY_NAME_MAX_LENGTH, message = ValidationConstants.DISPLAY_NAME_MAX_LENGTH_MESSAGE)
    private String displayName;
}
