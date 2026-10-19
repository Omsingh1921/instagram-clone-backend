package com.instagramclone.security;

import com.instagramclone.user.entity.User;

import java.util.UUID;

public interface IAccountLockoutService {

    /**
     * Records a failed login attempt for the given username or email.
     * Increments the failure counter and locks the account if the threshold is met or exceeded.
     * Executes in an isolated transaction so failure tracking commits even when the caller rolls back.
     *
     * @param usernameOrEmail the identifier attempted by the client
     */
    void handleFailedLogin(String usernameOrEmail);

    /**
     * Resets failed login attempts and clears lockout timestamp upon successful authentication.
     *
     * @param user the authenticated user
     */
    void handleSuccessfulLogin(User user);

    /**
     * Checks if the given user is currently locked out.
     *
     * @param user the user to check
     * @return true if currently locked out, false otherwise
     */
    boolean isAccountLocked(User user);

    /**
     * Unlocks an account explicitly (e.g., following a verified password reset).
     *
     * @param userId the ID of the user to unlock
     */
    void unlockAccount(UUID userId);
}
