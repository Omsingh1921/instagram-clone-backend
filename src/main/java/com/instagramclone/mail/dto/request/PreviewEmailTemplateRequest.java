package com.instagramclone.mail.dto.request;

import com.instagramclone.mail.enums.EmailType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewEmailTemplateRequest {

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    private String subjectTemplate;

    private String bodyHtmlTemplate;

    private String bodyTextTemplate;

    private Map<String, Object> sampleVariables;
}
