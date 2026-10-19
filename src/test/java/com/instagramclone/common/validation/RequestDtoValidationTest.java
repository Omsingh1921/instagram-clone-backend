package com.instagramclone.common.validation;

import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.dto.request.UpdateUserStatusRequest;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.chat.dto.TypingEvent;
import com.instagramclone.chat.dto.request.CreateConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.mail.dto.request.UnsubscribeRequest;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.dto.request.UpdatePostRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.reel.dto.request.CreateReelCommentRequest;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.story.dto.request.CreateStoryRequest;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import com.instagramclone.user.enums.UserStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- Authentication & User Requests ---

    @Test
    @DisplayName("LoginRequest - Should validate required fields and size bounds")
    void loginRequest_Validation() {
        LoginRequest valid = LoginRequest.builder()
                .usernameOrEmail("user@example.com")
                .password("Secr3tP@ss")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        LoginRequest blank = LoginRequest.builder()
                .usernameOrEmail("   ")
                .password("")
                .build();
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(blank);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("usernameOrEmail"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        LoginRequest oversized = LoginRequest.builder()
                .usernameOrEmail("a".repeat(151))
                .password("p".repeat(101))
                .build();
        violations = validator.validate(oversized);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("usernameOrEmail"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("SignupRequestOtpRequest - Should validate email format and presence")
    void signupRequestOtpRequest_Validation() {
        SignupRequestOtpRequest valid = SignupRequestOtpRequest.builder()
                .email("test.user@instagram.com")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        SignupRequestOtpRequest blank = SignupRequestOtpRequest.builder()
                .email("  ")
                .build();
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        SignupRequestOtpRequest invalidEmail = SignupRequestOtpRequest.builder()
                .email("not-an-email")
                .build();
        assertThat(validator.validate(invalidEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "000000", "012345", "999999"})
    @DisplayName("SignupVerifyOtpRequest - Valid 6-digit codes including leading zeroes")
    void signupVerifyOtpRequest_ValidCodes(String code) {
        SignupVerifyOtpRequest req = SignupVerifyOtpRequest.builder()
                .email("user@domain.com")
                .code(code)
                .build();
        assertThat(validator.validate(req)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12a456", "abcdef", "      "})
    @DisplayName("SignupVerifyOtpRequest - Invalid codes fail validation")
    void signupVerifyOtpRequest_InvalidCodes(String code) {
        SignupVerifyOtpRequest req = SignupVerifyOtpRequest.builder()
                .email("user@domain.com")
                .code(code)
                .build();
        Set<ConstraintViolation<SignupVerifyOtpRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("code"));
    }

    @Test
    @DisplayName("VerifyOtpRequest - Validates 6-digit numeric OTP constraint")
    void verifyOtpRequest_Validation() {
        VerifyOtpRequest valid = VerifyOtpRequest.builder()
                .email("user@domain.com")
                .code("045678")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        VerifyOtpRequest invalid = VerifyOtpRequest.builder()
                .email("user@domain.com")
                .code("12")
                .build();
        assertThat(validator.validate(invalid)).anyMatch(v -> v.getPropertyPath().toString().equals("code"));
    }

    @Test
    @DisplayName("ForgotPasswordRequest - Validates non-blank identifier")
    void forgotPasswordRequest_Validation() {
        ForgotPasswordRequest valid = ForgotPasswordRequest.builder()
                .email("user@example.com")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        ForgotPasswordRequest blank = ForgotPasswordRequest.builder()
                .email("   ")
                .build();
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("ResetPasswordRequest - Validates token and password length bounds")
    void resetPasswordRequest_Validation() {
        ResetPasswordRequest valid = ResetPasswordRequest.builder()
                .token("a".repeat(64))
                .newPassword("NewPassw0rd!")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        ResetPasswordRequest shortPw = ResetPasswordRequest.builder()
                .token("valid_token")
                .newPassword("short")
                .build();
        assertThat(validator.validate(shortPw)).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));

        ResetPasswordRequest blankToken = ResetPasswordRequest.builder()
                .token("")
                .newPassword("ValidPassword123!")
                .build();
        assertThat(validator.validate(blankToken)).anyMatch(v -> v.getPropertyPath().toString().equals("token"));
    }

    @Test
    @DisplayName("ChangePasswordRequest - Validates required fields and length bounds")
    void changePasswordRequest_Validation() {
        ChangePasswordRequest valid = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        ChangePasswordRequest blank = ChangePasswordRequest.builder()
                .currentPassword("")
                .newPassword("short")
                .confirmPassword("")
                .build();
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(blank);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confirmPassword"));
    }

    @Test
    @DisplayName("InitiateEmailChangeRequest - Validates current password and new email format")
    void initiateEmailChangeRequest_Validation() {
        InitiateEmailChangeRequest valid = InitiateEmailChangeRequest.builder()
                .currentPassword("Secret123!")
                .newEmail("new.email@example.com")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        InitiateEmailChangeRequest invalid = InitiateEmailChangeRequest.builder()
                .currentPassword("")
                .newEmail("not_an_email")
                .build();
        Set<ConstraintViolation<InitiateEmailChangeRequest>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newEmail"));
    }

    @Test
    @DisplayName("RefreshTokenRequest - Validates token presence and bounds")
    void refreshTokenRequest_Validation() {
        RefreshTokenRequest valid = RefreshTokenRequest.builder()
                .refreshToken("some-opaque-refresh-token")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        RefreshTokenRequest blank = RefreshTokenRequest.builder()
                .refreshToken("   ")
                .build();
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));

        RefreshTokenRequest oversized = RefreshTokenRequest.builder()
                .refreshToken("t".repeat(501))
                .build();
        assertThat(validator.validate(oversized)).anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
    }

    // --- Post, Media, Story & Reel Requests ---

    @Test
    @DisplayName("CreatePostRequest - Validates nested media constraints and limits")
    void createPostRequest_Validation() {
        PostMediaRequest media = PostMediaRequest.builder()
                .mediaUrl("https://cloudinary.com/img1.jpg")
                .mediaType(MediaType.IMAGE)
                .displayOrder(0)
                .width(1080)
                .height(1080)
                .build();

        CreatePostRequest valid = CreatePostRequest.builder()
                .caption("Awesome post caption")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(media))
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        CreatePostRequest emptyMedia = CreatePostRequest.builder()
                .caption("Missing media")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of())
                .build();
        assertThat(validator.validate(emptyMedia)).anyMatch(v -> v.getPropertyPath().toString().equals("media"));

        CreatePostRequest nullVisibility = CreatePostRequest.builder()
                .visibility(null)
                .media(List.of(media))
                .build();
        assertThat(validator.validate(nullVisibility)).anyMatch(v -> v.getPropertyPath().toString().equals("visibility"));

        PostMediaRequest invalidMedia = PostMediaRequest.builder()
                .mediaUrl("")
                .displayOrder(-1)
                .width(0)
                .height(0)
                .build();
        CreatePostRequest invalidNested = CreatePostRequest.builder()
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(invalidMedia))
                .build();
        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(invalidNested);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("media[0].mediaUrl"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("media[0].displayOrder"));
    }

    @Test
    @DisplayName("UpdatePostRequest - Caption bounds")
    void updatePostRequest_Validation() {
        UpdatePostRequest valid = UpdatePostRequest.builder()
                .caption("Updated caption")
                .visibility(PostVisibility.PRIVATE)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UpdatePostRequest tooLong = UpdatePostRequest.builder()
                .caption("c".repeat(2201))
                .build();
        assertThat(validator.validate(tooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("caption"));
    }

    @Test
    @DisplayName("CreateReelRequest - Video URL presence and duration bounds")
    void createReelRequest_Validation() {
        CreateReelRequest valid = CreateReelRequest.builder()
                .videoUrl("https://res.cloudinary.com/video.mp4")
                .caption("Reel caption")
                .durationMs(15000L)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        CreateReelRequest blankUrl = CreateReelRequest.builder()
                .videoUrl("  ")
                .durationMs(-50L)
                .build();
        Set<ConstraintViolation<CreateReelRequest>> violations = validator.validate(blankUrl);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("videoUrl"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("durationMs"));
    }

    @Test
    @DisplayName("CreateStoryRequest - Media URL and type presence")
    void createStoryRequest_Validation() {
        CreateStoryRequest valid = CreateStoryRequest.builder()
                .mediaUrl("https://cdn.example.com/story.jpg")
                .mediaType(MediaType.IMAGE)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        CreateStoryRequest invalid = CreateStoryRequest.builder()
                .mediaUrl("")
                .mediaType(null)
                .build();
        Set<ConstraintViolation<CreateStoryRequest>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("mediaUrl"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("mediaType"));
    }

    // --- Chat & Engagement Requests ---

    @Test
    @DisplayName("SendMessageRequest - Content and media length bounds")
    void sendMessageRequest_Validation() {
        SendMessageRequest valid = SendMessageRequest.builder()
                .content("Hello there!")
                .messageType(MessageType.TEXT)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        SendMessageRequest oversized = SendMessageRequest.builder()
                .content("m".repeat(2001))
                .mediaUrl("u".repeat(1001))
                .messageType(null)
                .build();
        Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(oversized);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("mediaUrl"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("messageType"));
    }

    @Test
    @DisplayName("TypingEvent - Validates conversationId and userId")
    void typingEvent_Validation() {
        TypingEvent valid = TypingEvent.builder()
                .conversationId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .typing(true)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        TypingEvent invalid = TypingEvent.builder().build();
        Set<ConstraintViolation<TypingEvent>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("conversationId"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("userId"));
    }

    @Test
    @DisplayName("CreateCommentRequest - Validates comment text presence and limit")
    void createCommentRequest_Validation() {
        CreateCommentRequest valid = CreateCommentRequest.builder()
                .text("Nice post!")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        CreateCommentRequest blank = CreateCommentRequest.builder()
                .text("   ")
                .build();
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("text"));

        CreateCommentRequest tooLong = CreateCommentRequest.builder()
                .text("c".repeat(1001))
                .build();
        assertThat(validator.validate(tooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("text"));
    }

    // --- Admin & Mail Requests ---

    @Test
    @DisplayName("CreateReportRequest - Validates target, reason and description bounds")
    void createReportRequest_Validation() {
        CreateReportRequest valid = CreateReportRequest.builder()
                .targetType(ReportTargetType.POST)
                .targetId(UUID.randomUUID())
                .reasonCode("SPAM")
                .description("Detailed description")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        CreateReportRequest invalid = CreateReportRequest.builder()
                .targetType(null)
                .targetId(null)
                .reasonCode("")
                .description("d".repeat(1001))
                .build();
        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("targetType"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("targetId"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("reasonCode"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    @DisplayName("ResolveReportRequest - Validates status presence and resolution notes limit")
    void resolveReportRequest_Validation() {
        ResolveReportRequest valid = ResolveReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .resolutionNotes("Action taken.")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        ResolveReportRequest invalid = ResolveReportRequest.builder()
                .status(null)
                .resolutionNotes("n".repeat(1001))
                .build();
        Set<ConstraintViolation<ResolveReportRequest>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("resolutionNotes"));
    }

    @Test
    @DisplayName("UpdateUserStatusRequest - Validates status presence")
    void updateUserStatusRequest_Validation() {
        UpdateUserStatusRequest valid = UpdateUserStatusRequest.builder()
                .status(UserStatus.ACTIVE)
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UpdateUserStatusRequest invalid = UpdateUserStatusRequest.builder().build();
        assertThat(validator.validate(invalid)).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

    @Test
    @DisplayName("UnsubscribeRequest - Validates parameters, positive expiry and signature")
    void unsubscribeRequest_Validation() {
        UnsubscribeRequest valid = UnsubscribeRequest.builder()
                .userId(UUID.randomUUID())
                .emailType(EmailType.POST_LIKED)
                .expires(System.currentTimeMillis() + 86400000L)
                .signature("valid-hmac-signature")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UnsubscribeRequest invalid = UnsubscribeRequest.builder()
                .userId(null)
                .emailType(null)
                .expires(-100L)
                .signature("")
                .build();
        Set<ConstraintViolation<UnsubscribeRequest>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("userId"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("emailType"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expires"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("signature"));
    }

    @Test
    @DisplayName("UpdateEmailPreferencesRequest - Validates digest frequency pattern")
    void updateEmailPreferencesRequest_Validation() {
        UpdateEmailPreferencesRequest valid = UpdateEmailPreferencesRequest.builder()
                .digestFrequency("WEEKLY")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UpdateEmailPreferencesRequest invalid = UpdateEmailPreferencesRequest.builder()
                .digestFrequency("HOURLY")
                .build();
        assertThat(validator.validate(invalid)).anyMatch(v -> v.getPropertyPath().toString().equals("digestFrequency"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Validates bio, websiteUrl and displayName bounds")
    void updateProfileRequest_Validation() {
        UpdateProfileRequest valid = UpdateProfileRequest.builder()
                .displayName("John Doe")
                .bio("Software Engineer")
                .websiteUrl("https://example.com")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UpdateProfileRequest oversized = UpdateProfileRequest.builder()
                .displayName("d".repeat(101))
                .bio("b".repeat(501))
                .websiteUrl("w".repeat(501))
                .build();
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(oversized);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("displayName"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("bio"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("websiteUrl"));
    }

    @Test
    @DisplayName("UpdateAvatarRequest - Validates non-blank URL and bounds")
    void updateAvatarRequest_Validation() {
        UpdateAvatarRequest valid = UpdateAvatarRequest.builder()
                .avatarUrl("https://cloudinary.com/avatar.jpg")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        UpdateAvatarRequest blank = UpdateAvatarRequest.builder()
                .avatarUrl("   ")
                .build();
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("avatarUrl"));

        UpdateAvatarRequest oversized = UpdateAvatarRequest.builder()
                .avatarUrl("https://example.com/" + "a".repeat(500))
                .build();
        assertThat(validator.validate(oversized)).anyMatch(v -> v.getPropertyPath().toString().equals("avatarUrl"));

        UpdateAvatarRequest invalidScheme = UpdateAvatarRequest.builder()
                .avatarUrl("ftp://cloudinary.com/avatar.jpg")
                .build();
        assertThat(validator.validate(invalidScheme)).anyMatch(v -> v.getPropertyPath().toString().equals("avatarUrl"));
    }

    @Test
    @DisplayName("RegisterRequest - Comprehensive regex validations for username, email, password, and displayName")
    void registerRequest_RegexValidations() {
        RegisterRequest valid = RegisterRequest.builder()
                .username("john_doe_99")
                .email("john.doe@example.com")
                .password("StrongP@ss123")
                .displayName("John Doe")
                .build();
        assertThat(validator.validate(valid)).isEmpty();

        // Weak passwords failing complexity regex
        for (String weakPassword : List.of("lowercaseonly1!", "UPPERCASEONLY1!", "NoSpecialChar123", "NoDigits!Abc", "Sh0rt!", "Has Space123!")) {
            RegisterRequest req = RegisterRequest.builder()
                    .username("john_doe_99")
                    .email("john.doe@example.com")
                    .password(weakPassword)
                    .displayName("John Doe")
                    .build();
            assertThat(validator.validate(req))
                    .as("Password '%s' should fail complexity regex", weakPassword)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        // Invalid emails
        for (String invalidEmail : List.of("plainaddress", "@missingusername.com", "user@.com", "user@domain..com")) {
            RegisterRequest req = RegisterRequest.builder()
                    .username("john_doe_99")
                    .email(invalidEmail)
                    .password("StrongP@ss123")
                    .displayName("John Doe")
                    .build();
            assertThat(validator.validate(req))
                    .as("Email '%s' should fail regex", invalidEmail)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        // Invalid usernames
        for (String invalidUsername : List.of(".leadingdot", "trailingdot.", "consecutive..dots", "bad@symbol", "ab")) {
            RegisterRequest req = RegisterRequest.builder()
                    .username(invalidUsername)
                    .email("john.doe@example.com")
                    .password("StrongP@ss123")
                    .displayName("John Doe")
                    .build();
            assertThat(validator.validate(req))
                    .as("Username '%s' should fail regex", invalidUsername)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }
    }

    @Test
    @DisplayName("Media URL Regex - Validates media URLs across post, reel, story and chat requests")
    void mediaUrl_RegexValidations() {
        // Valid URLs (both relative path and HTTP/HTTPS)
        PostMediaRequest validMediaRelative = PostMediaRequest.builder()
                .mediaUrl("/uploads/2026/image.jpg")
                .mediaType(MediaType.IMAGE)
                .build();
        assertThat(validator.validate(validMediaRelative)).isEmpty();

        CreateReelRequest validReel = CreateReelRequest.builder()
                .videoUrl("https://cdn.example.com/reel.mp4")
                .thumbnailUrl("")
                .build();
        assertThat(validator.validate(validReel)).isEmpty();

        CreateStoryRequest validStory = CreateStoryRequest.builder()
                .mediaUrl("http://cdn.example.com/story.png")
                .mediaType(MediaType.IMAGE)
                .build();
        assertThat(validator.validate(validStory)).isEmpty();

        SendMessageRequest validMsg = SendMessageRequest.builder()
                .content("Photo message")
                .messageType(MessageType.IMAGE)
                .mediaUrl("https://cdn.example.com/chat.jpg")
                .build();
        assertThat(validator.validate(validMsg)).isEmpty();

        // Invalid URLs (wrong protocol, raw invalid string)
        PostMediaRequest invalidMedia = PostMediaRequest.builder()
                .mediaUrl("javascript:alert(1)")
                .mediaType(MediaType.IMAGE)
                .build();
        assertThat(validator.validate(invalidMedia)).anyMatch(v -> v.getPropertyPath().toString().equals("mediaUrl"));

        CreateReelRequest invalidReel = CreateReelRequest.builder()
                .videoUrl("ftp://video.mp4")
                .thumbnailUrl("invalid-thumb")
                .build();
        Set<ConstraintViolation<CreateReelRequest>> reelViolations = validator.validate(invalidReel);
        assertThat(reelViolations).anyMatch(v -> v.getPropertyPath().toString().equals("videoUrl"));
        assertThat(reelViolations).anyMatch(v -> v.getPropertyPath().toString().equals("thumbnailUrl"));

        CreateStoryRequest invalidStory = CreateStoryRequest.builder()
                .mediaUrl("data:image/png;base64,xxxx")
                .mediaType(MediaType.IMAGE)
                .build();
        assertThat(validator.validate(invalidStory)).anyMatch(v -> v.getPropertyPath().toString().equals("mediaUrl"));

        SendMessageRequest invalidMsg = SendMessageRequest.builder()
                .content("Photo")
                .messageType(MessageType.IMAGE)
                .mediaUrl("ftp://cdn.example.com/chat.jpg")
                .build();
        assertThat(validator.validate(invalidMsg)).anyMatch(v -> v.getPropertyPath().toString().equals("mediaUrl"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Validates website URL regex")
    void updateProfileRequest_WebsiteUrlRegex() {
        UpdateProfileRequest invalidWebsite = UpdateProfileRequest.builder()
                .websiteUrl("not_a_valid_website")
                .build();
        assertThat(validator.validate(invalidWebsite)).anyMatch(v -> v.getPropertyPath().toString().equals("websiteUrl"));

        UpdateProfileRequest validWebsite = UpdateProfileRequest.builder()
                .websiteUrl("https://instagram.com/myprofile")
                .build();
        assertThat(validator.validate(validWebsite)).isEmpty();

        UpdateProfileRequest validDomainWithoutScheme = UpdateProfileRequest.builder()
                .websiteUrl("myportfolio.dev")
                .build();
        assertThat(validator.validate(validDomainWithoutScheme)).isEmpty();
    }
}
