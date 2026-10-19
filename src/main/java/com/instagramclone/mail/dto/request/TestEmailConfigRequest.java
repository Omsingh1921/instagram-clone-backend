package com.instagramclone.mail.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.mail.enums.EmailCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class TestEmailConfigRequest {

    @NotNull(message = "Email category/purpose is required")
    private EmailCategory category;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient must be a valid email address")
    private String recipientEmail;
}
