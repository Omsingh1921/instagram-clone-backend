package com.instagramclone.mail.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.mail.enums.EmailType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class UnsubscribeRequest {

    @NotNull(message = ValidationConstants.USER_ID_REQUIRED)
    private UUID userId;

    @NotNull(message = ValidationConstants.EMAIL_TYPE_REQUIRED)
    private EmailType emailType;

    @NotNull(message = ValidationConstants.EXPIRES_REQUIRED)
    @Positive(message = ValidationConstants.EXPIRES_POSITIVE_MESSAGE)
    private Long expires;

    @NotBlank(message = ValidationConstants.SIGNATURE_REQUIRED)
    @Size(max = ValidationConstants.TOKEN_MAX_LENGTH, message = ValidationConstants.SIGNATURE_MAX_LENGTH_MESSAGE)
    private String signature;
}
