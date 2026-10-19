package com.instagramclone.common.constants;

/**
 * Shared Kafka topic and messaging constants adhering to Section 50.4 (Common Constants).
 */
public final class KafkaConstants {

    private KafkaConstants() {}

    public static final String DEFAULT_TOPIC = "instagram-clone.events";
    public static final String DEFAULT_GROUP_ID = "instagram-clone-group";
    public static final String EVENT_USER_REGISTERED = "USER_REGISTERED";
    public static final String EVENT_USER_FOLLOWED = "USER_FOLLOWED";
    public static final String EVENT_POST_CREATED = "POST_CREATED";
    public static final String EVENT_POST_LIKED = "POST_LIKED";
    public static final String EVENT_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String EVENT_EMAIL_VERIFICATION_REQUESTED = "EMAIL_VERIFICATION_REQUESTED";
    public static final String EVENT_EMAIL_OTP_REQUESTED = "EMAIL_OTP_REQUESTED";
    public static final String EVENT_PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String EVENT_PASSWORD_CHANGED_SECURITY_ALERT = "PASSWORD_CHANGED_SECURITY_ALERT";
}
