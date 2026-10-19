package com.instagramclone.mail.dto.response;

import com.instagramclone.mail.enums.EmailCategory;
import com.instagramclone.mail.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicEmailTemplateResponse {

    private UUID id;
    private EmailType emailType;
    private EmailCategory category;
    private String templateName;
    private String description;
    private String subjectTemplate;
    private String bodyHtmlTemplate;
    private String bodyTextTemplate;
    private String availablePlaceholders;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
