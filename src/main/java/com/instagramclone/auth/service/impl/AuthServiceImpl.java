package com.instagramclone.auth.service.impl;

import com.instagramclone.auth.service.interfaces.*;
import com.instagramclone.user.service.interfaces.*;

import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.common.exception.ApiException;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ConflictException;
import com.instagramclone.common.exception.RateLimitExceededException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.exception.UnauthorizedException;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventPublisher;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.ratelimit.RateLimitProperties;
import com.instagramclone.security.IAccountLockoutService;
import com.instagramclone.security.IDeviceDetectionService;
import com.instagramclone.security.JwtProperties;
import com.instagramclone.security.JwtTokenProvider;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.security.UserPrincipal;
import com.instagramclone.security.otp.IEmailOtpService;
import com.instagramclone.security.otp.OtpChallengeResult;
import com.instagramclone.security.otp.OtpPurpose;
import com.instagramclone.security.otp.OtpVerificationResult;
import com.instagramclone.security.otp.OtpVerificationStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import com.instagramclone.auth.constants.AuthConstants;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import com.instagramclone.auth.dto.response.*;
import com.instagramclone.user.dto.response.*;
import com.instagramclone.auth.entity.EmailVerificationToken;
import com.instagramclone.auth.entity.PasswordResetToken;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.auth.entity.RefreshToken;
import com.instagramclone.auth.entity.SignupVerificationTicket;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.mapper.UserMapper;
import com.instagramclone.auth.repository.EmailVerificationTokenRepository;
import com.instagramclone.auth.repository.PasswordResetTokenRepository;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import com.instagramclone.auth.repository.SignupVerificationTicketRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.audit.service.interfaces.IAuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SignupVerificationTicketRepository signupVerificationTicketRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final IAuditLogService auditLogService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final MailProperties mailProperties;
    private final EmailEventPublisher emailEventPublisher;
    private final IDeviceDetectionService deviceDetectionService;
    private final IAccountLockoutService accountLockoutService;
    private final com.instagramclone.security.ClientIpResolver clientIpResolver;
    private final com.instagramclone.ratelimit.IRateLimiterService rateLimiterService;
    private final com.instagramclone.ratelimit.RateLimitProperties rateLimitProperties;
    private final IEmailOtpService emailOtpService;
    private final IMailService mailService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;
    private final com.instagramclone.auth.service.interfaces.IUsernameSuggestionService usernameSuggestionService;

    @Override
    public void requestSignupOtp(SignupRequestOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Email-targeted rate limiting
        if (rateLimitProperties.isEnabled()) {
            RateLimitProperties.CategoryLimit emailLimitConfig = rateLimitProperties
                    .getCategoryLimit("auth-signup-request-otp-email");
            int emailLimit = emailLimitConfig.getLimit() > 0 ? emailLimitConfig.getLimit() : 5;
            int windowSeconds = emailLimitConfig.getWindowSeconds() > 0 ? emailLimitConfig.getWindowSeconds() : 60;

            String emailRateLimitKey = String.format("rate_limit:auth-signup-request:email:%s", normalizedEmail);
            com.instagramclone.ratelimit.RateLimitResult rateLimitResult = rateLimiterService
                    .checkRateLimit(emailRateLimitKey, emailLimit, windowSeconds);

            if (!rateLimitResult.isAllowed()) {
                log.warn("Email-targeted signup OTP rate limit exceeded for [{}] (limit: {}/{}s, retry-after: {}s)",
                        maskEmail(normalizedEmail), emailLimit, windowSeconds, rateLimitResult.getRetryAfterSeconds());
                throw new RateLimitExceededException(
                        String.format("Too many verification requests. Please try again in %d seconds.",
                                rateLimitResult.getRetryAfterSeconds()),
                        rateLimitResult.getRetryAfterSeconds(),
                        rateLimitResult.getLimit());
            }
        }

        // Anti-enumeration: If 3 accounts already exist with this email, do not generate
        // OTP or reveal existence
        long existingCount = userRepository.countByEmail(normalizedEmail);
        if (existingCount >= 3) {
            log.info("Signup OTP suppressed for email at maximum capacity (3 accounts): {}", maskEmail(normalizedEmail));
            return;
        }

        // Create OTP challenge in Redis for SIGNUP purpose (enforces resend cooldown
        // and hourly cap)
        OtpChallengeResult challenge = emailOtpService.createOtpChallenge(normalizedEmail, null, OtpPurpose.SIGNUP);

        // Send OTP email synchronously via IMailService
        EmailDeliveryLog deliveryLog = mailService.sendOtpEmail(normalizedEmail, "there", challenge.getRawOtp());
        if (deliveryLog == null || deliveryLog.getStatus() == EmailDeliveryStatus.FAILED) {
            log.error("Synchronous OTP email delivery failed for signup email [{}]. Invalidating challenge.",
                    maskEmail(normalizedEmail));
            emailOtpService.invalidateChallenge(normalizedEmail, OtpPurpose.SIGNUP);
            throw new ApiException("Failed to dispatch verification email. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR, "MAIL_DELIVERY_FAILED");
        }

        log.info("Successfully sent signup OTP to [{}]", maskEmail(normalizedEmail));
    }

    @Override
    @Transactional
    public SignupProofResponse verifySignupOtp(SignupVerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();

        // Atomically verify OTP with SIGNUP purpose isolation
        OtpVerificationResult result = emailOtpService.verifyOtp(normalizedEmail, code, OtpPurpose.SIGNUP);

        if (result.getStatus() == OtpVerificationStatus.INVALID_CODE) {
            throw new BadRequestException(String.format("Invalid verification code. %d attempt(s) remaining.",
                    result.getRemainingAttempts()));
        }

        if (result.getStatus() == OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED) {
            throw new BadRequestException(
                    "Maximum verification attempts exceeded. Please request a new verification code.");
        }

        if (result.getStatus() == OtpVerificationStatus.EXPIRED_OR_NOT_FOUND) {
            throw new BadRequestException(
                    "Verification code has expired or does not exist. Please request a new code.");
        }

        // Generate cryptographically secure random token (256 bits of entropy)
        String rawSignupToken = generateSecureToken();
        String tokenHash = hashToken(rawSignupToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(600);

        // Invalidate any existing unused tickets for this email
        signupVerificationTicketRepository.invalidateExistingTicketsForEmail(normalizedEmail, LocalDateTime.now());

        SignupVerificationTicket ticket = SignupVerificationTicket.builder()
                .email(normalizedEmail)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        signupVerificationTicketRepository.save(ticket);

        log.info("Generated signup verification proof for [{}] valid for 600s", maskEmail(normalizedEmail));

        return SignupProofResponse.builder()
                .signupToken(rawSignupToken)
                .expiresIn(600L)
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request.getSignupToken() == null || request.getSignupToken().isBlank()) {
            throw new BadRequestException(AuthConstants.ERR_SIGNUP_PROOF_REQUIRED);
        }

        String rawToken = request.getSignupToken().trim();
        String tokenHash = hashToken(rawToken);

        SignupVerificationTicket initialTicket = signupVerificationTicketRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(AuthConstants.ERR_SIGNUP_TICKET_INVALID));

        if (!initialTicket.isValid()) {
            throw new BadRequestException(AuthConstants.ERR_SIGNUP_TOKEN_EXPIRED_OR_USED);
        }

        String normalizedEmail = initialTicket.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

        synchronized (normalizedEmail.intern()) {
            org.springframework.transaction.support.TransactionTemplate txTemplate =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            return txTemplate.execute(status -> executeRegistration(request, tokenHash, normalizedEmail));
        }
    }

    private AuthResponse executeRegistration(RegisterRequest request, String tokenHash, String normalizedEmail) {
        // Pessimistic write lock ensures two concurrent requests presenting the same
        // proof serialize at the database level and cannot both register
        SignupVerificationTicket ticket = signupVerificationTicketRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequestException(AuthConstants.ERR_SIGNUP_TICKET_INVALID));

        if (!ticket.isValid()) {
            throw new BadRequestException(AuthConstants.ERR_SIGNUP_TOKEN_EXPIRED_OR_USED);
        }

        String normalizedUsername = request.getUsername().trim().toLowerCase(java.util.Locale.ROOT);

        // Email-targeted registration rate limiting (Scope C anti-enumeration)
        if (rateLimitProperties.isEnabled()) {
            RateLimitProperties.CategoryLimit emailLimitConfig = rateLimitProperties
                    .getCategoryLimit("auth-register-email");
            int emailLimit = emailLimitConfig.getLimit() > 0 ? emailLimitConfig.getLimit() : 5;
            int windowSeconds = emailLimitConfig.getWindowSeconds() > 0 ? emailLimitConfig.getWindowSeconds() : 60;

            String emailRateLimitKey = String.format("rate_limit:auth-register:email:%s", normalizedEmail);
            com.instagramclone.ratelimit.RateLimitResult rateLimitResult = rateLimiterService
                    .checkRateLimit(emailRateLimitKey, emailLimit, windowSeconds);

            if (!rateLimitResult.isAllowed()) {
                log.warn(
                        "Email-targeted registration rate limit exceeded for identifier [{}] (limit: {}/{}s, retry-after: {}s)",
                        normalizedEmail, emailLimit, windowSeconds, rateLimitResult.getRetryAfterSeconds());
                throw new RateLimitExceededException(
                        String.format("Too many registration attempts. Please try again in %d seconds.",
                                rateLimitResult.getRetryAfterSeconds()),
                        rateLimitResult.getRetryAfterSeconds(),
                        rateLimitResult.getLimit());
            }
        }

        // Validate username uniqueness (if taken, throw 409 Conflict WITHOUT consuming
        // ticket)
        Optional<User> existingByUsername = userRepository.findByUsername(normalizedUsername);
        if (existingByUsername.isPresent()) {
            throw new ConflictException(String.format(AuthConstants.ERR_USERNAME_TAKEN_FORMAT, normalizedUsername));
        }

        // Multi-Account Policy: Enforce maximum 3 accounts per normalized email
        List<Integer> usedSlots = userRepository.findAccountSlotsByEmail(normalizedEmail);
        if (usedSlots.size() >= 3) {
            String presentedPassword = (request.getPassword() != null)
                    ? request.getPassword()
                    : "InstagramCloneTimingMitigationPassword!";
            passwordEncoder.encode(presentedPassword);

            throw new ConflictException(AuthConstants.ERR_MAX_ACCOUNTS_REACHED);
        }

        int nextSlot = java.util.stream.IntStream.rangeClosed(1, 3)
                .filter(s -> !usedSlots.contains(s))
                .findFirst()
                .orElseThrow(() -> new ConflictException(AuthConstants.ERR_MAX_ACCOUNTS_REACHED));

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName().trim()
                : normalizedUsername;

        // Atomically create user with emailVerified = true and emailVerifiedAt
        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .accountSlot(nextSlot)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        Profile profile = Profile.builder()
                .user(user)
                .displayName(displayName)
                .build();

        user.setProfile(profile);

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("Concurrent registration conflict for email [{}] or username [{}]: {}",
                    maskEmail(normalizedEmail), normalizedUsername, dive.getMessage());
            if (userRepository.findByUsername(normalizedUsername).isPresent()) {
                throw new ConflictException(String.format(AuthConstants.ERR_USERNAME_TAKEN_FORMAT, normalizedUsername));
            }
            throw new ConflictException(AuthConstants.ERR_MAX_ACCOUNTS_REACHED);
        }

        // Consume the signup proof
        ticket.setConsumedAt(LocalDateTime.now());
        signupVerificationTicketRepository.save(ticket);

        // Create and persist refresh token with stable session identity and client metadata
        UUID sessionId = UUID.randomUUID();
        ResolvedClientMetadata clientMetadata = resolveClientMetadata();

        String accessToken = jwtTokenProvider.generateTokenForUser(savedUser.getId(), savedUser.getUsername(),
                savedUser.getEmail(), sessionId);
        String rawRefreshToken = generateRawToken();
        String refreshTokenHash = hashToken(rawRefreshToken);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plus(jwtProperties.getRefreshExpirationMs(),
                ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(savedUser)
                .sessionId(sessionId)
                .tokenHash(refreshTokenHash)
                .deviceName(clientMetadata.deviceName())
                .ipAddress(clientMetadata.ipAddress())
                .userAgent(clientMetadata.userAgent())
                .expiresAt(refreshExpiresAt)
                .lastActivityAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        // Post-commit welcome email
        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ACCOUNT_WELCOME")
                .emailType(EmailType.WELCOME)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .variables(java.util.Map.of("username", savedUser.getUsername()))
                .timestamp(LocalDateTime.now())
                .build());

        auditLogService.record(savedUser.getId(), "USER_REGISTERED", "USER", savedUser.getId(), null,
                "Registered username: " + savedUser.getUsername());

        log.info("Successfully registered and authenticated new user ID: {} with username: {}", savedUser.getId(),
                savedUser.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .user(userMapper.toUserResponse(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail().trim().toLowerCase();

        // Scope B: Account-targeted login velocity throttling.
        if (rateLimitProperties.isEnabled()) {
            RateLimitProperties.CategoryLimit accountLimitConfig = rateLimitProperties
                    .getCategoryLimit("auth-login-account");
            int accountLimit = accountLimitConfig.getLimit() > 0 ? accountLimitConfig.getLimit() : 10;
            int windowSeconds = accountLimitConfig.getWindowSeconds() > 0 ? accountLimitConfig.getWindowSeconds() : 60;

            String accountRateLimitKey = String.format("rate_limit:auth-login:account:%s", usernameOrEmail);
            com.instagramclone.ratelimit.RateLimitResult rateLimitResult = rateLimiterService
                    .checkRateLimit(accountRateLimitKey, accountLimit, windowSeconds);

            if (!rateLimitResult.isAllowed()) {
                log.warn(
                        "Account-targeted login rate limit exceeded for identifier [{}] (limit: {}/{}s, retry-after: {}s)",
                        usernameOrEmail, accountLimit, windowSeconds, rateLimitResult.getRetryAfterSeconds());
                throw new RateLimitExceededException(
                        String.format("Too many login attempts for this account. Please try again in %d seconds.",
                                rateLimitResult.getRetryAfterSeconds()),
                        rateLimitResult.getRetryAfterSeconds(),
                        rateLimitResult.getLimit());
            }
        }

        // Reject login immediately if the account is suspended
        if (!usernameOrEmail.contains("@")) {
            userRepository.findByUsername(usernameOrEmail).ifPresent(candidate -> {
                if (candidate.getStatus() == com.instagramclone.user.enums.UserStatus.SUSPENDED) {
                    log.warn("Login attempt blocked for suspended account username: {}", candidate.getUsername());
                    throw new com.instagramclone.common.exception.ApiException(
                            com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_SUSPENDED,
                            org.springframework.http.HttpStatus.FORBIDDEN,
                            "ACCOUNT_SUSPENDED"
                    );
                }
            });
        } else {
            java.util.List<User> emailUsers = userRepository.findAllByEmail(usernameOrEmail);
            if (emailUsers.stream().anyMatch(u -> u.getStatus() == com.instagramclone.user.enums.UserStatus.SUSPENDED)) {
                log.warn("Login attempt blocked for suspended account email: {}", maskEmail(usernameOrEmail));
                throw new com.instagramclone.common.exception.ApiException(
                        com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_SUSPENDED,
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "ACCOUNT_SUSPENDED"
                );
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, request.getPassword()));
        } catch (BadCredentialsException ex) {
            accountLockoutService.handleFailedLogin(usernameOrEmail);
            throw ex;
        } catch (LockedException ex) {
            accountLockoutService.handleFailedLogin(usernameOrEmail);
            throw ex;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(AuthConstants.ERR_USER_NOT_FOUND_ID + principal.getId()));

        if (user.getStatus() == com.instagramclone.user.enums.UserStatus.SUSPENDED) {
            log.warn("Login attempt blocked for suspended account user ID: {}", user.getId());
            throw new com.instagramclone.common.exception.ApiException(
                    com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_SUSPENDED,
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_SUSPENDED"
            );
        }

        // Reactivate soft-deleted user account if logging in within 15-day recovery window
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            if (user.isWithinDeletionGracePeriod(com.instagramclone.user.constants.UserConstants.ACCOUNT_DELETION_GRACE_PERIOD_DAYS)) {
                user.restore();
                user = userRepository.save(user);
                tokenBlacklistService.clearUserBlacklist(user.getId());
                auditLogService.record(user.getId(), "ACCOUNT_REACTIVATED", "USER", user.getId(), null,
                        "Account reactivated upon successful login within 15 days grace period");
                log.info("User {} successfully reactivated deleted account upon login within 15 days recovery window", user.getUsername());
            } else {
                log.warn("Login rejected for permanently deleted account ID: {}", user.getId());
                throw new ApiException(com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_PERMANENTLY_DELETED,
                        HttpStatus.FORBIDDEN, "ACCOUNT_PERMANENTLY_DELETED");
            }
        } else if (Boolean.FALSE.equals(user.getIsActive()) || user.getStatus() == com.instagramclone.user.enums.UserStatus.DEACTIVATED) {
            user.setIsActive(true);
            user.setStatus(com.instagramclone.user.enums.UserStatus.ACTIVE);
            user = userRepository.save(user);
            tokenBlacklistService.clearUserBlacklist(user.getId());
            auditLogService.record(user.getId(), "ACCOUNT_REACTIVATED", "USER", user.getId(), null,
                    "Deactivated account reactivated upon successful login");
            log.info("User {} successfully reactivated deactivated account upon login", user.getUsername());
        }

        // Phase 4: Enforce email verification gate
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("Login rejected for unverified account ID: {} ({})", user.getId(), maskEmail(user.getEmail()));
            throw new ApiException(AuthConstants.ERR_VERIFY_EMAIL_BEFORE_LOGIN, HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED");
        }

        accountLockoutService.handleSuccessfulLogin(user);

        // Privacy-conscious device tracking & security inspection using unified trusted IP resolution
        HttpServletRequest currentReq = null;
        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs instanceof ServletRequestAttributes sra) {
            currentReq = sra.getRequest();
        }
        String rawUserAgent = currentReq != null ? currentReq.getHeader("User-Agent") : null;
        String userAgent = (rawUserAgent != null && rawUserAgent.length() > 500)
                ? rawUserAgent.substring(0, 500)
                : rawUserAgent;
        String remoteIp = clientIpResolver.resolveClientIp(currentReq);

        IDeviceDetectionService.DeviceInspectionResult deviceResult = deviceDetectionService.inspectDevice(user,
                userAgent, remoteIp);

        UUID sessionId = UUID.randomUUID();
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, sessionId);

        String rawRefreshToken = generateRawToken();
        String tokenHash = hashToken(rawRefreshToken);

        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProperties.getRefreshExpirationMs(), ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(tokenHash)
                .deviceName(deviceResult.deviceName())
                .ipAddress(remoteIp)
                .userAgent(userAgent)
                .expiresAt(expiresAt)
                .lastActivityAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("User {} logged in successfully. Issued session tokens for session {}.", user.getUsername(), sessionId);
        auditLogService.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), null,
                "User logged in: " + user.getUsername());

        EmailType loginEmailType = deviceResult.newDeviceAlertJustified()
                ? EmailType.NEW_DEVICE_LOGIN
                : EmailType.NEW_LOGIN;

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(loginEmailType == EmailType.NEW_DEVICE_LOGIN
                        ? "NEW_DEVICE_LOGIN_SECURITY_ALERT"
                        : "NEW_LOGIN_NOTIFICATION")
                .emailType(loginEmailType)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(java.util.Map.of(
                        "username", user.getUsername(),
                        "deviceName", deviceResult.deviceName(),
                        "loginTime", LocalDateTime.now().toString(),
                        "ipAddress", deviceResult.ipAddress()))
                .timestamp(LocalDateTime.now())
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional(noRollbackFor = { UnauthorizedException.class, ApiException.class })
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken().trim();
        String tokenHash = hashToken(rawToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException(AuthConstants.ERR_INVALID_REFRESH_TOKEN));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            log.warn("Attempt to use invalid or revoked refresh token for user ID: {}", storedToken.getUser().getId());
            throw new UnauthorizedException(AuthConstants.ERR_REFRESH_TOKEN_EXPIRED_OR_REVOKED);
        }

        User user = storedToken.getUser();

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("Refresh token rejected and revoked for unverified user ID: {}", user.getId());
            storedToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.saveAndFlush(storedToken);
            throw new UnauthorizedException(AuthConstants.ERR_EMAIL_NOT_VERIFIED);
        }

        // Concurrency-safe atomic rotation: mark old token as revoked conditionally
        LocalDateTime now = LocalDateTime.now();
        int updated = refreshTokenRepository.markRevokedIfActive(storedToken.getId(), now);
        if (updated == 0) {
            log.warn("Concurrent refresh race detected for token ID: {}", storedToken.getId());
            throw new UnauthorizedException(AuthConstants.ERR_REFRESH_TOKEN_EXPIRED_OR_REVOKED);
        }

        // Retain stable session ID
        UUID sessionId = storedToken.getSessionId() != null ? storedToken.getSessionId() : UUID.randomUUID();

        // Enforce session-level revocation check
        if (tokenBlacklistService.isSessionRevoked(sessionId)) {
            log.warn("Refresh rejected: session [{}] has been revoked", sessionId);
            throw new UnauthorizedException(AuthConstants.ERR_SESSION_REVOKED);
        }

        ResolvedClientMetadata clientMetadata = resolveClientMetadata();
        String deviceName = (clientMetadata.deviceName() != null && !clientMetadata.deviceName().equals("Web Browser (Direct Client)"))
                ? clientMetadata.deviceName()
                : (storedToken.getDeviceName() != null ? storedToken.getDeviceName() : clientMetadata.deviceName());
        String ipAddress = clientMetadata.ipAddress() != null ? clientMetadata.ipAddress() : storedToken.getIpAddress();
        String userAgent = clientMetadata.userAgent() != null ? clientMetadata.userAgent() : storedToken.getUserAgent();

        String newAccessToken = jwtTokenProvider.generateTokenForUser(user.getId(), user.getUsername(),
                user.getEmail(), sessionId);

        String newRawRefreshToken = generateRawToken();
        String newTokenHash = hashToken(newRawRefreshToken);
        LocalDateTime expiresAt = now.plus(jwtProperties.getRefreshExpirationMs(), ChronoUnit.MILLIS);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(newTokenHash)
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(expiresAt)
                .lastActivityAt(now)
                .build();

        refreshTokenRepository.save(newRefreshToken);
        log.info("Successfully rotated refresh token for user ID: {} retaining session ID: {}", user.getId(), sessionId);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        logout(refreshToken, null);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        LocalDateTime now = LocalDateTime.now();
        UUID currentUserId = null;
        UUID currentSessionId = null;

        // 1. Identify authenticated user and session
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            UserPrincipal principal = principalOpt.get();
            currentUserId = principal.getId();
            currentSessionId = principal.getSessionId();
        } else if (accessToken != null && !accessToken.isBlank() && jwtTokenProvider.validateToken(accessToken)) {
            try {
                currentUserId = jwtTokenProvider.getUserIdFromToken(accessToken);
                currentSessionId = jwtTokenProvider.getSessionIdFromToken(accessToken);
            } catch (Exception ignored) {}
        }

        // 2. Blacklist presented access token if supplied
        if (accessToken != null && !accessToken.isBlank()) {
            long remainingTtl = jwtTokenProvider.getRemainingTtlMs(accessToken);
            tokenBlacklistService.blacklistToken(accessToken, remainingTtl);
            log.info("Blacklisted JWT access token on logout with remaining TTL: {}ms", remainingTtl);
        }

        // 3. If refreshToken is passed, lookup and revoke it, and obtain its sessionId if missing
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = hashToken(refreshToken.trim());
            var tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
            if (tokenOpt.isPresent()) {
                RefreshToken token = tokenOpt.get();
                // Anti-cross-user check: only revoke if belongs to calling user or caller is unauthenticated
                if (currentUserId == null || token.getUser().getId().equals(currentUserId)) {
                    if (currentSessionId == null) {
                        currentSessionId = token.getSessionId();
                    }
                    if (currentUserId == null) {
                        currentUserId = token.getUser().getId();
                    }
                    token.setRevokedAt(now);
                    refreshTokenRepository.save(token);
                    log.info("Revoked refresh token on logout for user ID: {}", token.getUser().getId());
                } else {
                    log.warn("Cross-user logout attempt: authenticated user {} attempted to revoke token for user {}",
                            currentUserId, token.getUser().getId());
                }
            }
        }

        // 4. Revoke active tokens for this specific session and blacklist session in Redis
        if (currentSessionId != null) {
            refreshTokenRepository.revokeAllTokensBySessionId(currentSessionId, now);
            tokenBlacklistService.blacklistSession(currentSessionId, jwtProperties.getAccessExpirationMs());
            log.info("Revoked session [{}] on single logout", currentSessionId);
        }

        if (currentUserId != null) {
            auditLogService.record(currentUserId, "LOGOUT", "USER", currentUserId, null, "User logged out of session");
        }
    }

    @Override
    @Transactional
    public void logoutAll() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.revokeAllUserTokens(currentUserId, now);
        tokenBlacklistService.blacklistUser(currentUserId, jwtProperties.getAccessExpirationMs());

        log.info("Revoked all active sessions and blacklisted user ID: {} on global logout", currentUserId);
        auditLogService.record(currentUserId, "LOGOUT_ALL", "USER", currentUserId, null, "User logged out of all devices");
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(AuthConstants.ERR_INVALID_VERIFICATION_TOKEN));

        if (verificationToken.isUsed()) {
            throw new BadRequestException(AuthConstants.ERR_VERIFICATION_TOKEN_USED);
        }

        if (verificationToken.isExpired()) {
            throw new BadRequestException(AuthConstants.ERR_VERIFICATION_TOKEN_EXPIRED);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setUsedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified successfully for user ID: {} ({})", user.getId(), maskEmail(user.getEmail()));
        auditLogService.record(user.getId(), "EMAIL_VERIFIED", "USER", user.getId(), null,
                "Email verified successfully for user: " + user.getUsername());

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ACCOUNT_WELCOME")
                .emailType(EmailType.WELCOME)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(java.util.Map.of("username", user.getUsername()))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        List<User> unverifiedUsers = userRepository.findUnverifiedByEmail(normalizedEmail);

        if (unverifiedUsers.isEmpty()) {
            log.debug("Verification resend requested for nonexistent or already verified email: {}", maskEmail(normalizedEmail));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (User user : unverifiedUsers) {
            emailVerificationTokenRepository.invalidateAllUserTokens(user.getId(), now);

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);
            LocalDateTime expiresAt = now.plus(mailProperties.getVerificationTokenValidityMinutes(),
                    ChronoUnit.MINUTES);

            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            emailVerificationTokenRepository.save(verificationToken);

            emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(KafkaConstants.EVENT_EMAIL_VERIFICATION_REQUESTED)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .rawToken(rawToken)
                    .timestamp(now)
                    .build());

            log.info("Issued new email verification token for user ID: {}", user.getId());
            auditLogService.record(user.getId(), "EMAIL_VERIFICATION_RESENT", "USER", user.getId(), null,
                    "Verification email resent for user: " + user.getUsername());
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String input = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        List<User> targetUsers;
        if (!input.contains("@")) {
            targetUsers = userRepository.findByUsername(input).stream().toList();
        } else {
            targetUsers = userRepository.findAllByEmail(input);
        }

        if (targetUsers.isEmpty()) {
            log.debug("Password reset requested for nonexistent identifier: {}", maskEmail(input));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(mailProperties.getResetTokenValidityMinutes(), ChronoUnit.MINUTES);

        if (targetUsers.size() == 1) {
            User user = targetUsers.get(0);
            passwordResetTokenRepository.invalidateAllUserTokens(user.getId(), now);

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(KafkaConstants.EVENT_PASSWORD_RESET_REQUESTED)
                    .emailType(EmailType.PASSWORD_RESET)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .rawToken(rawToken)
                    .variables(java.util.Map.of(
                            "username", user.getUsername(),
                            "resetUrl", mailProperties.getResetUrl() + "?token=" + rawToken,
                            "validityMinutes", mailProperties.getResetTokenValidityMinutes()))
                    .timestamp(now)
                    .build());

            System.out.println("\n========================================================");
            System.out.println("  >>> [PASSWORD RESET URL] User: " + user.getUsername() + " | URL: " + mailProperties.getResetUrl() + "?token=" + rawToken + " <<<");
            System.out.println("========================================================\n");
            log.info("Issued password reset token for user ID: {}", user.getId());
            auditLogService.record(user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(), null,
                    "Password reset requested for user: " + user.getUsername());
        } else {
            // Multi-account email: issue distinct tokens for each account and send consolidated email
            List<java.util.Map<String, String>> accountLinks = new java.util.ArrayList<>();
            for (User user : targetUsers) {
                passwordResetTokenRepository.invalidateAllUserTokens(user.getId(), now);

                String rawToken = generateSecureToken();
                String tokenHash = hashToken(rawToken);

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(expiresAt)
                        .build();
                passwordResetTokenRepository.save(resetToken);

                accountLinks.add(java.util.Map.of(
                        "username", user.getUsername(),
                        "resetUrl", mailProperties.getResetUrl() + "?token=" + rawToken
                ));

                log.info("Issued account-bound password reset token for user ID: {} (@{})", user.getId(), user.getUsername());
                auditLogService.record(user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(), null,
                        "Password reset requested for user: " + user.getUsername());
            }

            String targetEmail = targetUsers.get(0).getEmail();
            emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(KafkaConstants.EVENT_PASSWORD_RESET_REQUESTED)
                    .emailType(EmailType.PASSWORD_RESET)
                    .email(targetEmail)
                    .username(targetUsers.get(0).getUsername())
                    .variables(java.util.Map.of(
                            "accounts", accountLinks,
                            "validityMinutes", mailProperties.getResetTokenValidityMinutes()))
                    .timestamp(now)
                    .build());

            log.info("Dispatched consolidated multi-account password reset email to [{}] for {} accounts",
                    maskEmail(targetEmail), targetUsers.size());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(AuthConstants.ERR_INVALID_PASSWORD_RESET_TOKEN));

        if (resetToken.isUsed()) {
            throw new BadRequestException(AuthConstants.ERR_PASSWORD_RESET_TOKEN_USED);
        }

        if (resetToken.isExpired()) {
            throw new BadRequestException(AuthConstants.ERR_PASSWORD_RESET_TOKEN_EXPIRED);
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 8) {
            throw new BadRequestException(AuthConstants.ERR_PASSWORD_MIN_LENGTH);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountLockoutService.unlockAccount(user.getId());
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllUserTokens(user.getId(), LocalDateTime.now());
        tokenBlacklistService.blacklistUser(user.getId(), jwtProperties.getAccessExpirationMs());
        log.info("Revoked active sessions and blacklisted tokens for user ID: {} following password reset",
                user.getId());

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaConstants.EVENT_PASSWORD_CHANGED_SECURITY_ALERT)
                .emailType(EmailType.PASSWORD_CHANGED)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(java.util.Map.of("username", user.getUsername()))
                .timestamp(LocalDateTime.now())
                .build());

        auditLogService.record(user.getId(), "PASSWORD_RESET_COMPLETED", "USER", user.getId(), null,
                "Password successfully reset for user: " + user.getUsername());
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();

        // Verify OTP atomically through IEmailOtpService for legacy ACCOUNT_VERIFICATION
        OtpVerificationResult result = emailOtpService.verifyOtp(normalizedEmail, code,
                OtpPurpose.ACCOUNT_VERIFICATION);

        if (result.getStatus() == OtpVerificationStatus.INVALID_CODE) {
            throw new BadRequestException(String.format("Invalid verification code. %d attempt(s) remaining.",
                    result.getRemainingAttempts()));
        }

        if (result.getStatus() == OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED) {
            throw new BadRequestException(
                    "Maximum verification attempts exceeded. Please request a new verification code.");
        }

        if (result.getStatus() == OtpVerificationStatus.EXPIRED_OR_NOT_FOUND) {
            throw new BadRequestException(
                    "Verification code has expired or does not exist. Please request a new code.");
        }

        User user;
        if (result.getUserId() != null) {
            user = userRepository.findById(result.getUserId())
                    .orElseThrow(() -> new BadRequestException(AuthConstants.ERR_NO_ACCOUNT_FOR_CHALLENGE));
        } else {
            List<User> unverified = userRepository.findUnverifiedByEmail(normalizedEmail);
            if (unverified.isEmpty()) {
                throw new BadRequestException(AuthConstants.ERR_NO_UNVERIFIED_ACCOUNT);
            }
            if (unverified.size() > 1) {
                log.warn("Multiple unverified accounts found for email [{}]. Ambiguous legacy verification rejected.", maskEmail(normalizedEmail));
                throw new BadRequestException(AuthConstants.ERR_MULTIPLE_UNVERIFIED_ACCOUNTS);
            }
            user = unverified.get(0);
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException(AuthConstants.ERR_ACCOUNT_ALREADY_VERIFIED);
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        emailVerificationTokenRepository.invalidateAllUserTokens(user.getId(), LocalDateTime.now());

        log.info("Email verified successfully via OTP for user ID: {} ({})", user.getId(), maskEmail(user.getEmail()));
        auditLogService.record(user.getId(), "EMAIL_VERIFIED_OTP", "USER", user.getId(), null,
                "Email verified via OTP for user: " + user.getUsername());

        emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ACCOUNT_WELCOME")
                .emailType(EmailType.WELCOME)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .variables(java.util.Map.of("username", user.getUsername()))
                .timestamp(LocalDateTime.now())
                .build());

        UUID sessionId = UUID.randomUUID();
        ResolvedClientMetadata clientMetadata = resolveClientMetadata();

        String accessToken = jwtTokenProvider.generateTokenForUser(user.getId(), user.getUsername(), user.getEmail(), sessionId);
        String rawRefreshToken = generateRawToken();
        String tokenHash = hashToken(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProperties.getRefreshExpirationMs(), ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(tokenHash)
                .deviceName(clientMetadata.deviceName())
                .ipAddress(clientMetadata.ipAddress())
                .userAgent(clientMetadata.userAgent())
                .expiresAt(expiresAt)
                .lastActivityAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        List<User> unverified = userRepository.findUnverifiedByEmail(normalizedEmail);

        if (unverified.isEmpty()) {
            log.debug("OTP resend requested for nonexistent or already verified email: {}", maskEmail(normalizedEmail));
            return;
        }

        if (unverified.size() > 1) {
            log.warn("Multiple unverified accounts found for email [{}]. Cannot resolve single account for OTP resend.", maskEmail(normalizedEmail));
            return;
        }

        User user = unverified.get(0);

        // Create new challenge in Redis for legacy ACCOUNT_VERIFICATION
        OtpChallengeResult challenge = emailOtpService.createOtpChallenge(normalizedEmail, user.getId(),
                OtpPurpose.ACCOUNT_VERIFICATION);

        EmailDeliveryLog deliveryLog = mailService.sendOtpEmail(user.getEmail(), user.getUsername(),
                challenge.getRawOtp());

        if (deliveryLog == null || deliveryLog.getStatus() == EmailDeliveryStatus.FAILED) {
            log.error("Synchronous OTP email resend delivery failed for user [{}]. Invalidating challenge.",
                    user.getUsername());
            emailOtpService.invalidateChallenge(normalizedEmail, OtpPurpose.ACCOUNT_VERIFICATION);
            throw new ApiException("Failed to dispatch verification code. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR, "MAIL_DELIVERY_FAILED");
        }

        auditLogService.record(user.getId(), "OTP_RESENT", "USER", user.getId(), null,
                "OTP verification code resent for user: " + user.getUsername());
        log.info("Successfully resent OTP verification code to [{}] for user: {}", maskEmail(user.getEmail()),
                user.getUsername());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    private String generateRawToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in runtime", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private record ResolvedClientMetadata(String userAgent, String ipAddress, String deviceName) {}

    private ResolvedClientMetadata resolveClientMetadata() {
        HttpServletRequest currentReq = null;
        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs instanceof ServletRequestAttributes sra) {
            currentReq = sra.getRequest();
        }
        String rawUserAgent = currentReq != null ? currentReq.getHeader("User-Agent") : null;
        String userAgent = (rawUserAgent != null && rawUserAgent.length() > 500)
                ? rawUserAgent.substring(0, 500)
                : rawUserAgent;
        String remoteIp = clientIpResolver.resolveClientIp(currentReq);
        String deviceName = deviceDetectionService.resolveCoarseDeviceName(userAgent);
        return new ResolvedClientMetadata(userAgent, remoteIp, deviceName);
    }

    @Override
    public UsernameCheckResponse checkUsername(String username) {
        return usernameSuggestionService.checkAndSuggest(username);
    }
}
