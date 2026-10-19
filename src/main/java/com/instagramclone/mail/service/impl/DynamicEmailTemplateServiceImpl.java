package com.instagramclone.mail.service.impl;

import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest;
import com.instagramclone.mail.dto.request.PreviewEmailTemplateRequest;
import com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse;
import com.instagramclone.mail.dto.response.PreviewEmailTemplateResponse;
import com.instagramclone.mail.entity.DynamicEmailTemplate;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.repository.DynamicEmailTemplateRepository;
import com.instagramclone.mail.service.interfaces.IDynamicEmailTemplateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DynamicEmailTemplateServiceImpl implements IDynamicEmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DynamicEmailTemplateServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "email:template:";

    private final DynamicEmailTemplateRepository dynamicEmailTemplateRepository;
    private final Optional<StringRedisTemplate> redisTemplate;

    @PostConstruct
    public void onStartup() {
        try {
            initDefaultTemplates();
        } catch (Exception ex) {
            log.warn("Could not initialize default dynamic email templates: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void initDefaultTemplates() {
        for (EmailType type : EmailType.values()) {
            if (!dynamicEmailTemplateRepository.existsByEmailType(type)) {
                DefaultTemplateDef def = getDefaultDefinition(type);
                if (def != null) {
                    DynamicEmailTemplate template = DynamicEmailTemplate.builder()
                            .emailType(type)
                            .templateName(def.name())
                            .description(def.description())
                            .subjectTemplate(def.subject())
                            .bodyHtmlTemplate(def.htmlBody())
                            .bodyTextTemplate(def.textBody())
                            .availablePlaceholders(def.placeholders())
                            .isActive(true)
                            .build();

                    dynamicEmailTemplateRepository.save(template);
                    log.info("Initialized default dynamic email template for [{}]", type);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicEmailTemplateResponse> getAllTemplates() {
        return dynamicEmailTemplateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DynamicEmailTemplateResponse getTemplateByType(EmailType emailType) {
        DynamicEmailTemplate template = dynamicEmailTemplateRepository.findByEmailType(emailType)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found for type: " + emailType));
        return toResponse(template);
    }

    @Override
    @Transactional
    public DynamicEmailTemplateResponse saveOrUpdateTemplate(DynamicEmailTemplateRequest request) {
        DynamicEmailTemplate template = dynamicEmailTemplateRepository.findByEmailType(request.getEmailType())
                .orElseGet(() -> DynamicEmailTemplate.builder()
                        .emailType(request.getEmailType())
                        .build());

        template.setSubjectTemplate(request.getSubjectTemplate().trim());
        template.setBodyHtmlTemplate(request.getBodyHtmlTemplate().trim());
        if (request.getBodyTextTemplate() != null) {
            template.setBodyTextTemplate(request.getBodyTextTemplate().trim());
        }
        if (request.getTemplateName() != null && !request.getTemplateName().isBlank()) {
            template.setTemplateName(request.getTemplateName().trim());
        } else if (template.getTemplateName() == null) {
            template.setTemplateName(request.getEmailType().name());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription().trim());
        }
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        DynamicEmailTemplate saved = dynamicEmailTemplateRepository.save(template);
        evictCache(request.getEmailType());

        log.info("Updated dynamic email template for [{}] (Active: {})", request.getEmailType(), saved.getIsActive());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DynamicEmailTemplate> findActiveTemplate(EmailType emailType) {
        return dynamicEmailTemplateRepository.findByEmailType(emailType)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()));
    }

    @Override
    public PreviewEmailTemplateResponse previewTemplate(PreviewEmailTemplateRequest request) {
        EmailType type = request.getEmailType();
        String subjectTemplate = request.getSubjectTemplate();
        String htmlTemplate = request.getBodyHtmlTemplate();
        String textTemplate = request.getBodyTextTemplate();

        // If not provided in request, load current active or default
        if (subjectTemplate == null || htmlTemplate == null) {
            Optional<DynamicEmailTemplate> existingOpt = dynamicEmailTemplateRepository.findByEmailType(type);
            if (existingOpt.isPresent()) {
                DynamicEmailTemplate existing = existingOpt.get();
                if (subjectTemplate == null) subjectTemplate = existing.getSubjectTemplate();
                if (htmlTemplate == null) htmlTemplate = existing.getBodyHtmlTemplate();
                if (textTemplate == null) textTemplate = existing.getBodyTextTemplate();
            } else {
                DefaultTemplateDef def = getDefaultDefinition(type);
                if (def != null) {
                    if (subjectTemplate == null) subjectTemplate = def.subject();
                    if (htmlTemplate == null) htmlTemplate = def.htmlBody();
                    if (textTemplate == null) textTemplate = def.textBody();
                }
            }
        }

        Map<String, Object> vars = (request.getSampleVariables() != null && !request.getSampleVariables().isEmpty())
                ? request.getSampleVariables()
                : getDefaultSampleVariables(type);

        String renderedSubject = renderPlaceholders(subjectTemplate != null ? subjectTemplate : type.name(), vars, false);
        String renderedHtml = renderPlaceholders(htmlTemplate != null ? htmlTemplate : "", vars, true);
        String renderedText = renderPlaceholders(textTemplate != null ? textTemplate : "", vars, false);

        return PreviewEmailTemplateResponse.builder()
                .subject(renderedSubject)
                .renderedHtml(renderedHtml)
                .renderedText(renderedText)
                .build();
    }

    @Override
    @Transactional
    public void resetTemplateToDefault(EmailType emailType) {
        DefaultTemplateDef def = getDefaultDefinition(emailType);
        if (def != null) {
            DynamicEmailTemplate template = dynamicEmailTemplateRepository.findByEmailType(emailType)
                    .orElseGet(() -> DynamicEmailTemplate.builder().emailType(emailType).build());

            template.setTemplateName(def.name());
            template.setDescription(def.description());
            template.setSubjectTemplate(def.subject());
            template.setBodyHtmlTemplate(def.htmlBody());
            template.setBodyTextTemplate(def.textBody());
            template.setAvailablePlaceholders(def.placeholders());
            template.setIsActive(true);

            dynamicEmailTemplateRepository.save(template);
            evictCache(emailType);
            log.info("Reset dynamic email template for [{}] to system default", emailType);
        }
    }

    private void evictCache(EmailType emailType) {
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().delete(REDIS_KEY_PREFIX + emailType.name());
            } catch (Exception ex) {
                log.warn("Failed to evict Redis cache for template {}: {}", emailType, ex.getMessage());
            }
        }
    }

    private String renderPlaceholders(String template, Map<String, Object> variables, boolean isHtml) {
        if (template == null || template.isEmpty()) return "";
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String rawVal = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            String safeVal = isHtml ? HtmlUtils.htmlEscape(rawVal) : rawVal;
            result = result.replace(placeholder, safeVal);
        }
        return result;
    }

    private DynamicEmailTemplateResponse toResponse(DynamicEmailTemplate t) {
        return DynamicEmailTemplateResponse.builder()
                .id(t.getId())
                .emailType(t.getEmailType())
                .category(t.getEmailType().getCategory())
                .templateName(t.getTemplateName())
                .description(t.getDescription())
                .subjectTemplate(t.getSubjectTemplate())
                .bodyHtmlTemplate(t.getBodyHtmlTemplate())
                .bodyTextTemplate(t.getBodyTextTemplate())
                .availablePlaceholders(t.getAvailablePlaceholders())
                .isActive(t.getIsActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private Map<String, Object> getDefaultSampleVariables(EmailType type) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "alex_creator");
        map.put("exploreUrl", "http://localhost:5173/explore");
        map.put("verifyUrl", "http://localhost:5173/verify-email?token=sample_token_xyz");
        map.put("validityHours", 24);
        map.put("otpCode", "582914");
        map.put("validityMinutes", 10);
        map.put("resetUrl", "http://localhost:5173/reset-password?token=sample_reset_xyz");
        map.put("changedAt", "October 17, 2026, 08:30 UTC");
        map.put("loginTime", "October 17, 2026, 08:30 UTC");
        map.put("device", "Chrome on Windows 11");
        map.put("ipAddress", "192.168.1.100");
        map.put("location", "Mumbai, India");
        map.put("actorUsername", "sam_traveler");
        map.put("actorProfileUrl", "http://localhost:5173/sam_traveler");
        map.put("postUrl", "http://localhost:5173/p/c9b0e21a");
        map.put("commentContent", "Stunning shot! Where was this taken?");
        map.put("unreadCount", 4);
        map.put("messagesUrl", "http://localhost:5173/direct");
        return map;
    }

    private record DefaultTemplateDef(String name, String description, String subject, String htmlBody, String textBody, String placeholders) {}

    private DefaultTemplateDef getDefaultDefinition(EmailType type) {
        return switch (type) {
            case WELCOME -> new DefaultTemplateDef(
                    "Welcome Email",
                    "Sent to newly registered and verified users",
                    "Welcome to Instagram Clone, {{username}}!",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>Welcome to Instagram Clone! Connect with friends, share your everyday moments, and discover creative reels and photos from around the world.</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{exploreUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">Explore Instagram</a></div><p style=\"color: #737373; font-size: 13px;\">Upload your first photo, update your profile picture, and start following interesting creators today.</p>",
                    "Hi {{username}},\n\nWelcome to Instagram Clone!\nConnect with friends and discover reels and photos.\nExplore: {{exploreUrl}}",
                    "{{username}}, {{exploreUrl}}"
            );
            case EMAIL_VERIFICATION -> new DefaultTemplateDef(
                    "Email Verification",
                    "Sent during account registration to verify email ownership",
                    "Verify your Instagram clone account",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>Thank you for signing up! To complete your registration and secure your account, please verify your email address by clicking below:</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{verifyUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">Verify Account</a></div><p style=\"font-size: 13px; color: #737373;\">This link expires in {{validityHours}} hours.</p>",
                    "Hi {{username}},\nPlease verify your Instagram clone account: {{verifyUrl}}\nExpires in {{validityHours}} hours.",
                    "{{username}}, {{verifyUrl}}, {{validityHours}}"
            );
            case EMAIL_OTP -> new DefaultTemplateDef(
                    "OTP Verification Code",
                    "Sent when an OTP verification code is requested",
                    "{{otpCode}} is your Instagram clone verification code",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>Here is your six-digit verification code:</p><div style=\"text-align: center; margin: 28px 0;\"><div style=\"display: inline-block; background-color: #f8f9fa; border: 1px solid #dbdbdb; border-radius: 8px; padding: 14px 28px; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #262626;\">{{otpCode}}</div></div><p style=\"font-size: 13px; color: #737373;\">This code is valid for {{validityMinutes}} minutes.</p>",
                    "Hi {{username}},\nYour verification code is: {{otpCode}}\nValid for {{validityMinutes}} minutes.",
                    "{{username}}, {{otpCode}}, {{validityMinutes}}"
            );
            case PASSWORD_RESET -> new DefaultTemplateDef(
                    "Password Reset",
                    "Sent when a user requests to reset their password",
                    "Reset your Instagram clone password",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>We received a request to reset your password. You can reset your password using the link below:</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{resetUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">Reset Password</a></div><p style=\"font-size: 13px; color: #737373;\">This link expires in {{validityMinutes}} minutes.</p>",
                    "Hi {{username}},\nReset your password here: {{resetUrl}}\nExpires in {{validityMinutes}} minutes.",
                    "{{username}}, {{resetUrl}}, {{validityMinutes}}"
            );
            case PASSWORD_CHANGED -> new DefaultTemplateDef(
                    "Password Changed Alert",
                    "Sent when an account password has been updated",
                    "Security Alert: Your password was changed",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>This is a confirmation that your account password was changed on {{changedAt}} from device {{device}} (IP: {{ipAddress}}).</p><p>If you did not make this change, please secure your account immediately.</p>",
                    "Hi {{username}},\nSecurity Alert: Your password was changed on {{changedAt}} from {{device}}.",
                    "{{username}}, {{changedAt}}, {{device}}, {{ipAddress}}"
            );
            case NEW_LOGIN -> new DefaultTemplateDef(
                    "New Login Alert",
                    "Sent when an account is accessed from a new device/session",
                    "Security Alert: New login to your account",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>We noticed a new login to your Instagram clone account on {{loginTime}} from {{device}} (IP: {{ipAddress}}, {{location}}).</p>",
                    "Hi {{username}},\nNew login detected on {{loginTime}} from {{device}}.",
                    "{{username}}, {{loginTime}}, {{device}}, {{ipAddress}}, {{location}}"
            );
            case NEW_FOLLOWER -> new DefaultTemplateDef(
                    "New Follower Notification",
                    "Sent when someone starts following the user",
                    "{{actorUsername}} started following you on Instagram",
                    "<p>Hi <strong>{{username}}</strong>,</p><p><strong>{{actorUsername}}</strong> started following you on Instagram Clone.</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{actorProfileUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">View Profile</a></div>",
                    "Hi {{username}},\n{{actorUsername}} started following you on Instagram.\nView Profile: {{actorProfileUrl}}",
                    "{{username}}, {{actorUsername}}, {{actorProfileUrl}}"
            );
            case POST_LIKED -> new DefaultTemplateDef(
                    "Post Liked Notification",
                    "Sent when someone likes the user's post",
                    "{{actorUsername}} liked your post",
                    "<p>Hi <strong>{{username}}</strong>,</p><p><strong>{{actorUsername}}</strong> liked your post.</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{postUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">View Post</a></div>",
                    "Hi {{username}},\n{{actorUsername}} liked your post: {{postUrl}}",
                    "{{username}}, {{actorUsername}}, {{postUrl}}"
            );
            case POST_COMMENTED -> new DefaultTemplateDef(
                    "Post Commented Notification",
                    "Sent when someone comments on the user's post",
                    "{{actorUsername}} commented on your post",
                    "<p>Hi <strong>{{username}}</strong>,</p><p><strong>{{actorUsername}}</strong> commented: <em>\"{{commentContent}}\"</em></p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{postUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">Reply to Comment</a></div>",
                    "Hi {{username}},\n{{actorUsername}} commented: \"{{commentContent}}\"\nView: {{postUrl}}",
                    "{{username}}, {{actorUsername}}, {{commentContent}}, {{postUrl}}"
            );
            case UNREAD_MESSAGE_DIGEST -> new DefaultTemplateDef(
                    "Unread Direct Messages Reminder",
                    "Sent when user has unread direct messages",
                    "You have unread direct messages on Instagram",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>You have <strong>{{unreadCount}}</strong> unread direct message(s) waiting for you.</p><div style=\"text-align: center; margin: 28px 0;\"><a href=\"{{messagesUrl}}\" style=\"display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;\">View Messages</a></div>",
                    "Hi {{username}},\nYou have {{unreadCount}} unread direct message(s).\nView Messages: {{messagesUrl}}",
                    "{{username}}, {{unreadCount}}, {{messagesUrl}}"
            );
            default -> new DefaultTemplateDef(
                    type.name(),
                    "Notification template for " + type.name(),
                    type.getDefaultSubject() != null ? type.getDefaultSubject() : "Instagram Notification",
                    "<p>Hi <strong>{{username}}</strong>,</p><p>You have a new notification on Instagram Clone.</p>",
                    "Hi {{username}},\nYou have a new notification on Instagram Clone.",
                    "{{username}}"
            );
        };
    }
}
