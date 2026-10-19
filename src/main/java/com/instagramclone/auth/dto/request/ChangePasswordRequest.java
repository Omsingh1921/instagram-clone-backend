package com.instagramclone.auth.dto.request;

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
public class ChangePasswordRequest {

    @NotBlank(message = ValidationConstants.CURRENT_PASSWORD_REQUIRED)
    @Size(max = ValidationConstants.PASSWORD_MAX_LENGTH, message = ValidationConstants.PASSWORD_MAX_LENGTH_MESSAGE)
    private String currentPassword;

    @NotBlank(message = ValidationConstants.NEW_PASSWORD_REQUIRED)
    @Size(min = ValidationConstants.PASSWORD_MIN_LENGTH, max = ValidationConstants.PASSWORD_MAX_LENGTH, message = ValidationConstants.PASSWORD_LENGTH_RANGE_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_PATTERN_MESSAGE
    )
    private String newPassword;

    @NotBlank(message = ValidationConstants.CONFIRM_PASSWORD_REQUIRED)
    @Size(min = ValidationConstants.PASSWORD_MIN_LENGTH, max = ValidationConstants.PASSWORD_MAX_LENGTH, message = ValidationConstants.PASSWORD_LENGTH_RANGE_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.PASSWORD_REGEX,
        message = ValidationConstants.PASSWORD_PATTERN_MESSAGE
    )
    private String confirmPassword;
}
