package com.instagramclone.security.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Common security principal payload representing the authenticated user context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPrincipal {

    private UUID userId;
    private String username;
    private String role;
    private boolean isEnabled;
    private boolean isAccountNonLocked;
}
