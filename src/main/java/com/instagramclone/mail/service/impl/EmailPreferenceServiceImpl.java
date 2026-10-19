package com.instagramclone.mail.service.impl;

import com.instagramclone.mail.service.interfaces.*;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.constants.MailConstants;
import com.instagramclone.mail.dto.response.EmailPreferencesResponse;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.DigestFrequency;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailPreferenceServiceImpl implements IEmailPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(EmailPreferenceServiceImpl.class);
    private static final String REDIS_EMAIL_PREFS_KEY_PREFIX = "cache:user:email_prefs:";

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final MailProperties mailProperties;
    private final RedisUtility redisUtility;

    @Override
    @Transactional(readOnly = true)
    public EmailPreferencesResponse getPreferences(UUID userId) {
        UserSettings settings = getOrCreateSettings(userId);
        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public EmailPreferencesResponse updatePreferences(UUID userId, UpdateEmailPreferencesRequest request) {
        UserSettings settings = getOrCreateSettings(userId);

        if (request.getEmailSocialFollows() != null) {
            settings.setEmailSocialFollows(request.getEmailSocialFollows());
        }
        if (request.getEmailSocialLikes() != null) {
            settings.setEmailSocialLikes(request.getEmailSocialLikes());
        }
        if (request.getEmailSocialComments() != null) {
            settings.setEmailSocialComments(request.getEmailSocialComments());
        }
        if (request.getEmailMessageReminders() != null) {
            settings.setEmailMessageReminders(request.getEmailMessageReminders());
        }
        if (request.getEmailActivityDigest() != null) {
            settings.setEmailActivityDigest(request.getEmailActivityDigest());
        }
        if (request.getEmailProductAnnouncements() != null) {
            settings.setEmailProductAnnouncements(request.getEmailProductAnnouncements());
        }
        if (request.getDigestFrequency() != null && !request.getDigestFrequency().isBlank()) {
            settings.setDigestFrequency(DigestFrequency.fromValue(request.getDigestFrequency()));
        }

        userSettingsRepository.save(settings);
        invalidateCache(userId);
        log.info("Updated email preferences for user [{}]", userId);
        return mapToResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAllowed(UUID userId, EmailType emailType) {
        if (emailType == null || emailType.isMandatory() || emailType.isSecurityCritical()) {
            return true;
        }
        if (userId == null) {
            return true;
        }

        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        if (settings == null) {
            return true;
        }

        return switch (emailType) {
            case NEW_FOLLOWER, FOLLOW_REQUEST -> settings.isEmailSocialFollows();
            case POST_LIKED -> settings.isEmailSocialLikes();
            case POST_COMMENTED, USER_MENTIONED, USER_TAGGED -> settings.isEmailSocialComments();
            case UNREAD_MESSAGE_DIGEST, MISSED_MESSAGE_REMINDER -> settings.isEmailMessageReminders();
            case DAILY_ACTIVITY_DIGEST, WEEKLY_ACTIVITY_DIGEST -> settings.isEmailActivityDigest();
            case PRODUCT_ANNOUNCEMENT -> settings.isEmailProductAnnouncements();
            default -> true;
        };
    }

    @Override
    public String generateUnsubscribeUrl(UUID userId, EmailType emailType) {
        if (userId == null || emailType == null || emailType.isMandatory()) {
            return null;
        }
        long expires = Instant.now().plus(mailProperties.getUnsubscribeTokenValidityDays(), ChronoUnit.DAYS).getEpochSecond();
        String signature = computeHmacSignature(userId, emailType, expires);
        return "%s?userId=%s&emailType=%s&expires=%d&signature=%s"
                .formatted(mailProperties.getUnsubscribeUrl(), userId, emailType.name(), expires, signature);
    }

    @Override
    @Transactional
    public boolean unsubscribeWithToken(UUID userId, EmailType emailType, long expires, String signature) {
        if (emailType == null || emailType.isMandatory()) {
            throw new BadRequestException(MailConstants.ERR_CANNOT_UNSUBSCRIBE_MANDATORY);
        }
        if (Instant.now().getEpochSecond() > expires) {
            throw new BadRequestException(MailConstants.ERR_UNSUBSCRIBE_LINK_EXPIRED);
        }

        String expectedSignature = computeHmacSignature(userId, emailType, expires);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException(MailConstants.ERR_INVALID_UNSUBSCRIBE_SIGNATURE);
        }

        UserSettings settings = getOrCreateSettings(userId);
        applyPreferenceToggle(settings, emailType, false);
        userSettingsRepository.save(settings);
        invalidateCache(userId);

        log.info("Successfully processed unauthenticated unsubscribe for user [{}] and email type [{}]", userId, emailType);
        return true;
    }

    private UserSettings getOrCreateSettings(UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + userId));
                    UserSettings newSettings = UserSettings.builder()
                            .user(user)
                            .build();
                    return userSettingsRepository.save(newSettings);
                });
    }

    private void applyPreferenceToggle(UserSettings settings, EmailType emailType, boolean enabled) {
        switch (emailType) {
            case NEW_FOLLOWER, FOLLOW_REQUEST -> settings.setEmailSocialFollows(enabled);
            case POST_LIKED -> settings.setEmailSocialLikes(enabled);
            case POST_COMMENTED, USER_MENTIONED, USER_TAGGED -> settings.setEmailSocialComments(enabled);
            case UNREAD_MESSAGE_DIGEST, MISSED_MESSAGE_REMINDER -> settings.setEmailMessageReminders(enabled);
            case DAILY_ACTIVITY_DIGEST, WEEKLY_ACTIVITY_DIGEST -> settings.setEmailActivityDigest(enabled);
            case PRODUCT_ANNOUNCEMENT -> settings.setEmailProductAnnouncements(enabled);
            default -> {}
        }
    }

    private void invalidateCache(UUID userId) {
        if (redisUtility.isAvailable()) {
            redisUtility.delete(REDIS_EMAIL_PREFS_KEY_PREFIX + userId);
        }
    }

    private String computeHmacSignature(UUID userId, EmailType emailType, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    mailProperties.getUnsubscribeSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            String payload = userId + ":" + emailType.name() + ":" + expires;
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception ex) {
            throw new IllegalStateException(MailConstants.ERR_HMAC_SIGNATURE_FAILED, ex);
        }
    }

    private EmailPreferencesResponse mapToResponse(UserSettings s) {
        return EmailPreferencesResponse.builder()
                .userId(s.getUser().getId())
                .emailSocialFollows(s.isEmailSocialFollows())
                .emailSocialLikes(s.isEmailSocialLikes())
                .emailSocialComments(s.isEmailSocialComments())
                .emailMessageReminders(s.isEmailMessageReminders())
                .emailActivityDigest(s.isEmailActivityDigest())
                .emailProductAnnouncements(s.isEmailProductAnnouncements())
                .digestFrequency(s.getDigestFrequency() != null ? s.getDigestFrequency().name() : null)
                .build();
    }
}
