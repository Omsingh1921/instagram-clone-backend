package com.instagramclone.security.otp;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailOtpServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IEmailOtpService emailOtpService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUpOtpState() {
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys("otp:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    @Test
    @DisplayName("Redis State - Key names use SHA-256 hashes with purpose namespaces instead of raw email addresses")
    void redisKeys_UseKeyedIdentifierWithoutRawEmail() throws Exception {
        String testEmail = "privacy.sensitive@example.com";
        OtpChallengeResult challenge = emailOtpService.createOtpChallenge(testEmail, null, OtpPurpose.ACCOUNT_VERIFICATION);
        assertThat(challenge.getRawOtp()).hasSize(6);

        String signupEmail = "signup.sensitive@example.com";
        OtpChallengeResult signupChallenge = emailOtpService.createOtpChallenge(signupEmail, null, OtpPurpose.SIGNUP);
        assertThat(signupChallenge.getRawOtp()).hasSize(6);

        if (redisTemplate != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String expectedAccountHash = HexFormat.of().formatHex(digest.digest(testEmail.getBytes(StandardCharsets.UTF_8)));
            String expectedSignupHash = HexFormat.of().formatHex(digest.digest(signupEmail.getBytes(StandardCharsets.UTF_8)));

            // Keys in Redis must be hashed with purpose separation, never containing raw email
            Set<String> accountKeys = redisTemplate.keys("otp:account_verification:challenge:*");
            assertThat(accountKeys).contains("otp:account_verification:challenge:" + expectedAccountHash);

            Set<String> signupKeys = redisTemplate.keys("otp:signup:challenge:*");
            assertThat(signupKeys).contains("otp:signup:challenge:" + expectedSignupHash);

            // Namespaces must not overlap
            assertThat(accountKeys).doesNotContain("otp:signup:challenge:" + expectedSignupHash);
            assertThat(signupKeys).doesNotContain("otp:account_verification:challenge:" + expectedAccountHash);

            Set<String> rawEmailKeys = redisTemplate.keys("*" + testEmail + "*");
            assertThat(rawEmailKeys).isEmpty();

            Set<String> rawSignupEmailKeys = redisTemplate.keys("*" + signupEmail + "*");
            assertThat(rawSignupEmailKeys).isEmpty();

            // Hash must contain hmac, userId, attempts
            String hmac = (String) redisTemplate.opsForHash().get("otp:account_verification:challenge:" + expectedAccountHash, "hmac");
            assertThat(hmac).isNotNull();
            assertThat(hmac).hasSize(64); // 64 hex chars for HMAC-SHA256
            assertThat(hmac).isNotEqualTo(challenge.getRawOtp()); // Plaintext OTP is NEVER stored

            String signupHmac = (String) redisTemplate.opsForHash().get("otp:signup:challenge:" + expectedSignupHash, "hmac");
            assertThat(signupHmac).isNotNull();
            assertThat(signupHmac).hasSize(64);
            assertThat(signupHmac).isNotEqualTo(signupChallenge.getRawOtp());
        }
    }

    @Test
    @DisplayName("Redis State - Enforces atomic Lua script verify with single-use deletion")
    void verifyOtp_LuaScript_AtomicSingleUse() {
        String email = "redis.atomic@example.com";
        UUID userId = UUID.randomUUID();

        OtpChallengeResult challenge = emailOtpService.createOtpChallenge(email, userId);
        String code = challenge.getRawOtp();

        // Verify with correct code
        OtpVerificationResult result = emailOtpService.verifyOtp(email, code);
        assertThat(result.getStatus()).isEqualTo(OtpVerificationStatus.SUCCESS);
        assertThat(result.getUserId()).isEqualTo(userId);

        // Challenge must be completely deleted in Redis (single use)
        if (redisTemplate != null) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
                String hash = HexFormat.of().formatHex(digest.digest(email.getBytes(StandardCharsets.UTF_8)));
                Boolean exists = redisTemplate.hasKey("otp:account_verification:challenge:" + hash);
                assertThat(exists).isFalse();
            } catch (Exception ignored) {
            }
        }

        // Replay of same OTP must fail
        OtpVerificationResult replayResult = emailOtpService.verifyOtp(email, code);
        assertThat(replayResult.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);
    }

    @Test
    @DisplayName("Redis State - Atomically increments attempts on wrong code and burns on 5th attempt")
    void verifyOtp_LuaScript_AttemptsIncrementAndBurn() {
        String email = "redis.burn@example.com";
        OtpChallengeResult challenge = emailOtpService.createOtpChallenge(email, null);
        String correctCode = challenge.getRawOtp();
        String wrongCode = "123456".equals(correctCode) ? "654321" : "123456";

        // Attempts 1 to 4: INVALID_CODE with decremented remaining attempts
        for (int i = 1; i <= 4; i++) {
            OtpVerificationResult result = emailOtpService.verifyOtp(email, wrongCode);
            assertThat(result.getStatus()).isEqualTo(OtpVerificationStatus.INVALID_CODE);
            assertThat(result.getRemainingAttempts()).isEqualTo(5 - i);
        }

        // Attempt 5: Burns the challenge
        OtpVerificationResult burned = emailOtpService.verifyOtp(email, wrongCode);
        assertThat(burned.getStatus()).isEqualTo(OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED);

        // Subsequent attempt with the CORRECT code returns EXPIRED_OR_NOT_FOUND because it was burned
        OtpVerificationResult replayCorrect = emailOtpService.verifyOtp(email, correctCode);
        assertThat(replayCorrect.getStatus()).isEqualTo(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND);
    }

    @Test
    @DisplayName("Redis State - Enforces resend cooldown with Redis TTL")
    void cooldown_RedisTtlEnforcement() {
        String email = "redis.cooldown@example.com";
        emailOtpService.createOtpChallenge(email, null);

        int cooldownSeconds = emailOtpService.getRemainingCooldownSeconds(email);
        assertThat(cooldownSeconds).isGreaterThan(0).isLessThanOrEqualTo(60);

        assertThatThrownBy(() -> emailOtpService.createOtpChallenge(email, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Please wait");
    }
}
