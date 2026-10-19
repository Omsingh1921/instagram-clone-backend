package com.instagramclone.mail.enums;

public enum EmailDeliveryStatus {
    QUEUED,
    PROCESSING,
    SENT_TO_PROVIDER,
    FAILED,
    RETRYING,
    SKIPPED_PREFERENCE,
    SKIPPED_THROTTLED,
    DELIVERED_SIMULATED
}
