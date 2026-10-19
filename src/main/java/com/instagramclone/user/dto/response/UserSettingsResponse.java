package com.instagramclone.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    private String username;
    private PrivacySettings privacy;
    private NotificationPreferences notifications;
    private String accountStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivacySettings {
        private boolean privateAccount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences {
        private boolean likes;
        private boolean comments;
        private boolean followers;
        private boolean messages;
    }
}
