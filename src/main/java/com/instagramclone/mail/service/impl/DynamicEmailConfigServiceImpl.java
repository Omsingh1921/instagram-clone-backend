package com.instagramclone.mail.service.impl;

import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.dto.request.DynamicEmailConfigRequest;
import com.instagramclone.mail.dto.request.TestEmailConfigRequest;
import com.instagramclone.mail.dto.response.DynamicEmailConfigResponse;
import com.instagramclone.mail.entity.DynamicEmailConfig;
import com.instagramclone.mail.enums.EmailCategory;
import com.instagramclone.mail.provider.IEmailProvider;
import com.instagramclone.mail.repository.DynamicEmailConfigRepository;
import com.instagramclone.mail.service.interfaces.IDynamicEmailConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DynamicEmailConfigServiceImpl implements IDynamicEmailConfigService {

    private static final Logger log = LoggerFactory.getLogger(DynamicEmailConfigServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "email:sender:";

    private final DynamicEmailConfigRepository dynamicEmailConfigRepository;
    private final MailProperties mailProperties;
    private final IEmailProvider emailProvider;
    private final Optional<StringRedisTemplate> redisTemplate;

    @PostConstruct
    public void onStartup() {
        try {
            initDefaultConfigs();
        } catch (Exception ex) {
            log.warn("Could not initialize default dynamic email configs: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void initDefaultConfigs() {
        for (EmailCategory cat : EmailCategory.values()) {
            if (!dynamicEmailConfigRepository.existsByCategory(cat)) {
                MailProperties.SenderIdentity defaultSender = mailProperties.getSenderForCategory(cat);
                String purpose = switch (cat) {
                    case ACCOUNT -> "Account & Authentication";
                    case SECURITY -> "Security Alerts & Protection";
                    case SOCIAL_ACTIVITY -> "Social Notifications & Interactions";
                    case MESSAGING -> "Direct Messaging & Reminders";
                    case DIGEST_PRODUCT -> "Product News & Digests";
                };
                String desc = switch (cat) {
                    case ACCOUNT -> "Handles verification emails, OTP codes, password reset requests, and welcome emails";
                    case SECURITY -> "Handles new login alerts, new device alerts, and account security changes";
                    case SOCIAL_ACTIVITY -> "Handles follower alerts, likes, comments, and mention notifications";
                    case MESSAGING -> "Handles unread chat message notifications and direct digests";
                    case DIGEST_PRODUCT -> "Handles periodic activity digests, newsletters, and announcements";
                };

                DynamicEmailConfig config = DynamicEmailConfig.builder()
                        .category(cat)
                        .purposeName(purpose)
                        .description(desc)
                        .fromEmail(defaultSender.getFrom())
                        .senderName(defaultSender.getName())
                        .isEnabled(true)
                        .build();

                dynamicEmailConfigRepository.save(config);
                log.info("Initialized default dynamic email configuration for category [{}] -> {}", cat, defaultSender.getFrom());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicEmailConfigResponse> getAllConfigs() {
        return dynamicEmailConfigRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DynamicEmailConfigResponse getConfigByCategory(EmailCategory category) {
        DynamicEmailConfig config = dynamicEmailConfigRepository.findByCategory(category)
                .orElseThrow(() -> new ResourceNotFoundException("Email configuration not found for category: " + category));
        return toResponse(config);
    }

    @Override
    @Transactional
    public DynamicEmailConfigResponse saveOrUpdateConfig(DynamicEmailConfigRequest request) {
        DynamicEmailConfig config = dynamicEmailConfigRepository.findByCategory(request.getCategory())
                .orElseGet(() -> DynamicEmailConfig.builder()
                        .category(request.getCategory())
                        .build());

        config.setFromEmail(request.getFromEmail().trim());
        config.setSenderName(request.getSenderName().trim());
        if (request.getPurposeName() != null && !request.getPurposeName().isBlank()) {
            config.setPurposeName(request.getPurposeName().trim());
        } else if (config.getPurposeName() == null) {
            config.setPurposeName(request.getCategory().name());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription().trim());
        }
        config.setReplyTo(request.getReplyTo() != null ? request.getReplyTo().trim() : null);
        config.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true);

        DynamicEmailConfig saved = dynamicEmailConfigRepository.save(config);

        // Invalidate or update Redis cache
        evictCache(request.getCategory());

        log.info("Dynamic email configuration updated for category [{}] -> from: [{}], name: [{}]",
                request.getCategory(), saved.getFromEmail(), saved.getSenderName());

        return toResponse(saved);
    }

    @Override
    public MailProperties.SenderIdentity resolveSender(EmailCategory category) {
        // 1. Try cache
        String cacheKey = REDIS_KEY_PREFIX + category.name();
        if (redisTemplate.isPresent()) {
            try {
                String cached = redisTemplate.get().opsForValue().get(cacheKey);
                if (cached != null && cached.contains("|")) {
                    String[] parts = cached.split("\\|", 2);
                    return new MailProperties.SenderIdentity(parts[0], parts[1]);
                }
            } catch (Exception ex) {
                log.warn("Redis read failed for {}: {}", cacheKey, ex.getMessage());
            }
        }

        // 2. Query Database
        Optional<DynamicEmailConfig> configOpt = dynamicEmailConfigRepository.findByCategory(category);
        if (configOpt.isPresent() && Boolean.TRUE.equals(configOpt.get().getIsEnabled())) {
            DynamicEmailConfig config = configOpt.get();
            MailProperties.SenderIdentity identity = new MailProperties.SenderIdentity(config.getFromEmail(), config.getSenderName());

            // Populate Cache with 1 hour TTL
            if (redisTemplate.isPresent()) {
                try {
                    redisTemplate.get().opsForValue().set(cacheKey, config.getFromEmail() + "|" + config.getSenderName(), Duration.ofHours(1));
                } catch (Exception ignored) {
                }
            }
            return identity;
        }

        // 3. Fallback to MailProperties defaults
        return mailProperties.getSenderForCategory(category);
    }

    @Override
    public void sendTestEmail(TestEmailConfigRequest request) {
        MailProperties.SenderIdentity sender = resolveSender(request.getCategory());
        String subject = "Test Configuration Email: " + request.getCategory().name();
        String htmlBody = "<div style='font-family:sans-serif;padding:20px;'>" +
                "<h2>Email Configuration Verification</h2>" +
                "<p>This is a test email sent from the dynamic Instagram administrative sender config.</p>" +
                "<ul>" +
                "<li><b>Category/Purpose:</b> " + request.getCategory().name() + "</li>" +
                "<li><b>Configured Sender:</b> " + sender.getName() + " &lt;" + sender.getFrom() + "&gt;</li>" +
                "</ul>" +
                "<p>If you received this message, the email routing for this specific job is working correctly!</p>" +
                "</div>";
        String textBody = "Test Email Verification\nCategory: " + request.getCategory().name() +
                "\nSender: " + sender.getName() + " <" + sender.getFrom() + ">" +
                "\nRouting is operational.";

        if (emailProvider.isAvailable()) {
            emailProvider.send(request.getRecipientEmail(), subject, htmlBody, textBody, sender.getFrom(), sender.getName());
            log.info("Sent test email for category [{}] to [{}] from [{}]", request.getCategory(), request.getRecipientEmail(), sender.getFrom());
        } else {
            log.info("Email provider unavailable/mock mode. Test email for category [{}] to [{}] from [{}] simulated successfully.",
                    request.getCategory(), request.getRecipientEmail(), sender.getFrom());
        }
    }

    private void evictCache(EmailCategory category) {
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().delete(REDIS_KEY_PREFIX + category.name());
            } catch (Exception ex) {
                log.warn("Failed to evict Redis cache for {}: {}", category, ex.getMessage());
            }
        }
    }

    private DynamicEmailConfigResponse toResponse(DynamicEmailConfig config) {
        return DynamicEmailConfigResponse.builder()
                .id(config.getId())
                .category(config.getCategory())
                .purposeName(config.getPurposeName())
                .description(config.getDescription())
                .fromEmail(config.getFromEmail())
                .senderName(config.getSenderName())
                .replyTo(config.getReplyTo())
                .isEnabled(config.getIsEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
