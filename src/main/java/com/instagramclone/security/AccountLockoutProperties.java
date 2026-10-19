package com.instagramclone.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.lockout")
public class AccountLockoutProperties {
    /**
     * Maximum consecutive failed login attempts before an account is locked.
     */
    private int maxFailedAttempts = 5;

    /**
     * Duration in minutes for which the account remains locked.
     */
    private int lockoutDurationMinutes = 15;
}
