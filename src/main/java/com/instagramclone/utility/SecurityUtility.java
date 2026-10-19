package com.instagramclone.utility;

import com.instagramclone.common.constants.SecurityConstants;
import com.instagramclone.common.exception.UnauthorizedException;
import com.instagramclone.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Reusable security helper operations adhering to Section 49.3 (Security Utilities).
 */
public final class SecurityUtility {

    private SecurityUtility() {}

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }
        return Optional.empty();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new UnauthorizedException(SecurityConstants.ERR_USER_NOT_AUTHENTICATED));
    }

    public static Optional<UUID> getCurrentSessionId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getSessionId);
    }

    public static String getCurrentUsername() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUsername)
                .orElseThrow(() -> new UnauthorizedException(SecurityConstants.ERR_USER_NOT_AUTHENTICATED));
    }

    public static boolean isAuthenticated() {
        return getCurrentUserPrincipal().isPresent();
    }

    public static boolean isCurrentUserAdmin() {
        return getCurrentUserPrincipal()
                .map(principal -> principal.getAuthorities() != null && principal.getAuthorities().stream()
                        .anyMatch(a -> SecurityConstants.ROLE_ADMIN.equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority())))
                .orElse(false);
    }
}
