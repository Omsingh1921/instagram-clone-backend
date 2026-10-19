package com.instagramclone.mail.dto.response;

import com.instagramclone.mail.enums.EmailCategory;
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
public class DynamicEmailConfigResponse {

    private UUID id;
    private EmailCategory category;
    private String purposeName;
    private String description;
    private String fromEmail;
    private String senderName;
    private String replyTo;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
