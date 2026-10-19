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
public class SignupRequestOtpRequest {

    @NotBlank(message = ValidationConstants.EMAIL_REQUIRED)
    @Email(message = ValidationConstants.EMAIL_FORMAT_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.EMAIL_REGEX,
        message = ValidationConstants.EMAIL_FORMAT_MESSAGE
    )
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_MAX_LENGTH_MESSAGE)
    private String email;
}
