package com.instagramclone.auth.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.instagramclone.common.constants.ValidationConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class ForgotPasswordRequest {

    @NotBlank(message = ValidationConstants.EMAIL_OR_USERNAME_REQUIRED)
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_OR_USERNAME_MAX_LENGTH_MESSAGE)
    private String email;
}
