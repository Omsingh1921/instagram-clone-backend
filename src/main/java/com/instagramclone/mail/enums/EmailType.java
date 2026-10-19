package com.instagramclone.mail.enums;

import lombok.Getter;

@Getter
public enum EmailType {

    // 1. Account Category
    WELCOME(
            EmailCategory.ACCOUNT,
            EmailPriority.HIGH,
            true,
            null,
            "Welcome to Instagram Clone",
            3,
            true
    ),
    EMAIL_VERIFICATION(
            EmailCategory.ACCOUNT,
            EmailPriority.CRITICAL,
            true,
            null,
            "Verify your Instagram clone account",
            3,
            true
    ),
    EMAIL_OTP(
            EmailCategory.ACCOUNT,
            EmailPriority.CRITICAL,
            true,
            null,
            "Your Instagram clone verification code",
            3,
            true
    ),
    PASSWORD_RESET(
            EmailCategory.ACCOUNT,
            EmailPriority.CRITICAL,
            true,
            null,
            "Reset your Instagram clone password",
            3,
            true
    ),
    PASSWORD_CHANGED(
            EmailCategory.ACCOUNT,
            EmailPriority.HIGH,
            true,
            null,
            "Security Alert: Your password was changed",
            3,
            true
    ),
    EMAIL_ADDRESS_CHANGED(
            EmailCategory.ACCOUNT,
            EmailPriority.HIGH,
            true,
            null,
            "Security Alert: Your email address was updated",
            3,
            false // Deferred: email modification endpoint not currently in API
    ),
    ACCOUNT_RECOVERY(
            EmailCategory.ACCOUNT,
            EmailPriority.CRITICAL,
            true,
            null,
            "Instagram Clone Account Recovery Options",
            3,
            false // Handled via password reset flow
    ),

    // 2. Security Category
    NEW_LOGIN(
            EmailCategory.SECURITY,
            EmailPriority.HIGH,
            true,
            null,
            "Security Alert: New login to your account",
            3,
            true
    ),
    NEW_DEVICE_LOGIN(
            EmailCategory.SECURITY,
            EmailPriority.HIGH,
            true,
            null,
            "Security Alert: Login from a new device",
            3,
            false // Deferred: device fingerprinting engine not active
    ),
    SUSPICIOUS_LOGIN(
            EmailCategory.SECURITY,
            EmailPriority.CRITICAL,
            true,
            null,
            "CRITICAL: Suspicious login attempt blocked",
            3,
            false // Deferred: automated fraud/risk scoring engine not active
    ),
    SECURITY_SETTINGS_CHANGED(
            EmailCategory.SECURITY,
            EmailPriority.HIGH,
            true,
            null,
            "Security Alert: Your privacy or security settings changed",
            3,
            true
    ),

    // 3. Social Activity Category
    NEW_FOLLOWER(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.MEDIUM,
            false,
            "emailSocialFollows",
            "%s started following you on Instagram",
            2,
            true
    ),
    FOLLOW_REQUEST(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.MEDIUM,
            false,
            "emailSocialFollows",
            "%s requested to follow you",
            2,
            true
    ),
    POST_LIKED(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.LOW,
            false,
            "emailSocialLikes",
            "%s liked your post",
            1,
            true
    ),
    POST_COMMENTED(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.MEDIUM,
            false,
            "emailSocialComments",
            "%s commented on your post",
            2,
            true
    ),
    USER_MENTIONED(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.MEDIUM,
            false,
            "emailSocialComments",
            "%s mentioned you in a comment",
            2,
            false // Deferred: caption mention parser not active
    ),
    USER_TAGGED(
            EmailCategory.SOCIAL_ACTIVITY,
            EmailPriority.MEDIUM,
            false,
            "emailSocialComments",
            "%s tagged you in a photo",
            2,
            false // Deferred: photo tagging coordinates not active
    ),

    // 4. Messaging Category
    UNREAD_MESSAGE_DIGEST(
            EmailCategory.MESSAGING,
            EmailPriority.MEDIUM,
            false,
            "emailMessageReminders",
            "You have unread direct messages on Instagram",
            2,
            true
    ),
    MISSED_MESSAGE_REMINDER(
            EmailCategory.MESSAGING,
            EmailPriority.LOW,
            false,
            "emailMessageReminders",
            "Don't miss messages from your friends",
            1,
            false // Consolidated into UNREAD_MESSAGE_DIGEST
    ),

    // 5. Digest and Product Category
    DAILY_ACTIVITY_DIGEST(
            EmailCategory.DIGEST_PRODUCT,
            EmailPriority.LOW,
            false,
            "emailActivityDigest",
            "Your Daily Instagram Clone Highlights",
            1,
            false // Deferred: daily batching cron job
    ),
    WEEKLY_ACTIVITY_DIGEST(
            EmailCategory.DIGEST_PRODUCT,
            EmailPriority.LOW,
            false,
            "emailActivityDigest",
            "Your Weekly Instagram Clone Catch-Up",
            1,
            false // Deferred: weekly batching cron job
    ),
    PRODUCT_ANNOUNCEMENT(
            EmailCategory.DIGEST_PRODUCT,
            EmailPriority.LOW,
            false,
            "emailProductAnnouncements",
            "New on Instagram Clone: %s",
            1,
            true
    );

    private final EmailCategory category;
    private final EmailPriority defaultPriority;
    private final boolean mandatory;
    private final String preferenceField;
    private final String defaultSubject;
    private final int maxRetries;
    private final boolean implemented;

    EmailType(
            EmailCategory category,
            EmailPriority defaultPriority,
            boolean mandatory,
            String preferenceField,
            String defaultSubject,
            int maxRetries,
            boolean implemented
    ) {
        this.category = category;
        this.defaultPriority = defaultPriority;
        this.mandatory = mandatory;
        this.preferenceField = preferenceField;
        this.defaultSubject = defaultSubject;
        this.maxRetries = maxRetries;
        this.implemented = implemented;
    }

    public boolean isSecurityCritical() {
        return category == EmailCategory.SECURITY || category == EmailCategory.ACCOUNT;
    }

    public boolean requiresUnsubscribeFooter() {
        return !mandatory;
    }
}
