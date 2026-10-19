package com.instagramclone.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom authentication provider extending DaoAuthenticationProvider to enforce
 * timing equalization across all authentication failure modes.
 *
 * Prevents account-state and account-existence enumeration by ensuring that accounts
 * failing pre-authentication checks (locked, inactive, disabled, or banned) perform
 * password hashing verification against a dummy hash, matching the execution latency
 * of nonexistent users and bad-credential verification (~150-250ms).
 */
@Slf4j
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public CustomAuthenticationProvider(CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super();
        this.passwordEncoder = passwordEncoder;
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.dummyPasswordHash = passwordEncoder.encode("InstagramCloneDummyPasswordForTimingMitigation!");

        setPreAuthenticationChecks(toCheck -> {
            if (!toCheck.isAccountNonLocked()) {
                throw new org.springframework.security.authentication.LockedException("User account is locked");
            }
            if (!toCheck.isAccountNonExpired()) {
                throw new org.springframework.security.authentication.AccountExpiredException("User account has expired");
            }
            if (toCheck instanceof UserPrincipal up) {
                if (up.isSuspended()) {
                    throw new org.springframework.security.authentication.DisabledException("User account is suspended");
                }
                if (up.isDeleted() && !up.isEligibleForGraceReactivation()) {
                    throw new org.springframework.security.authentication.DisabledException("User account is permanently deleted");
                }
                if (!up.isEnabled() && !up.isDeactivated() && !(up.isDeleted() && up.isEligibleForGraceReactivation())) {
                    throw new org.springframework.security.authentication.DisabledException("User account is disabled");
                }
                // Allow deactivated accounts and soft-deleted accounts within grace period to proceed to credential verification
            } else if (!toCheck.isEnabled()) {
                throw new org.springframework.security.authentication.DisabledException("User account is disabled");
            }
        });
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            return super.authenticate(authentication);
        } catch (AccountStatusException ex) {
            // Mitigate timing side-channel:
            // When an account is locked or disabled, Spring Security's preAuthenticationChecks throws before
            // password hashing occurs (~2ms vs ~200ms).
            // Performing a dummy BCrypt hash verification equalizes response time with nonexistent
            // and bad-credential login attempts, preventing account state enumeration.
            performDummyPasswordCheck(authentication);
            throw ex;
        }
    }

    private void performDummyPasswordCheck(Authentication authentication) {
        String presentedPassword = (authentication != null && authentication.getCredentials() != null)
                ? authentication.getCredentials().toString()
                : "";
        passwordEncoder.matches(presentedPassword, dummyPasswordHash);
    }
}
