package com.instagramclone.security;

import com.instagramclone.audit.service.interfaces.IAuditLogService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutServiceImpl implements IAccountLockoutService {

    private final UserRepository userRepository;
    private final AccountLockoutProperties properties;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return;
        }

        String normalized = usernameOrEmail.trim().toLowerCase(java.util.Locale.ROOT);
        Optional<User> userOpt;
        if (!normalized.contains("@")) {
            userOpt = userRepository.findByUsernameForUpdate(normalized);
        } else {
            java.util.List<User> accounts = userRepository.findAllByEmail(normalized);
            if (accounts.size() == 1) {
                userOpt = userRepository.findByUsernameForUpdate(accounts.get(0).getUsername());
            } else {
                // Multi-account email or nonexistent: do not lock all accounts sharing email
                return;
            }
        }
        if (userOpt.isEmpty()) {
            // Anti-enumeration: Silent return if user does not exist
            return;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // If the account is currently locked, keep lock in place until expiry
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now)) {
            log.warn("Login attempt on already locked account: {}", user.getUsername());
            return;
        }

        // If a previous lockout expired, decay / reset the failed attempts count
        if (user.getLockoutUntil() != null && !user.getLockoutUntil().isAfter(now)) {
            user.setLockoutUntil(null);
            user.setFailedLoginAttempts(0);
        }

        int currentAttempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(currentAttempts);

        if (currentAttempts >= properties.getMaxFailedAttempts()) {
            LocalDateTime lockoutUntil = now.plusMinutes(properties.getLockoutDurationMinutes());
            user.setLockoutUntil(lockoutUntil);
            log.warn("Account [{}] temporarily locked due to {} consecutive failed login attempts. Locked until: {}",
                    user.getUsername(), currentAttempts, lockoutUntil);
            auditLogService.record(
                    user.getId(),
                    "ACCOUNT_LOCKED",
                    "USER",
                    user.getId(),
                    null,
                    "Account locked due to " + currentAttempts + " failed login attempts"
            );
        } else {
            log.info("Recorded failed login attempt {}/{} for user [{}]",
                    currentAttempts, properties.getMaxFailedAttempts(), user.getUsername());
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void handleSuccessfulLogin(User user) {
        if (user == null) {
            return;
        }

        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getLockoutUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
            log.info("Reset failed login attempts and cleared lockout for user [{}]", user.getUsername());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountLocked(User user) {
        if (user == null || user.getLockoutUntil() == null) {
            return false;
        }
        return user.getLockoutUntil().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void unlockAccount(UUID userId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
            log.info("Unlocked account and reset failed attempts for user ID: {}", userId);
            auditLogService.record(userId, "ACCOUNT_UNLOCKED", "USER", userId, null, "Account unlocked");
        });
    }
}
