package com.instagramclone.auth.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.NotBlank;
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
public class RefreshTokenRequest {

    @NotBlank(message = ValidationConstants.REFRESH_TOKEN_REQUIRED)
    @Size(max = ValidationConstants.REFRESH_TOKEN_MAX_LENGTH, message = ValidationConstants.REFRESH_TOKEN_MAX_LENGTH_MESSAGE)
    private String refreshToken;
}
