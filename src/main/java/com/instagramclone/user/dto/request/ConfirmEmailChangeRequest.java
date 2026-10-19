package com.instagramclone.user.dto.request;

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
public class ConfirmEmailChangeRequest {

    @NotBlank(message = ValidationConstants.CONFIRMATION_TOKEN_REQUIRED)
    @Size(max = ValidationConstants.TOKEN_MAX_LENGTH, message = ValidationConstants.CONFIRMATION_TOKEN_MAX_LENGTH_MESSAGE)
    private String token;
}
