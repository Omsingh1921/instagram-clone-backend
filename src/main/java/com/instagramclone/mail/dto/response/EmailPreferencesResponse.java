package com.instagramclone.mail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailPreferencesResponse {

    private UUID userId;
    private boolean emailSocialFollows;
    private boolean emailSocialLikes;
    private boolean emailSocialComments;
    private boolean emailMessageReminders;
    private boolean emailActivityDigest;
    private boolean emailProductAnnouncements;
    private String digestFrequency;
}
