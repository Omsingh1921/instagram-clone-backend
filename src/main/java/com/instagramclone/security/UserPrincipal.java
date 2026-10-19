package com.instagramclone.security;

import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID sessionId;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;
    private final boolean nonLocked;
    private final boolean emailVerified;
    private final boolean deleted;
    private final boolean deactivated;
    private final boolean suspended;
    private final boolean eligibleForGraceReactivation;

    public static UserPrincipal create(User user) {
        return create(user, null);
    }

    public static UserPrincipal create(User user, UUID sessionId) {
        String roleName = (user.getRole() != null) ? user.getRole().name() : "ROLE_USER";
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        boolean isEligibleForGraceReactivation = user.isWithinDeletionGracePeriod(com.instagramclone.user.constants.UserConstants.ACCOUNT_DELETION_GRACE_PERIOD_DAYS);
        boolean isDeactivated = Boolean.FALSE.equals(user.getIsActive()) && (user.getStatus() == UserStatus.DEACTIVATED || user.getStatus() == UserStatus.INACTIVE);
        boolean isSuspended = user.getStatus() == UserStatus.SUSPENDED;
        boolean isDeleted = Boolean.TRUE.equals(user.getIsDeleted());

        // Strictly enabled for protected resource access only if active, not deleted, and ACTIVE status
        boolean isActive = Boolean.TRUE.equals(user.getIsActive())
                && !isDeleted
                && (user.getStatus() == UserStatus.ACTIVE);
        boolean isLocked = (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(java.time.LocalDateTime.now()));
        boolean isEmailVerified = Boolean.TRUE.equals(user.getEmailVerified());
        return UserPrincipal.builder()
                .id(user.getId())
                .sessionId(sessionId)
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .active(isActive)
                .nonLocked(!isLocked)
                .emailVerified(isEmailVerified)
                .deleted(isDeleted)
                .deactivated(isDeactivated)
                .suspended(isSuspended)
                .eligibleForGraceReactivation(isEligibleForGraceReactivation)
                .build();
    }

    public UserPrincipal withSessionId(UUID sessionId) {
        return this.toBuilder().sessionId(sessionId).build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
