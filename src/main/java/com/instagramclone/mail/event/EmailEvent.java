package com.instagramclone.mail.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.instagramclone.mail.enums.EmailPriority;
import com.instagramclone.mail.enums.EmailType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent implements Serializable {

    private String eventId;
    private String eventType;
    private UUID userId;
    private String email;
    private String username;
    private String rawToken;
    private LocalDateTime timestamp;

    private EmailType emailType;
    private EmailPriority priority;
    private Map<String, Object> variables;
}
