package com.instagramclone.mail.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.mail.enums.EmailCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class DynamicEmailConfigRequest {

    @NotNull(message = "Email category/purpose is required")
    private EmailCategory category;

    @NotBlank(message = "Sender from email is required")
    @Email(message = "From email must be a valid email address")
    @Size(max = 255, message = "From email cannot exceed 255 characters")
    private String fromEmail;

    @NotBlank(message = "Sender name is required")
    @Size(max = 100, message = "Sender name cannot exceed 100 characters")
    private String senderName;

    @Size(max = 100, message = "Purpose name cannot exceed 100 characters")
    private String purposeName;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Email(message = "Reply-to must be a valid email address if provided")
    @Size(max = 255, message = "Reply-to cannot exceed 255 characters")
    private String replyTo;

    @Builder.Default
    private Boolean isEnabled = true;
}
