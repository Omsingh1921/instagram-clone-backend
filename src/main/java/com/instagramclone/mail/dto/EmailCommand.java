package com.instagramclone.mail.dto;

import com.instagramclone.mail.enums.EmailPriority;
import com.instagramclone.mail.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCommand implements Serializable {

    private EmailType emailType;
    private String recipientEmail;
    private UUID recipientUserId;
    private Map<String, Object> variables;
    private EmailPriority priority;
    private String correlationId;
}
