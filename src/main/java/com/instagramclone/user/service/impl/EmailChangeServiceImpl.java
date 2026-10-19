package com.instagramclone.user.service.impl;

import com.instagramclone.user.service.interfaces.*;

import com.instagramclone.audit.service.interfaces.IAuditLogService;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ConflictException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventPublisher;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.security.JwtProperties;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.user.constants.SettingsConstants;
import com.instagramclone.user.dto.request.ConfirmEmailChangeRequest;
import com.instagramclone.user.dto.response.EmailChangeResponse;
import com.instagramclone.user.dto.request.InitiateEmailChangeRequest;
import com.instagramclone.user.entity.EmailChangeRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.EmailChangeStatus;
import com.instagramclone.user.repository.EmailChangeRequestRepository;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailChangeServiceImpl implements IEmailChangeService {

    private static final Logger log = LoggerFactory.getLogger(EmailChangeServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailEventPublisher emailEventPublisher;
    private final MailProperties mailProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ITokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;
    private final IAuditLogService auditLogService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public EmailChangeResponse initiateEmailChange(UUID userId, InitiateEmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SettingsConstants.ERR_USER_NOT_FOUND_ID + userId));

        // 1. Password verification
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(SettingsConstants.ERR_CURRENT_PASSWORD_PROVIDED_INCORRECT);
        }

        // 2. Normalize and validate new email
        String newEmail = request.getNewEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (newEmail.equalsIgnoreCase(user.getEmail().trim().toLowerCase(java.util.Locale.ROOT))) {
            throw new BadRequestException(SettingsConstants.ERR_NEW_EMAIL_SAME_AS_CURRENT);
        }

        if (userRepository.countByEmail(newEmail) >= 3) {
            throw new ConflictException(SettingsConstants.ERR_EMAIL_ACCOUNTS_LIMIT_REACHED);
        }

        // 3. Concurrency guard: cancel previous pending requests for this user
        emailChangeRequestRepository.cancelAllPendingForUser(userId);

        // 4. Generate blind verification token
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(mailProperties.getResetTokenValidityMinutes(), ChronoUnit.MINUTES);

        EmailChangeRequest changeRequest = EmailChangeRequest.builder()
                .user(user)
                .newEmail(newEmail)
                .tokenHash(tokenHash)
                .status(EmailChangeStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
        emailChangeRequestRepository.save(changeRequest);

        // 5. Send verification email to NEW address
        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EMAIL_CHANGE_VERIFICATION_REQUESTED")
                .emailType(EmailType.EMAIL_ADDRESS_CHANGED)
                .userId(user.getId())
                .email(newEmail)
                .username(user.getUsername())
                .rawToken(rawToken)
                .variables(Map.of(
                        "username", user.getUsername(),
                        "newEmail", newEmail,
                        "oldEmail", user.getEmail(),
                        "confirmUrl", mailProperties.getVerificationUrl() + "?confirmEmailChange=" + rawToken,
                        "action", "CONFIRM_NEW_EMAIL"
                ))
                .timestamp(LocalDateTime.now())
                .build());

        // 6. Send security alert to OLD/CURRENT address
        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EMAIL_CHANGE_INITIATED_ALERT")
                .emailType(EmailType.EMAIL_ADDRESS_CHANGED)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(Map.of(
                        "username", user.getUsername(),
                        "newEmail", newEmail,
                        "oldEmail", user.getEmail(),
                        "action", "INITIATED_ALERT"
                ))
                .timestamp(LocalDateTime.now())
                .build());

        auditLogService.record(userId, "EMAIL_CHANGE_INITIATED", "USER", userId, null, "Email change requested to " + newEmail);
        log.info("Email change initiated for user [{}]. Verification sent to [{}] and alert to [{}]",
                user.getUsername(), newEmail, user.getEmail());

        return EmailChangeResponse.builder()
                .message(SettingsConstants.MSG_EMAIL_CHANGE_VERIFICATION_SENT)
                .currentEmail(user.getEmail())
                .pendingEmail(newEmail)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public void confirmEmailChange(ConfirmEmailChangeRequest request) {
        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        EmailChangeRequest changeRequest = emailChangeRequestRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(SettingsConstants.ERR_INVALID_EMAIL_CHANGE_TOKEN));

        if (!changeRequest.isPending()) {
            throw new BadRequestException(SettingsConstants.ERR_EMAIL_CHANGE_REQUEST_EXPIRED_OR_PROCESSED);
        }

        String newEmail = changeRequest.getNewEmail().trim().toLowerCase(java.util.Locale.ROOT);

        synchronized (newEmail.intern()) {
            org.springframework.transaction.support.TransactionTemplate tx =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            tx.executeWithoutResult(status -> executeConfirmEmailChange(tokenHash, newEmail));
        }
    }

    private void executeConfirmEmailChange(String tokenHash, String newEmail) {
        EmailChangeRequest changeRequest = emailChangeRequestRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(SettingsConstants.ERR_INVALID_EMAIL_CHANGE_TOKEN));

        if (!changeRequest.isPending()) {
            throw new BadRequestException(SettingsConstants.ERR_EMAIL_CHANGE_REQUEST_EXPIRED_OR_PROCESSED);
        }

        // Concurrency check: ensure new email hasn't reached 3 accounts in the meantime
        java.util.List<Integer> usedSlots = userRepository.findAccountSlotsByEmail(newEmail);
        if (usedSlots.size() >= 3) {
            changeRequest.setStatus(EmailChangeStatus.CANCELLED);
            emailChangeRequestRepository.save(changeRequest);
            throw new ConflictException(SettingsConstants.ERR_EMAIL_ACCOUNTS_LIMIT_REACHED);
        }

        int nextSlot = java.util.stream.IntStream.rangeClosed(1, 3)
                .filter(s -> !usedSlots.contains(s))
                .findFirst()
                .orElseThrow(() -> new ConflictException(SettingsConstants.ERR_EMAIL_ACCOUNTS_LIMIT_REACHED));

        User user = changeRequest.getUser();
        String oldEmail = user.getEmail();

        // 1. Switch email, assign slot, and mark verified
        user.setEmail(newEmail);
        user.setAccountSlot(nextSlot);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("Database constraint violation during email change confirmation to [{}]: {}", newEmail, dive.getMessage());
            throw new ConflictException(SettingsConstants.ERR_EMAIL_ACCOUNTS_LIMIT_REACHED);
        }

        // 2. Mark request completed
        changeRequest.setStatus(EmailChangeStatus.COMPLETED);
        changeRequest.setUsedAt(LocalDateTime.now());
        emailChangeRequestRepository.save(changeRequest);

        // 3. Security requirement: Revoke active sessions across cluster
        refreshTokenRepository.revokeAllUserTokens(user.getId(), LocalDateTime.now());
        tokenBlacklistService.blacklistUser(user.getId(), jwtProperties.getAccessExpirationMs());
        log.info("Terminated active sessions for user [{}] after successful email change", user.getUsername());

        // 4. Send completion alerts to both addresses
        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EMAIL_CHANGE_COMPLETED_OLD_ALERT")
                .emailType(EmailType.EMAIL_ADDRESS_CHANGED)
                .userId(user.getId())
                .email(oldEmail)
                .username(user.getUsername())
                .variables(Map.of(
                        "username", user.getUsername(),
                        "oldEmail", oldEmail,
                        "newEmail", newEmail,
                        "action", "COMPLETED_ALERT"
                ))
                .timestamp(LocalDateTime.now())
                .build());

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EMAIL_CHANGE_COMPLETED_NEW_CONFIRM")
                .emailType(EmailType.EMAIL_ADDRESS_CHANGED)
                .userId(user.getId())
                .email(newEmail)
                .username(user.getUsername())
                .variables(Map.of(
                        "username", user.getUsername(),
                        "oldEmail", oldEmail,
                        "newEmail", newEmail,
                        "action", "COMPLETED_CONFIRMATION"
                ))
                .timestamp(LocalDateTime.now())
                .build());

        auditLogService.record(user.getId(), "EMAIL_CHANGE_COMPLETED", "USER", user.getId(), null,
                "Email changed from " + oldEmail + " to " + newEmail);
        log.info("Email address successfully changed for user [{}] from [{}] to [{}]", user.getUsername(), oldEmail, newEmail);
    }

    @Override
    @Transactional
    public void cancelPendingEmailChange(UUID userId) {
        int cancelled = emailChangeRequestRepository.cancelAllPendingForUser(userId);
        log.info("Cancelled {} pending email change requests for user [{}]", cancelled, userId);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
