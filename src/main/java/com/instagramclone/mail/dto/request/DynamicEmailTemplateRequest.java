package com.instagramclone.mail.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.mail.enums.EmailType;
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
public class DynamicEmailTemplateRequest {

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    @NotBlank(message = "Template subject is required")
    @Size(max = 255, message = "Subject template cannot exceed 255 characters")
    private String subjectTemplate;

    @NotBlank(message = "HTML body template is required")
    private String bodyHtmlTemplate;

    private String bodyTextTemplate;

    @Size(max = 100, message = "Template name cannot exceed 100 characters")
    private String templateName;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Builder.Default
    private Boolean isActive = true;
}
