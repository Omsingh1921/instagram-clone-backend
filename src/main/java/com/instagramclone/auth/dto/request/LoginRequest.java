package com.instagramclone.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class LoginRequest {

    @NotBlank(message = ValidationConstants.USERNAME_OR_EMAIL_REQUIRED)
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.USERNAME_OR_EMAIL_MAX_LENGTH_MESSAGE)
    @JsonAlias({"username", "email", "emailOrUsername"})
    private String usernameOrEmail;

    @NotBlank(message = ValidationConstants.PASSWORD_REQUIRED)
    @Size(max = ValidationConstants.PASSWORD_MAX_LENGTH, message = ValidationConstants.PASSWORD_MAX_LENGTH_MESSAGE)
    private String password;
}
