package com.instagramclone.security.otp;

import com.instagramclone.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailOtpServiceTest {

    private OtpProperties otpProperties;
    private EmailOtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpProperties = new OtpProperties();
        otpProperties.setSecret("test-email-otp-hmac-sha256-secret-32bytes-long");
        otpProperties.setLength(6);
        otpProperties.setValidityMinutes(5);
        otpProperties.setMaxAttempts(5);
        otpProperties.setResendCooldownSeconds(60);
        otpProperties.setMaxHourlyResends(5);

        // Instantiate with null RedisTemplate to test in-memory resilient fallback engine
        otpService = new EmailOtpServiceImpl(otpProperties, null);
    }

    @Test
    @DisplayName("OTP Generation - Strictly produces 6-digit numeric codes with leading zeroes preserved")
    void generateOtp_PreservesSixDigitsAndLeadingZeroes() {
        Pattern pattern = Pattern.compile("^[0-9]{6}$");
        for (int i = 0; i < 50; i++) {
            OtpChallengeResult challenge = otpService.createOtpChallenge("user" + i + "@test.com", null);
            assertThat(challenge.getRawOtp()).isNotNull();
            assertThat(challenge.getRawOtp()).hasSize(6);
            assertThat(pattern.matcher(challenge.getRawOtp()).matches()).isTrue();
            assertThat(challenge.getValiditySeconds()).isEqualTo(300);
            assertThat(challenge.getCooldownSeconds()).isEqualTo(60);
        }
    }

    @Test
    @DisplayName("Challenge Verification - Correct OTP verifies successfully and is single-use consumed")
    void verifyOtp_SuccessAndSingleUseConsumption() {
        String email = "success@example.com";
        UUID userId = UUID.randomUUID();

        OtpChallengeResult challenge = otpService.createOtpChallenge(email, userId);
        String rawOtp = challenge.getRawOtp();

        // 1. Verify with correct OTP
        OtpVerificationResult result = otpService.verifyOtp(email, rawOtp);
        assertThat(result.getStatus()).isEqualTo(OtpVerificationStatus.SUCCESS);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getRemainingAttempts()).isEqualTo(0);
        assertThat(result.getUserId()).isEqualTo(userId);

        // 2. Second verification with same OTP must fail as already consumed
        OtpVerificationResult secondResult = otpService.verifyOtp(email, rawOtp);
        assertThat(secondResult.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);
        assertThat(secondResult.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Incorrect OTP - Decrements remaining attempts and preserves challenge")
    void verifyOtp_IncorrectCode_DecrementsAttempts() {
        String email = "attempts@example.com";
        OtpChallengeResult challenge = otpService.createOtpChallenge(email, null);
        String correctOtp = challenge.getRawOtp();
        String wrongOtp = correctOtp.equals("123456") ? "654321" : "123456";

        // Attempt 1: Wrong OTP
        OtpVerificationResult result1 = otpService.verifyOtp(email, wrongOtp);
        assertThat(result1.getStatus()).isEqualTo(OtpVerificationStatus.INVALID_CODE);
        assertThat(result1.getRemainingAttempts()).isEqualTo(4);

        // Attempt 2: Wrong OTP
        OtpVerificationResult result2 = otpService.verifyOtp(email, wrongOtp);
        assertThat(result2.getStatus()).isEqualTo(OtpVerificationStatus.INVALID_CODE);
        assertThat(result2.getRemainingAttempts()).isEqualTo(3);

        // Attempt 3: Now provide correct OTP -> succeeds!
        OtpVerificationResult result3 = otpService.verifyOtp(email, correctOtp);
        assertThat(result3.getStatus()).isEqualTo(OtpVerificationStatus.SUCCESS);
    }

    @Test
    @DisplayName("Max Attempts Exceeded - Burns challenge after 5 incorrect attempts")
    void verifyOtp_MaxAttemptsExceeded_BurnsChallenge() {
        String email = "bruteforce@example.com";
        OtpChallengeResult challenge = otpService.createOtpChallenge(email, null);
        String correctOtp = challenge.getRawOtp();
        String wrongOtp = "000000".equals(correctOtp) ? "999999" : "000000";

        for (int i = 1; i <= 4; i++) {
            OtpVerificationResult result = otpService.verifyOtp(email, wrongOtp);
            assertThat(result.getStatus()).isEqualTo(OtpVerificationStatus.INVALID_CODE);
            assertThat(result.getRemainingAttempts()).isEqualTo(5 - i);
        }

        // 5th failed attempt -> Burns challenge
        OtpVerificationResult burnedResult = otpService.verifyOtp(email, wrongOtp);
        assertThat(burnedResult.getStatus()).isEqualTo(OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED);
        assertThat(burnedResult.getRemainingAttempts()).isEqualTo(0);

        // Even with the correct OTP now, the challenge is burned and gone
        OtpVerificationResult nextResult = otpService.verifyOtp(email, correctOtp);
        assertThat(nextResult.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);
    }

    @Test
    @DisplayName("Resend Cooldown - Enforces minimum 60-second cooldown between requests")
    void createOtpChallenge_EnforcesResendCooldown() {
        String email = "cooldown@example.com";
        otpService.createOtpChallenge(email, null);

        // Immediate subsequent request must throw RateLimitExceededException
        assertThatThrownBy(() -> otpService.createOtpChallenge(email, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Please wait")
                .hasMessageContaining("seconds before requesting another verification code");

        int remainingCooldown = otpService.getRemainingCooldownSeconds(email);
        assertThat(remainingCooldown).isGreaterThan(0).isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Hourly Resend Cap - Rejects requests beyond max hourly limit")
    void createOtpChallenge_EnforcesHourlyCap() {
        String email = "hourlycap@example.com";
        otpProperties.setResendCooldownSeconds(0); // Set cooldown to 0 to test pure hourly cap
        otpService = new EmailOtpServiceImpl(otpProperties, null);

        // Allow up to 5 resends
        for (int i = 1; i <= 5; i++) {
            OtpChallengeResult result = otpService.createOtpChallenge(email, null);
            assertThat(result.getRemainingHourlyResends()).isEqualTo(5 - i);
        }

        // 6th attempt must be rejected by hourly cap
        assertThatThrownBy(() -> otpService.createOtpChallenge(email, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Hourly verification code limit reached");
    }

    @Test
    @DisplayName("Email Normalization - Case and whitespace variations target the exact same challenge")
    void emailNormalization_CaseInsensitiveChallenge() {
        String registeredEmail = "  User.Name@Example.COM  ";
        String normalizedEmail = "user.name@example.com";

        OtpChallengeResult challenge = otpService.createOtpChallenge(registeredEmail, null);
        String rawOtp = challenge.getRawOtp();

        // Verify with different casing and whitespace
        OtpVerificationResult result = otpService.verifyOtp("USER.NAME@EXAMPLE.COM", rawOtp);
        assertThat(result.getStatus()).isEqualTo(OtpVerificationStatus.SUCCESS);
    }

    @Test
    @DisplayName("HMAC Keyed Binding - Code generated for User A cannot verify User B")
    void hmacKeyedBinding_CannotVerifyDifferentUser() {
        OtpChallengeResult challengeA = otpService.createOtpChallenge("userA@test.com", null);
        String codeA = challengeA.getRawOtp();

        // Attempt to verify codeA against userB
        OtpVerificationResult resultB = otpService.verifyOtp("userB@test.com", codeA);
        assertThat(resultB.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);
    }

    @Test
    @DisplayName("Invalidate Challenge - Completely purges challenge and active cooldown")
    void invalidateChallenge_PurgesChallenge() {
        String email = "invalidate@example.com";
        OtpChallengeResult challenge = otpService.createOtpChallenge(email, null);

        otpService.invalidateChallenge(email);

        // Verification must now fail
        OtpVerificationResult verifyResult = otpService.verifyOtp(email, challenge.getRawOtp());
        assertThat(verifyResult.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);

        // Cooldown is reset, so a new challenge can be created immediately
        OtpChallengeResult newChallenge = otpService.createOtpChallenge(email, null);
        assertThat(newChallenge.getRawOtp()).isNotNull();
    }
}
