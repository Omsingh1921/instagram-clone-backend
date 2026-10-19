package com.instagramclone.common.constants;

/**
 * Shared security-related constants adhering to Section 50.4 (Common Constants).
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final int TOKEN_PREFIX_LENGTH = 7;
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ERR_USER_NOT_AUTHENTICATED = "User is not authenticated";
}
