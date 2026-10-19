package com.instagramclone.mail.common;

import com.instagramclone.mail.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Common payload data structure for composing and dispatching outgoing transactional emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessagePayload {

    private String recipient;
    private String subject;
    private EmailType emailType;
    private String templateName;
    private Map<String, Object> templateVariables;
}
