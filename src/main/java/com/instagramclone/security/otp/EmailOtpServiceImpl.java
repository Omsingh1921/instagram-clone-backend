package com.instagramclone.security.otp;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of IEmailOtpService providing cryptographically secure 6-digit OTP generation,
 * purpose-bound HMAC-SHA256 representation, atomic Redis Lua script state management, and resilient
 * in-memory fallback.
 */
@Service
public class EmailOtpServiceImpl implements IEmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OtpProperties otpProperties;
    private final StringRedisTemplate redisTemplate;

    // --- Redis Lua Scripts ---

    // 1. Create/Store Challenge with Cooldown and Hourly Cap
    private static final String CREATE_CHALLENGE_LUA =
            "local cooldownTtl = redis.call('TTL', KEYS[2])\n" +
            "if cooldownTtl > 0 then\n" +
            "    return {-1, cooldownTtl}\n" +
            "end\n" +
            "local hourly = tonumber(redis.call('GET', KEYS[3]) or 0)\n" +
            "if hourly >= tonumber(ARGV[5]) then\n" +
            "    local hourlyTtl = redis.call('TTL', KEYS[3])\n" +
            "    return {-2, hourlyTtl}\n" +
            "end\n" +
            "redis.call('HSET', KEYS[1], 'hmac', ARGV[1], 'userId', ARGV[2], 'attempts', 0)\n" +
            "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))\n" +
            "redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[4]))\n" +
            "local newHourly = redis.call('INCR', KEYS[3])\n" +
            "if newHourly == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[3], 3600)\n" +
            "end\n" +
            "return {1, tonumber(ARGV[5]) - newHourly}";

    private static final DefaultRedisScript<List> CREATE_SCRIPT =
            new DefaultRedisScript<>(CREATE_CHALLENGE_LUA, List.class);

    // 2. Atomic Verify Challenge with Single-Use Deletion and Failure Increment
    private static final String VERIFY_CHALLENGE_LUA =
            "if redis.call('EXISTS', KEYS[1]) == 0 then\n" +
            "    return {0, 0, ''}\n" +
            "end\n" +
            "local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or 0)\n" +
            "local maxAttempts = tonumber(ARGV[2])\n" +
            "if attempts >= maxAttempts then\n" +
            "    redis.call('DEL', KEYS[1])\n" +
            "    return {-1, 0, ''}\n" +
            "end\n" +
            "local storedHmac = redis.call('HGET', KEYS[1], 'hmac')\n" +
            "if storedHmac == ARGV[1] then\n" +
            "    local userId = redis.call('HGET', KEYS[1], 'userId') or ''\n" +
            "    redis.call('DEL', KEYS[1])\n" +
            "    return {1, 0, userId}\n" +
            "else\n" +
            "    local newAttempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)\n" +
            "    local remaining = maxAttempts - newAttempts\n" +
            "    if remaining <= 0 then\n" +
            "        redis.call('DEL', KEYS[1])\n" +
            "        return {-1, 0, ''}\n" +
            "    else\n" +
            "        return {-2, remaining, ''}\n" +
            "    end\n" +
            "end";

    private static final DefaultRedisScript<List> VERIFY_SCRIPT =
            new DefaultRedisScript<>(VERIFY_CHALLENGE_LUA, List.class);

    // --- In-Memory Fallback State ---
    private static class LocalChallenge {
        final String hmac;
        final UUID userId;
        final long expiryMillis;
        final AtomicInteger attempts = new AtomicInteger(0);

        LocalChallenge(String hmac, UUID userId, long expiryMillis) {
            this.hmac = hmac;
            this.userId = userId;
            this.expiryMillis = expiryMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryMillis;
        }
    }

    private static class LocalHourlyCounter {
        final AtomicInteger count = new AtomicInteger(0);
        final long resetTimeMillis;

        LocalHourlyCounter(long resetTimeMillis) {
            this.resetTimeMillis = resetTimeMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > resetTimeMillis;
        }
    }

    private final Map<String, LocalChallenge> localChallenges = new ConcurrentHashMap<>();
    private final Map<String, Long> localCooldowns = new ConcurrentHashMap<>();
    private final Map<String, LocalHourlyCounter> localHourlyCounters = new ConcurrentHashMap<>();

    @Autowired
    public EmailOtpServiceImpl(
            OtpProperties otpProperties,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.otpProperties = otpProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OtpChallengeResult createOtpChallenge(String email, UUID userId, OtpPurpose purpose) {
        OtpPurpose actualPurpose = purpose != null ? purpose : OtpPurpose.ACCOUNT_VERIFICATION;
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new BadRequestException("Email address is required for verification challenge");
        }

        String emailHash = calculateEmailHash(normalizedEmail);
        String rawOtp = generateNumericOtp(otpProperties.getLength());
        String otpHmac = calculateHmac(actualPurpose, normalizedEmail, rawOtp);

        int validitySeconds = otpProperties.getValidityMinutes() * 60;
        int cooldownSeconds = otpProperties.getResendCooldownSeconds();
        int maxHourly = otpProperties.getMaxHourlyResends();
        String userIdStr = userId != null ? userId.toString() : "";

        // Attempt Redis execution
        if (redisTemplate != null) {
            try {
                List<String> keys = List.of(
                        challengeKey(actualPurpose, emailHash),
                        cooldownKey(actualPurpose, emailHash),
                        resendCountKey(actualPurpose, emailHash)
                );

                List<?> result = redisTemplate.execute(
                        CREATE_SCRIPT,
                        keys,
                        otpHmac,
                        userIdStr,
                        String.valueOf(validitySeconds),
                        String.valueOf(cooldownSeconds),
                        String.valueOf(maxHourly)
                );

                if (result != null && !result.isEmpty()) {
                    long status = ((Number) result.get(0)).longValue();
                    long secondVal = result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;

                    if (status == -1) {
                        // Cooldown active
                        long remainingCooldown = Math.max(1, secondVal);
                        log.warn("OTP creation blocked by resend cooldown for purpose [{}] identifier [{}]. Retry after {}s",
                                actualPurpose, emailHash.substring(0, 8), remainingCooldown);
                        throw new RateLimitExceededException(
                                String.format("Please wait %d seconds before requesting another verification code.", remainingCooldown),
                                remainingCooldown,
                                1
                        );
                    }

                    if (status == -2) {
                        // Hourly cap exceeded
                        long retryAfter = Math.max(1, secondVal);
                        log.warn("OTP creation blocked by hourly resend cap for purpose [{}] identifier [{}]. Retry after {}s",
                                actualPurpose, emailHash.substring(0, 8), retryAfter);
                        throw new RateLimitExceededException(
                                "Hourly verification code limit reached. Please try again later.",
                                retryAfter,
                                maxHourly
                        );
                    }

                    int remainingHourly = (int) Math.max(0, secondVal);
                    log.info("Successfully created OTP challenge in Redis for purpose [{}] identifier [{}] (validity: {}s, cooldown: {}s)",
                            actualPurpose, emailHash.substring(0, 8), validitySeconds, cooldownSeconds);

                    return OtpChallengeResult.builder()
                            .rawOtp(rawOtp)
                            .validitySeconds(validitySeconds)
                            .cooldownSeconds(cooldownSeconds)
                            .remainingHourlyResends(remainingHourly)
                            .build();
                }
            } catch (RateLimitExceededException rle) {
                throw rle;
            } catch (Exception ex) {
                log.warn("Redis unavailable during OTP challenge creation for purpose [{}] identifier [{}]: {}. Falling back to in-memory store.",
                        actualPurpose, emailHash.substring(0, 8), ex.getMessage());
            }
        }

        // Fallback to in-memory state
        return createFallbackChallenge(actualPurpose, normalizedEmail, emailHash, rawOtp, otpHmac, userId, validitySeconds, cooldownSeconds, maxHourly);
    }

    @Override
    public OtpVerificationResult verifyOtp(String email, String presentedOtp, OtpPurpose purpose) {
        OtpPurpose actualPurpose = purpose != null ? purpose : OtpPurpose.ACCOUNT_VERIFICATION;
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || presentedOtp == null || presentedOtp.isBlank()) {
            return OtpVerificationResult.builder()
                    .status(OtpVerificationStatus.INVALID_CODE)
                    .remainingAttempts(0)
                    .build();
        }

        String trimmedCode = presentedOtp.trim();
        String emailHash = calculateEmailHash(normalizedEmail);
        String presentedHmac = calculateHmac(actualPurpose, normalizedEmail, trimmedCode);

        // Attempt Redis evaluation
        if (redisTemplate != null) {
            try {
                List<String> keys = Collections.singletonList(challengeKey(actualPurpose, emailHash));
                List<?> result = redisTemplate.execute(
                        VERIFY_SCRIPT,
                        keys,
                        presentedHmac,
                        String.valueOf(otpProperties.getMaxAttempts())
                );

                if (result != null && !result.isEmpty()) {
                    long code = ((Number) result.get(0)).longValue();
                    long remaining = result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;
                    String userIdStr = result.size() > 2 ? String.valueOf(result.get(2)) : "";

                    UUID userId = parseUuidSafely(userIdStr);

                    if (code == 1) {
                        log.info("OTP verification succeeded for purpose [{}] identifier [{}]. Code consumed.", actualPurpose, emailHash.substring(0, 8));
                        return OtpVerificationResult.builder()
                                .status(OtpVerificationStatus.SUCCESS)
                                .remainingAttempts(0)
                                .userId(userId)
                                .build();
                    } else if (code == -2) {
                        log.warn("Invalid OTP submitted for purpose [{}] identifier [{}]. Remaining attempts: {}",
                                actualPurpose, emailHash.substring(0, 8), remaining);
                        return OtpVerificationResult.builder()
                                .status(OtpVerificationStatus.INVALID_CODE)
                                .remainingAttempts((int) remaining)
                                .userId(userId)
                                .build();
                    } else if (code == -1) {
                        log.warn("OTP challenge burned due to maximum attempts for purpose [{}] identifier [{}]", actualPurpose, emailHash.substring(0, 8));
                        return OtpVerificationResult.builder()
                                .status(OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED)
                                .remainingAttempts(0)
                                .userId(userId)
                                .build();
                    } else {
                        log.info("OTP challenge expired or not found for purpose [{}] identifier [{}]", actualPurpose, emailHash.substring(0, 8));
                        return OtpVerificationResult.builder()
                                .status(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND)
                                .remainingAttempts(0)
                                .build();
                    }
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable during OTP verification for purpose [{}] identifier [{}]: {}. Falling back to in-memory store.",
                        actualPurpose, emailHash.substring(0, 8), ex.getMessage());
            }
        }

        // Fallback to in-memory state
        return verifyFallbackChallenge(actualPurpose, emailHash, presentedHmac);
    }

    @Override
    public int getRemainingCooldownSeconds(String email, OtpPurpose purpose) {
        OtpPurpose actualPurpose = purpose != null ? purpose : OtpPurpose.ACCOUNT_VERIFICATION;
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) return 0;
        String emailHash = calculateEmailHash(normalizedEmail);

        if (redisTemplate != null) {
            try {
                Long ttl = redisTemplate.getExpire(cooldownKey(actualPurpose, emailHash), TimeUnit.SECONDS);
                if (ttl != null && ttl > 0) {
                    return ttl.intValue();
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable while querying cooldown for purpose [{}] identifier [{}]: {}", actualPurpose, emailHash.substring(0, 8), ex.getMessage());
            }
        }

        String stateKey = purposeStateKey(actualPurpose, emailHash);
        Long cooldownExpiry = localCooldowns.get(stateKey);
        if (cooldownExpiry != null) {
            long remaining = (cooldownExpiry - System.currentTimeMillis()) / 1000L;
            return remaining > 0 ? (int) remaining : 0;
        }
        return 0;
    }

    @Override
    public void invalidateChallenge(String email, OtpPurpose purpose) {
        OtpPurpose actualPurpose = purpose != null ? purpose : OtpPurpose.ACCOUNT_VERIFICATION;
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) return;
        String emailHash = calculateEmailHash(normalizedEmail);

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(List.of(challengeKey(actualPurpose, emailHash), cooldownKey(actualPurpose, emailHash)));
                log.info("Invalidated OTP challenge and cooldown in Redis for purpose [{}] identifier [{}]", actualPurpose, emailHash.substring(0, 8));
            } catch (Exception ex) {
                log.warn("Redis unavailable during OTP invalidation: {}", ex.getMessage());
            }
        }

        String stateKey = purposeStateKey(actualPurpose, emailHash);
        localChallenges.remove(stateKey);
        localCooldowns.remove(stateKey);
    }

    public void clearAllLocalState() {
        localChallenges.clear();
        localCooldowns.clear();
        localHourlyCounters.clear();
    }

    // --- Private Helper Methods ---

    private OtpChallengeResult createFallbackChallenge(
            OtpPurpose purpose,
            String normalizedEmail,
            String emailHash,
            String rawOtp,
            String otpHmac,
            UUID userId,
            int validitySeconds,
            int cooldownSeconds,
            int maxHourly) {

        long now = System.currentTimeMillis();
        String stateKey = purposeStateKey(purpose, emailHash);

        synchronized (stateKey.intern()) {
            // Check cooldown
            Long cooldownUntil = localCooldowns.get(stateKey);
            if (cooldownUntil != null && now < cooldownUntil) {
                long remaining = Math.max(1, (cooldownUntil - now) / 1000L);
                throw new RateLimitExceededException(
                        String.format("Please wait %d seconds before requesting another verification code.", remaining),
                        remaining,
                        1
                );
            }

            // Check hourly cap
            LocalHourlyCounter hourly = localHourlyCounters.compute(stateKey, (k, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new LocalHourlyCounter(now + 3600_000L);
                }
                return existing;
            });

            if (hourly.count.get() >= maxHourly) {
                long retryAfter = Math.max(1, (hourly.resetTimeMillis - now) / 1000L);
                throw new RateLimitExceededException(
                        "Hourly verification code limit reached. Please try again later.",
                        retryAfter,
                        maxHourly
                );
            }

            // Store challenge
            localChallenges.put(stateKey, new LocalChallenge(otpHmac, userId, now + (validitySeconds * 1000L)));
            localCooldowns.put(stateKey, now + (cooldownSeconds * 1000L));
            int currentResend = hourly.count.incrementAndGet();

            int remainingHourly = Math.max(0, maxHourly - currentResend);
            log.info("Successfully created OTP challenge in local fallback for purpose [{}] identifier [{}]", purpose, emailHash.substring(0, 8));

            return OtpChallengeResult.builder()
                    .rawOtp(rawOtp)
                    .validitySeconds(validitySeconds)
                    .cooldownSeconds(cooldownSeconds)
                    .remainingHourlyResends(remainingHourly)
                    .build();
        }
    }

    private OtpVerificationResult verifyFallbackChallenge(OtpPurpose purpose, String emailHash, String presentedHmac) {
        String stateKey = purposeStateKey(purpose, emailHash);
        synchronized (stateKey.intern()) {
            LocalChallenge challenge = localChallenges.get(stateKey);
            if (challenge == null || challenge.isExpired()) {
                localChallenges.remove(stateKey);
                return OtpVerificationResult.builder()
                        .status(OtpVerificationStatus.EXPIRED_OR_NOT_FOUND)
                        .remainingAttempts(0)
                        .build();
            }

            int attempts = challenge.attempts.get();
            int maxAttempts = otpProperties.getMaxAttempts();

            if (attempts >= maxAttempts) {
                localChallenges.remove(stateKey);
                return OtpVerificationResult.builder()
                        .status(OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED)
                        .remainingAttempts(0)
                        .userId(challenge.userId)
                        .build();
            }

            boolean matches = MessageDigest.isEqual(
                    challenge.hmac.getBytes(StandardCharsets.UTF_8),
                    presentedHmac.getBytes(StandardCharsets.UTF_8)
            );

            if (matches) {
                localChallenges.remove(stateKey);
                return OtpVerificationResult.builder()
                        .status(OtpVerificationStatus.SUCCESS)
                        .remainingAttempts(0)
                        .userId(challenge.userId)
                        .build();
            } else {
                int newAttempts = challenge.attempts.incrementAndGet();
                int remaining = maxAttempts - newAttempts;
                if (remaining <= 0) {
                    localChallenges.remove(stateKey);
                    return OtpVerificationResult.builder()
                        .status(OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED)
                        .remainingAttempts(0)
                        .userId(challenge.userId)
                        .build();
                } else {
                    return OtpVerificationResult.builder()
                            .status(OtpVerificationStatus.INVALID_CODE)
                            .remainingAttempts(remaining)
                            .userId(challenge.userId)
                            .build();
                }
            }
        }
    }

    private String generateNumericOtp(int length) {
        int bound = (int) Math.pow(10, length);
        int code = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + length + "d", code);
    }

    private String calculateHmac(OtpPurpose purpose, String normalizedEmail, String rawOtp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    otpProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKey);
            String payload = purpose.name() + ":" + normalizedEmail + ":" + rawOtp;
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed computing HMAC-SHA256 for OTP challenge", ex);
        }
    }

    private String calculateEmailHash(String normalizedEmail) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm missing from runtime", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private String challengeKey(OtpPurpose purpose, String emailHash) {
        return "otp:" + purpose.name().toLowerCase() + ":challenge:" + emailHash;
    }

    private String cooldownKey(OtpPurpose purpose, String emailHash) {
        return "otp:" + purpose.name().toLowerCase() + ":cooldown:" + emailHash;
    }

    private String resendCountKey(OtpPurpose purpose, String emailHash) {
        return "otp:" + purpose.name().toLowerCase() + ":resend-count:" + emailHash;
    }

    private String purposeStateKey(OtpPurpose purpose, String emailHash) {
        return purpose.name() + ":" + emailHash;
    }

    private UUID parseUuidSafely(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            return UUID.fromString(uuidStr.trim());
        } catch (Exception ex) {
            return null;
        }
    }
}
