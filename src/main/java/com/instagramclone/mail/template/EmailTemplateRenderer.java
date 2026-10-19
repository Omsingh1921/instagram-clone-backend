package com.instagramclone.mail.template;

import com.instagramclone.mail.enums.EmailType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@Component
@lombok.RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final com.instagramclone.mail.repository.DynamicEmailTemplateRepository dynamicEmailTemplateRepository;

    public RenderedEmail render(EmailType emailType, Map<String, Object> variables, String unsubscribeUrl) {
        if (variables == null) {
            variables = Map.of();
        }

        var customTemplateOpt = dynamicEmailTemplateRepository.findByEmailType(emailType)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()));

        if (customTemplateOpt.isPresent()) {
            var custom = customTemplateOpt.get();
            String renderedSubject = renderPlaceholders(custom.getSubjectTemplate(), variables, false);
            String renderedBody = renderPlaceholders(custom.getBodyHtmlTemplate(), variables, true);
            String renderedText = renderPlaceholders(custom.getBodyTextTemplate(), variables, false);
            boolean isSecurity = emailType.getCategory() == com.instagramclone.mail.enums.EmailCategory.SECURITY;
            String fullHtml = wrapInCard(renderedSubject, renderedBody, isSecurity, unsubscribeUrl);
            return new RenderedEmail(renderedSubject, fullHtml, renderedText);
        }

        return switch (emailType) {
            case WELCOME -> renderWelcome(variables, unsubscribeUrl);
            case EMAIL_VERIFICATION -> renderVerification(variables);
            case EMAIL_OTP -> renderOtp(variables);
            case PASSWORD_RESET -> renderPasswordReset(variables);
            case PASSWORD_CHANGED -> renderPasswordChanged(variables);
            case SECURITY_SETTINGS_CHANGED -> renderSecuritySettingsChanged(variables);
            case NEW_LOGIN -> renderNewLogin(variables);
            case NEW_FOLLOWER -> renderNewFollower(variables, unsubscribeUrl);
            case FOLLOW_REQUEST -> renderFollowRequest(variables, unsubscribeUrl);
            case POST_LIKED -> renderPostLiked(variables, unsubscribeUrl);
            case POST_COMMENTED -> renderPostCommented(variables, unsubscribeUrl);
            case UNREAD_MESSAGE_DIGEST -> renderUnreadMessageDigest(variables, unsubscribeUrl);
            case PRODUCT_ANNOUNCEMENT -> renderProductAnnouncement(variables, unsubscribeUrl);
            default -> renderGeneric(emailType, variables, unsubscribeUrl);
        };
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

    private String escape(Object value) {
        if (value == null) return "";
        return HtmlUtils.htmlEscape(String.valueOf(value));
    }

    private RenderedEmail renderWelcome(Map<String, Object> v, String unsubscribeUrl) {
        String username = escape(v.getOrDefault("username", "friend"));
        String exploreUrl = escape(v.getOrDefault("exploreUrl", "http://localhost:5173/explore"));

        String subject = "Welcome to Instagram Clone, " + username + "!";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <p>Welcome to Instagram Clone! Connect with friends, share your everyday moments, and discover creative reels and photos from around the world.</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s" style="%s">Explore Instagram</a>
                </div>
                <p style="color: #737373; font-size: 13px;">Upload your first photo, update your profile picture, and start following interesting creators today.</p>
                """.formatted(username, exploreUrl, primaryButtonStyle());

        String plainText = """
                Hi %s,

                Welcome to Instagram Clone!
                Connect with friends and discover reels and photos from around the world.
                Start exploring: %s
                """.formatted(username, exploreUrl);

        return new RenderedEmail(subject, wrapInCard(subject, content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderVerification(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));
        String verifyUrl = escape(v.getOrDefault("verifyUrl", "#"));
        long hours = Long.parseLong(String.valueOf(v.getOrDefault("validityHours", 24)));

        String subject = "Verify your Instagram clone account";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <p>Thank you for signing up! To complete your registration and secure your account, please verify your email address by clicking below:</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s" style="%s">Verify Account</a>
                </div>
                <p style="font-size: 13px; color: #737373;">This link expires in %d hours. If the button doesn't work, open this URL: <br><span style="word-break: break-all; color: #0095f6;">%s</span></p>
                <p style="font-size: 13px; color: #8e8e8e; margin-top: 20px;">If you didn't create an account, you can safely ignore this email.</p>
                """.formatted(username, verifyUrl, primaryButtonStyle(), hours, verifyUrl);

        String plainText = """
                Hi %s,
                Please verify your Instagram clone account by opening: %s
                This link expires in %d hours.
                """.formatted(username, verifyUrl, hours);

        return new RenderedEmail(subject, wrapInCard(subject, content, true, null), plainText);
    }

    private RenderedEmail renderOtp(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));
        String otpCode = escape(v.getOrDefault("otpCode", "000000"));
        long minutes = Long.parseLong(String.valueOf(v.getOrDefault("validityMinutes", 5)));

        String subject = otpCode + " is your Instagram clone verification code";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <p>Someone recently tried to register or verify an account with your email address. Here is your six-digit verification code:</p>
                <div style="text-align: center; margin: 28px 0;">
                    <div style="display: inline-block; background-color: #f8f9fa; border: 1px solid #dbdbdb; border-radius: 8px; padding: 14px 28px; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #262626; font-family: -apple-system, BlinkMacSystemFont, monospace;">
                        %s
                    </div>
                </div>
                <p style="font-size: 13px; color: #737373; text-align: center;">This code expires in %d minutes and can only be used once.</p>
                <div style="%s">
                    <strong>Security Notice:</strong> Never share this code with anyone. Instagram clone representatives will never ask for your verification code.
                </div>
                <p style="font-size: 13px; color: #8e8e8e; margin-top: 20px;">If you did not request this verification code, you can safely ignore this email.</p>
                """.formatted(username, otpCode, minutes, alertBoxStyle());

        String plainText = """
                Hi %s,

                Your Instagram clone verification code is: %s

                This code expires in %d minutes and can only be used once.

                Security Notice: Never share this code with anyone. Instagram clone representatives will never ask for your verification code.
                If you did not request this code, you can safely ignore this email.
                """.formatted(username, otpCode, minutes);

        return new RenderedEmail(subject, wrapInCard(subject, content, true, null), plainText);
    }

    private RenderedEmail renderPasswordReset(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));
        String resetUrl = escape(v.getOrDefault("resetUrl", "#"));
        long minutes = Long.parseLong(String.valueOf(v.getOrDefault("validityMinutes", 30)));
        String subject = "Reset your Instagram clone password";

        Object accountsObj = v.get("accounts");
        if (accountsObj instanceof java.util.List<?> accountsList && !accountsList.isEmpty()) {
            StringBuilder itemsHtml = new StringBuilder();
            StringBuilder itemsText = new StringBuilder();
            for (Object item : accountsList) {
                if (item instanceof Map<?, ?> acc) {
                    Object userVal = acc.get("username");
                    Object urlVal = acc.get("resetUrl");
                    String accUsername = escape(userVal != null ? String.valueOf(userVal) : "user");
                    String accResetUrl = escape(urlVal != null ? String.valueOf(urlVal) : "#");
                    itemsHtml.append("""
                            <div style="margin: 16px 0; padding: 12px; background: #fafafa; border: 1px solid #dbdbdb; border-radius: 6px;">
                                <div style="font-weight: 600; font-size: 14px; margin-bottom: 8px;">@%s</div>
                                <a href="%s" style="%s">Reset password for @%s</a>
                            </div>
                            """.formatted(accUsername, accResetUrl, alertButtonStyle(), accUsername));
                    itemsText.append("- @%s: %s\n".formatted(accUsername, accResetUrl));
                }
            }
            String content = """
                    <p>Hi,</p>
                    <p>We received a request to reset the password for your Instagram clone accounts associated with this email address:</p>
                    %s
                    <p style="font-size: 13px; color: #737373;">Each link expires in %d minutes and can only be used once. If you did not make this request, you can safely disregard this email—your accounts remain safe.</p>
                    """.formatted(itemsHtml.toString(), minutes);

            String plainText = """
                    Hi,
                    We received a password reset request for your Instagram clone accounts:
                    %s
                    Each link expires in %d minutes.
                    If you did not request this, please disregard this email.
                    """.formatted(itemsText.toString(), minutes);

            return new RenderedEmail(subject, wrapInCard(subject, content, true, null), plainText);
        }

        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <p>We received a request to reset your Instagram clone account password. Click the button below to set a new password:</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s" style="%s">Reset Password</a>
                </div>
                <p style="font-size: 13px; color: #737373;">This single-use link expires in %d minutes. If you did not make this request, you can safely disregard this email—your account remains safe.</p>
                <p style="word-break: break-all; color: #0095f6; font-size: 13px;">%s</p>
                """.formatted(username, resetUrl, alertButtonStyle(), minutes, resetUrl);

        String plainText = """
                Hi %s,
                We received a password reset request for your Instagram clone account.
                Reset your password: %s
                This link expires in %d minutes.
                """.formatted(username, resetUrl, minutes);

        return new RenderedEmail(subject, wrapInCard(subject, content, true, null), plainText);
    }

    private RenderedEmail renderPasswordChanged(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));

        String subject = "Security Alert: Your Instagram clone password was changed";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <div style="%s">
                    <strong>Security Notice:</strong> The password for your Instagram clone account was just changed.
                </div>
                <p>All active login sessions, mobile tokens, and remember-me credentials have been revoked across all devices for your protection.</p>
                <p style="color: #737373; font-size: 13px;">If you made this change, no further action is required. If you did NOT change your password, please reset your password immediately or contact support.</p>
                """.formatted(username, alertBoxStyle());

        String plainText = """
                SECURITY ALERT: The password for your Instagram clone account was changed.
                All active sessions and refresh tokens have been revoked.
                If you did not perform this change, reset your password immediately.
                """;

        return new RenderedEmail(subject, wrapInCard("Security Alert", content, true, null), plainText);
    }

    private RenderedEmail renderSecuritySettingsChanged(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));
        String changeDetails = escape(v.getOrDefault("changeDetails", "Privacy or security settings were updated."));

        String subject = "Security Alert: Account settings updated";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <div style="%s">
                    <strong>Settings Update:</strong> %s
                </div>
                <p>If you made this change, you're all set. If you did not authorize this change, please log in to review your account security settings.</p>
                """.formatted(username, noticeBoxStyle(), changeDetails);

        String plainText = """
                Security Notice: Account settings were updated for %s.
                Details: %s
                """.formatted(username, changeDetails);

        return new RenderedEmail(subject, wrapInCard("Security Notice", content, true, null), plainText);
    }

    private RenderedEmail renderNewLogin(Map<String, Object> v) {
        String username = escape(v.getOrDefault("username", "user"));
        String ipAddress = escape(v.getOrDefault("ipAddress", "Unknown IP"));
        String loginTime = escape(v.getOrDefault("loginTime", "Recently"));

        String subject = "Security Alert: New login to your Instagram clone account";
        String content = """
                <p>Hi <strong>%s</strong>,</p>
                <p>A new login was recorded for your account at <strong>%s</strong> (IP: %s).</p>
                <p style="color: #737373; font-size: 13px;">If this was you, you can safely disregard this message. If you suspect unauthorized access, reset your password immediately to terminate all active sessions.</p>
                """.formatted(username, loginTime, ipAddress);

        String plainText = """
                New login detected for %s at %s from IP %s.
                If this wasn't you, reset your password immediately.
                """.formatted(username, loginTime, ipAddress);

        return new RenderedEmail(subject, wrapInCard("Security Notice", content, true, null), plainText);
    }

    private RenderedEmail renderNewFollower(Map<String, Object> v, String unsubscribeUrl) {
        String actorUsername = escape(v.getOrDefault("actorUsername", "Someone"));
        String profileUrl = escape(v.getOrDefault("profileUrl", "http://localhost:5173/" + actorUsername));

        String subject = actorUsername + " started following you on Instagram";
        String content = """
                <p><strong>%s</strong> started following you.</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">View %s's Profile</a>
                </div>
                """.formatted(actorUsername, profileUrl, primaryButtonStyle(), actorUsername);

        String plainText = "%s started following you on Instagram! View profile: %s".formatted(actorUsername, profileUrl);
        return new RenderedEmail(subject, wrapInCard("New Follower", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderFollowRequest(Map<String, Object> v, String unsubscribeUrl) {
        String actorUsername = escape(v.getOrDefault("actorUsername", "Someone"));
        String requestsUrl = escape(v.getOrDefault("requestsUrl", "http://localhost:5173/activity"));

        String subject = actorUsername + " requested to follow you";
        String content = """
                <p><strong>%s</strong> has requested to follow your private account.</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">Review Request</a>
                </div>
                """.formatted(actorUsername, requestsUrl, primaryButtonStyle());

        String plainText = "%s requested to follow you. Review: %s".formatted(actorUsername, requestsUrl);
        return new RenderedEmail(subject, wrapInCard("Follow Request", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderPostLiked(Map<String, Object> v, String unsubscribeUrl) {
        String actorUsername = escape(v.getOrDefault("actorUsername", "Someone"));
        String postUrl = escape(v.getOrDefault("postUrl", "http://localhost:5173"));

        String subject = actorUsername + " liked your post";
        String content = """
                <p><strong>%s</strong> liked your post.</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">View Post</a>
                </div>
                """.formatted(actorUsername, postUrl, primaryButtonStyle());

        String plainText = "%s liked your post on Instagram clone. View: %s".formatted(actorUsername, postUrl);
        return new RenderedEmail(subject, wrapInCard("Post Liked", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderPostCommented(Map<String, Object> v, String unsubscribeUrl) {
        String actorUsername = escape(v.getOrDefault("actorUsername", "Someone"));
        String commentText = escape(v.getOrDefault("commentText", "Nice post!"));
        String postUrl = escape(v.getOrDefault("postUrl", "http://localhost:5173"));

        String subject = actorUsername + " commented on your post";
        String content = """
                <p><strong>%s</strong> commented: <em>"%s"</em></p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">Reply to Comment</a>
                </div>
                """.formatted(actorUsername, commentText, postUrl, primaryButtonStyle());

        String plainText = "%s commented on your post: \"%s\". View: %s".formatted(actorUsername, commentText, postUrl);
        return new RenderedEmail(subject, wrapInCard("New Comment", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderUnreadMessageDigest(Map<String, Object> v, String unsubscribeUrl) {
        String senderName = escape(v.getOrDefault("senderName", "A friend"));
        long unreadCount = Long.parseLong(String.valueOf(v.getOrDefault("unreadCount", 1)));
        String messagesUrl = escape(v.getOrDefault("messagesUrl", "http://localhost:5173/direct"));

        String subject = "You have " + unreadCount + " unread message" + (unreadCount > 1 ? "s" : "") + " on Instagram";
        String content = """
                <p>You have unread direct messages from <strong>%s</strong> and other friends.</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">Open Direct Messages</a>
                </div>
                """.formatted(senderName, messagesUrl, primaryButtonStyle());

        String plainText = "You have %d unread messages from %s. Open messages: %s".formatted(unreadCount, senderName, messagesUrl);
        return new RenderedEmail(subject, wrapInCard("Direct Messages", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderProductAnnouncement(Map<String, Object> v, String unsubscribeUrl) {
        String title = escape(v.getOrDefault("title", "New Features on Instagram Clone"));
        String announcement = escape(v.getOrDefault("announcement", "We've added new enhancements to your experience!"));
        String learnMoreUrl = escape(v.getOrDefault("learnMoreUrl", "http://localhost:5173"));

        String subject = "New on Instagram Clone: " + title;
        String content = """
                <h2>%s</h2>
                <p>%s</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="%s">Learn More</a>
                </div>
                """.formatted(title, announcement, learnMoreUrl, primaryButtonStyle());

        String plainText = "%s\n%s\nLearn more: %s".formatted(title, announcement, learnMoreUrl);
        return new RenderedEmail(subject, wrapInCard("Announcement", content, false, unsubscribeUrl), plainText);
    }

    private RenderedEmail renderGeneric(EmailType type, Map<String, Object> v, String unsubscribeUrl) {
        String title = escape(type.name().replace('_', ' '));
        String message = escape(v.getOrDefault("message", "You have a new notification from Instagram Clone."));
        String subject = "Instagram Clone: " + title;
        String content = "<p>" + message + "</p>";
        String plainText = subject + "\n" + message;
        return new RenderedEmail(subject, wrapInCard(title, content, type.isMandatory(), unsubscribeUrl), plainText);
    }

    private String wrapInCard(String headerTitle, String bodyContent, boolean isSecurity, String unsubscribeUrl) {
        String headerGradient = isSecurity
                ? "background: #262626;"
                : "background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);";

        String footerUnsubscribe = "";
        if (unsubscribeUrl != null && !unsubscribeUrl.isBlank() && !isSecurity) {
            footerUnsubscribe = """
                    <div style="margin-top: 10px; font-size: 11px;">
                        <a href="%s" style="color: #8e8e8e; text-decoration: underline;">Unsubscribe from this category</a> &bull;
                        <a href="http://localhost:5173/settings/notifications" style="color: #8e8e8e; text-decoration: underline;">Email Preferences</a>
                    </div>
                    """.formatted(unsubscribeUrl);
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: #fafafa; margin: 0; padding: 0; }
                    .wrapper { max-width: 520px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; border: 1px solid #dbdbdb; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
                    .header { %s padding: 22px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }
                    .body { padding: 30px 26px; color: #262626; font-size: 15px; line-height: 1.6; }
                    .footer { padding: 18px 24px; background-color: #fafafa; border-top: 1px solid #ebebeb; font-size: 12px; color: #8e8e8e; text-align: center; }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <div class="header">
                        <h1>Instagram</h1>
                    </div>
                    <div class="body">
                        %s
                    </div>
                    <div class="footer">
                        &copy; 2026 Instagram Clone. All rights reserved.
                        %s
                    </div>
                </div>
            </body>
            </html>
            """.formatted(headerTitle, headerGradient, bodyContent, footerUnsubscribe);
    }

    private String primaryButtonStyle() {
        return "display: inline-block; background-color: #0095f6; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;";
    }

    private String alertButtonStyle() {
        return "display: inline-block; background-color: #ed4956; color: #ffffff !important; padding: 11px 26px; font-weight: 600; text-decoration: none; border-radius: 8px; font-size: 14px;";
    }

    private String alertBoxStyle() {
        return "background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 14px 18px; color: #991b1b; font-size: 14px; margin: 16px 0;";
    }

    private String noticeBoxStyle() {
        return "background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 14px 18px; color: #1e40af; font-size: 14px; margin: 16px 0;";
    }
}
