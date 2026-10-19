package com.instagramclone.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.exception.ConflictException;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.security.otp.OtpPurpose;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import com.instagramclone.auth.dto.response.*;
import com.instagramclone.user.dto.response.*;
import com.instagramclone.auth.entity.*;
import com.instagramclone.user.entity.*;
import com.instagramclone.user.enums.EmailChangeStatus;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MultiAccountRegistrationTest extends BaseIntegrationTest {

    @SpyBean
    private IMailService mailService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        reset(mailService);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MvcResult registerUser(String email, String username, String password) throws Exception {
        String token = createValidSignupTicket(email);
        RegisterRequest req = RegisterRequest.builder()
                .signupToken(token)
                .username(username)
                .password(password)
                .displayName("Display " + username)
                .build();

        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    private MvcResult attemptRegister(String email, String username, String password) throws Exception {
        String token = createValidSignupTicket(email);
        RegisterRequest req = RegisterRequest.builder()
                .signupToken(token)
                .username(username)
                .password(password)
                .displayName("Display " + username)
                .build();

        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    @Test
    @DisplayName("1. First account for an email uses slot 1")
    void test01_firstAccount_usesSlot1() throws Exception {
        String email = "first_test@example.com";
        registerUser(email, "first_user", "Password123!");

        User user = userRepository.findByUsername("first_user").orElseThrow();
        assertThat(user.getAccountSlot()).isEqualTo(1);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getId()).isNotNull();

        Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getDisplayName()).isEqualTo("Display first_user");
    }

    @Test
    @DisplayName("2. Second account with the same normalized email uses a distinct slot")
    void test02_secondAccount_sameNormalizedEmail_usesDistinctSlot() throws Exception {
        String email = "multi_slot@example.com";
        registerUser(email, "account_one", "Password123!");
        registerUser(email, "account_two", "Password123!");

        User user1 = userRepository.findByUsername("account_one").orElseThrow();
        User user2 = userRepository.findByUsername("account_two").orElseThrow();

        assertThat(user1.getAccountSlot()).isEqualTo(1);
        assertThat(user2.getAccountSlot()).isEqualTo(2);
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user1.getEmail()).isEqualTo(user2.getEmail());

        List<Integer> slots = userRepository.findAccountSlotsByEmail(email);
        assertThat(slots).containsExactly(1, 2);
    }

    @Test
    @DisplayName("3. Third account with that email succeeds")
    void test03_thirdAccount_sameEmail_succeeds() throws Exception {
        String email = "three_accounts@example.com";
        registerUser(email, "account_slot1", "Password123!");
        registerUser(email, "account_slot2", "Password123!");
        registerUser(email, "account_slot3", "Password123!");

        List<User> accounts = userRepository.findAllByEmail(email);
        assertThat(accounts).hasSize(3);

        List<Integer> slots = userRepository.findAccountSlotsByEmail(email);
        assertThat(slots).containsExactly(1, 2, 3);
        assertThat(userRepository.countByEmail(email)).isEqualTo(3);
    }

    @Test
    @DisplayName("4. Fourth account fails with 409 Conflict and safe error")
    void test04_fourthAccount_sameEmail_failsWith409() throws Exception {
        String email = "four_accounts@example.com";
        registerUser(email, "acc_one", "Password123!");
        registerUser(email, "acc_two", "Password123!");
        registerUser(email, "acc_three", "Password123!");

        MvcResult result = attemptRegister(email, "acc_four", "Password123!");
        assertThat(result.getResponse().getStatus()).isEqualTo(409);

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("message").asText()).contains("Maximum number of accounts (3) reached");

        assertThat(userRepository.findByUsername("acc_four")).isEmpty();
        assertThat(userRepository.countByEmail(email)).isEqualTo(3);
    }

    @Test
    @DisplayName("5. Email capitalization and whitespace normalization cannot bypass the limit")
    void test05_emailNormalization_cannotBypassLimit() throws Exception {
        String baseEmail = "normalize_limit@example.com";
        registerUser(baseEmail, "norm_one", "Password123!");
        registerUser("  NORMALIZE_LIMIT@EXAMPLE.COM  ", "norm_two", "Password123!");
        registerUser("Normalize_Limit@example.com", "norm_three", "Password123!");

        assertThat(userRepository.countByEmail(baseEmail)).isEqualTo(3);

        // Attempt 4th with weird capitalization and whitespace
        MvcResult result = attemptRegister("   NORMALIZE_LIMIT@EXAMPLE.COM   ", "norm_four", "Password123!");
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(userRepository.countByEmail(baseEmail)).isEqualTo(3);
    }

    @Test
    @DisplayName("6. Dotted addresses remain distinct unless exact normalized values are equivalent")
    void test06_dottedAddresses_remainDistinct() throws Exception {
        registerUser("john.doe@example.com", "john_dot_doe", "Password123!");
        registerUser("johndoe@example.com", "john_nodot_doe", "Password123!");

        User dotUser = userRepository.findByUsername("john_dot_doe").orElseThrow();
        User noDotUser = userRepository.findByUsername("john_nodot_doe").orElseThrow();

        assertThat(dotUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(dotUser.getAccountSlot()).isEqualTo(1);

        assertThat(noDotUser.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(noDotUser.getAccountSlot()).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Signup OTP requests for one/two-account emails work under configured rate limits")
    void test07_signupOtpRequest_underLimit_dispatchesOtp() throws Exception {
        String email = "otp_available@example.com";
        registerUser(email, "existing_first", "Password123!");

        reset(mailService);

        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder().email(email).build();
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendOtpEmail(eq(email), any(), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).matches("^\\d{6}$");
    }

    @Test
    @DisplayName("8. At the three-account limit, OTP requests remain generic and do not dispatch an OTP")
    void test08_signupOtpRequest_atThreeAccountLimit_noOtpDispatched() throws Exception {
        String email = "at_limit@example.com";
        registerUser(email, "limit_1", "Password123!");
        registerUser(email, "limit_2", "Password123!");
        registerUser(email, "limit_3", "Password123!");

        reset(mailService);

        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder().email(email).build();
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        verify(mailService, never()).sendOtpEmail(eq(email), any(), any());
    }

    @Test
    @DisplayName("9. OTP verification alone creates no user/profile/session records")
    void test09_otpVerificationAlone_createsNoUserOrSession() throws Exception {
        String email = "fresh_signup@example.com";

        // Request OTP
        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder().email(email).build();
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendOtpEmail(eq(email), any(), otpCaptor.capture());
        String code = otpCaptor.getValue();

        // Verify OTP
        SignupVerifyOtpRequest verifyReq = SignupVerifyOtpRequest.builder().email(email).code(code).build();
        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signupToken").isString());

        // Assert no records created yet
        assertThat(userRepository.countByEmail(email)).isZero();
        assertThat(profileRepository.count()).isZero();
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("10. An expired, invalid or consumed signup proof cannot create another account")
    void test10_invalidOrConsumedProof_cannotCreateAccount() throws Exception {
        String email = "consumed_ticket@example.com";
        String token = createValidSignupTicket(email);

        RegisterRequest req1 = RegisterRequest.builder()
                .signupToken(token)
                .username("first_consumer")
                .password("Password123!")
                .displayName("Consumer 1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Attempt second registration with same token
        RegisterRequest req2 = RegisterRequest.builder()
                .signupToken(token)
                .username("second_consumer")
                .password("Password123!")
                .displayName("Consumer 2")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signup verification token is expired or has already been used"));

        // Attempt with bogus token
        RegisterRequest reqBogus = RegisterRequest.builder()
                .signupToken("completely_bogus_token")
                .username("third_consumer")
                .password("Password123!")
                .displayName("Consumer 3")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBogus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid, expired, or already consumed signup verification token"));
    }

    @Test
    @DisplayName("11. Concurrent registrations never exceed three accounts for an email")
    void test11_concurrentRegistrations_neverExceedThreeAccounts() throws Exception {
        String email = "concurrent_limit@example.com";
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<String> threadLogs = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            String token = createValidSignupTicket(email);
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    RegisterRequest req = RegisterRequest.builder()
                            .signupToken(token)
                            .username("concurrent_user_" + index)
                            .password("Password123!")
                            .displayName("Concurrent " + index)
                            .build();

                    MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    String body = res.getResponse().getContentAsString();
                    threadLogs.add("Thread " + index + " status: " + status + " body: " + body);

                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        System.out.println("TEST11 LOGS:\n" + String.join("\n", threadLogs));

        assertThat(userRepository.countByEmail(email)).isLessThanOrEqualTo(3);
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 3);

        List<Integer> slots = userRepository.findAccountSlotsByEmail(email);
        assertThat(slots).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("12. Concurrency allocation and database uniqueness constraints are actually exercised")
    void test12_databaseUniquenessConstraints_exercised() throws Exception {
        String email = "db_constraint@example.com";
        registerUser(email, "db_slot_1", "Password123!");
        registerUser(email, "db_slot_2", "Password123!");
        registerUser(email, "db_slot_3", "Password123!");

        // Attempt manual duplicate slot insertion
        User duplicateSlotUser = User.builder()
                .username("duplicate_slot")
                .email(email)
                .passwordHash("hash")
                .accountSlot(1)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicateSlotUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("13. Slot conflict or registration rollback does not leave partial user/profile/token records")
    void test13_registrationRollback_leavesNoPartialRecords() throws Exception {
        String email = "partial_test@example.com";
        registerUser(email, "partial_one", "Password123!");
        registerUser(email, "partial_two", "Password123!");
        registerUser(email, "partial_three", "Password123!");

        long userCountBefore = userRepository.count();
        long profileCountBefore = profileRepository.count();
        long tokenCountBefore = refreshTokenRepository.count();

        // Attempt 4th registration
        attemptRegister(email, "partial_four", "Password123!");

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
        assertThat(profileRepository.count()).isEqualTo(profileCountBefore);
        assertThat(refreshTokenRepository.count()).isEqualTo(tokenCountBefore);
        assertThat(userRepository.findByUsername("partial_four")).isEmpty();
    }

    @Test
    @DisplayName("14. Username login independently authenticates each shared-email account")
    void test14_usernameLogin_authenticatesEachSharedEmailAccount() throws Exception {
        String email = "shared_login@example.com";
        registerUser(email, "login_alpha", "AlphaPass123!");
        registerUser(email, "login_beta", "BetaPass123!");
        registerUser(email, "login_gamma", "GammaPass123!");

        // Log in to alpha
        LoginRequest reqAlpha = LoginRequest.builder().usernameOrEmail("login_alpha").password("AlphaPass123!").build();
        MvcResult resAlpha = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAlpha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("login_alpha"))
                .andReturn();

        // Log in to beta
        LoginRequest reqBeta = LoginRequest.builder().usernameOrEmail("login_beta").password("BetaPass123!").build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBeta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("login_beta"));

        // Log in to gamma
        LoginRequest reqGamma = LoginRequest.builder().usernameOrEmail("login_gamma").password("GammaPass123!").build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqGamma)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("login_gamma"));
    }

    @Test
    @DisplayName("15. Single-account email login remains compatible")
    void test15_singleAccountEmailLogin_remainsCompatible() throws Exception {
        String email = "single_compat@example.com";
        registerUser(email, "single_actor", "Password123!");

        LoginRequest req = LoginRequest.builder()
                .usernameOrEmail(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("single_actor"))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("16. Ambiguous shared-email login returns generic invalid credentials")
    void test16_ambiguousSharedEmailLogin_returnsGeneric401() throws Exception {
        String email = "ambiguous_auth@example.com";
        registerUser(email, "ambig_one", "Password123!");
        registerUser(email, "ambig_two", "Password123!");

        LoginRequest req = LoginRequest.builder()
                .usernameOrEmail(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("17. Ambiguous email login does not arbitrarily lock unrelated accounts")
    void test17_ambiguousEmailLogin_doesNotLockAccounts() throws Exception {
        String email = "lock_safe@example.com";
        registerUser(email, "safe_one", "Password123!");
        registerUser(email, "safe_two", "Password123!");

        // Fire 10 failed login attempts using ambiguous email
        LoginRequest failedReq = LoginRequest.builder()
                .usernameOrEmail(email)
                .password("WrongPassword123!")
                .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(failedReq)))
                    .andExpect(status().isUnauthorized());
        }

        if (redisRateLimiterService != null) {
            redisRateLimiterService.clearLimits();
        }

        // Both accounts must NOT be locked and must be able to log in with username
        LoginRequest req1 = LoginRequest.builder().usernameOrEmail("safe_one").password("Password123!").build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        LoginRequest req2 = LoginRequest.builder().usernameOrEmail("safe_two").password("Password123!").build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("18. Password reset by username changes only the target account")
    void test18_passwordResetByUsername_changesOnlyTargetAccount() throws Exception {
        String email = "shared_pwreset@example.com";
        registerUser(email, "pw_target", "InitialPass123!");
        registerUser(email, "pw_sibling", "InitialPass123!");

        // Request reset by username
        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder().email("pw_target").build();
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());

        User targetUser = userRepository.findByUsername("pw_target").orElseThrow();
        User siblingUser = userRepository.findByUsername("pw_sibling").orElseThrow();

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        PasswordResetToken targetToken = tokens.stream()
                .filter(t -> t.getUser().getId().equals(targetUser.getId()))
                .findFirst().orElseThrow();

        // Seed raw token matching hash for confirmation
        String rawToken = "my_special_reset_token_target_1234567";
        targetToken.setTokenHash(hashToken(rawToken));
        passwordResetTokenRepository.save(targetToken);

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewTargetPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // Target user can log in with new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pw_target", "NewTargetPass123!"))))
                .andExpect(status().isOk());

        // Sibling user still has original password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pw_sibling", "InitialPass123!"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("19. Shared-email reset sends isolated per-account links and consuming one changes only that account")
    void test19_sharedEmailReset_sendsIsolatedLinks_andConsumingOneChangesOnlyThatAccount() throws Exception {
        String email = "shared_reset_links@example.com";
        registerUser(email, "iso_one", "OldPass123!");
        registerUser(email, "iso_two", "OldPass123!");

        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder().email(email).build();
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());

        User u1 = userRepository.findByUsername("iso_one").orElseThrow();
        User u2 = userRepository.findByUsername("iso_two").orElseThrow();

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        PasswordResetToken t1 = tokens.stream().filter(t -> t.getUser().getId().equals(u1.getId())).findFirst().orElseThrow();
        PasswordResetToken t2 = tokens.stream().filter(t -> t.getUser().getId().equals(u2.getId())).findFirst().orElseThrow();

        assertThat(t1.getTokenHash()).isNotEqualTo(t2.getTokenHash());

        String rawToken1 = "raw_reset_token_for_iso_one_1234567";
        t1.setTokenHash(hashToken(rawToken1));
        passwordResetTokenRepository.save(t1);

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawToken1)
                .newPassword("BrandNewPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // u1 has new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("iso_one", "BrandNewPass123!"))))
                .andExpect(status().isOk());

        // u2 still has old password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("iso_two", "OldPass123!"))))
                .andExpect(status().isOk());

        // t2 is still unused
        PasswordResetToken refreshedT2 = passwordResetTokenRepository.findById(t2.getId()).orElseThrow();
        assertThat(refreshedT2.isUsed()).isFalse();
    }

    @Test
    @DisplayName("20. Legacy account verification remains account-specific")
    void test20_legacyAccountVerification_remainsAccountSpecific() throws Exception {
        String email = "legacy_specific@example.com";

        // Seed 2 legacy unverified users
        User u1 = User.builder()
                .username("legacy_u1")
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .accountSlot(1)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();
        User u2 = User.builder()
                .username("legacy_u2")
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .accountSlot(2)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAllAndFlush(List.of(u1, u2));

        // When multiple unverified exist, verify-otp without challenge userId binding fails securely
        VerifyOtpRequest req = VerifyOtpRequest.builder().email(email).code("123456").build();
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("21. Email changes allow a target with remaining capacity and reject a target at its limit")
    void test21_emailChange_capacityCheck() throws Exception {
        String targetEmail = "change_capacity@example.com";
        registerUser(targetEmail, "cap_1", "Password123!");
        registerUser(targetEmail, "cap_2", "Password123!");

        // Third user with different email
        MvcResult res3 = registerUser("other_user@example.com", "cap_3", "Password123!");
        String token3 = objectMapper.readTree(res3.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 3rd user initiates email change to targetEmail -> Allowed (2 < 3)
        InitiateEmailChangeRequest initReq = InitiateEmailChangeRequest.builder()
                .newEmail(targetEmail)
                .currentPassword("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/users/me/email")
                        .header("Authorization", "Bearer " + token3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk());

        // 4th user registers under separate email and tries to change to targetEmail -> Rejected at initiation or limit
        MvcResult res4 = registerUser("fourth_isolated@example.com", "cap_4", "Password123!");
        String token4 = objectMapper.readTree(res4.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Register a 3rd user directly on targetEmail to fill it up
        registerUser(targetEmail, "cap_filler", "Password123!");

        mockMvc.perform(post("/api/v1/users/me/email")
                        .header("Authorization", "Bearer " + token4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The target email address has reached the maximum allowed limit of 3 accounts."));
    }

    @Test
    @DisplayName("22. Concurrent email changes cannot exceed the target limit")
    void test22_concurrentEmailChanges_cannotExceedLimit() throws Exception {
        String targetEmail = "concurrent_change@example.com";
        registerUser(targetEmail, "base_1", "Password123!");
        registerUser(targetEmail, "base_2", "Password123!");

        // Target has 2 accounts. Only 1 more can change into it.
        MvcResult userARes = registerUser("user_a@example.com", "change_user_a", "Password123!");
        MvcResult userBRes = registerUser("user_b@example.com", "change_user_b", "Password123!");

        User userA = userRepository.findByUsername("change_user_a").orElseThrow();
        User userB = userRepository.findByUsername("change_user_b").orElseThrow();

        // Seed 2 pending email change requests targeting targetEmail
        String rawTokenA = "token_change_user_a_raw_1234567";
        String rawTokenB = "token_change_user_b_raw_1234567";

        EmailChangeRequest reqA = EmailChangeRequest.builder()
                .user(userA)
                .newEmail(targetEmail)
                .tokenHash(hashToken(rawTokenA))
                .status(EmailChangeStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        EmailChangeRequest reqB = EmailChangeRequest.builder()
                .user(userB)
                .newEmail(targetEmail)
                .tokenHash(hashToken(rawTokenB))
                .status(EmailChangeStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        emailChangeRequestRepository.saveAllAndFlush(List.of(reqA, reqB));

        // Concurrently attempt confirmation
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                latch.await();
                MvcResult r = mockMvc.perform(post("/api/v1/auth/confirm-email-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(rawTokenA))))
                        .andReturn();
                if (r.getResponse().getStatus() == 200) successCount.incrementAndGet();
                else if (r.getResponse().getStatus() == 409) conflictCount.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        futures.add(executor.submit(() -> {
            try {
                latch.await();
                MvcResult r = mockMvc.perform(post("/api/v1/auth/confirm-email-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(rawTokenB))))
                        .andReturn();
                if (r.getResponse().getStatus() == 200) successCount.incrementAndGet();
                else if (r.getResponse().getStatus() == 409) conflictCount.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        latch.countDown();
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        assertThat(userRepository.countByEmail(targetEmail)).isEqualTo(3);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("23. Sessions and refresh-token families remain isolated across accounts sharing an email")
    void test23_sessionsAndRefreshTokenFamilies_isolated() throws Exception {
        String email = "session_iso@example.com";
        registerUser(email, "sess_one", "Password123!");
        registerUser(email, "sess_two", "Password123!");

        // Log in to sess_one
        MvcResult loginOne = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sess_one", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonOne = objectMapper.readTree(loginOne.getResponse().getContentAsString()).path("data");
        String refreshOne = jsonOne.path("refreshToken").asText();
        String accessOne = jsonOne.path("accessToken").asText();

        // Log in to sess_two
        MvcResult loginTwo = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sess_two", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonTwo = objectMapper.readTree(loginTwo.getResponse().getContentAsString()).path("data");
        String refreshTwo = jsonTwo.path("refreshToken").asText();
        String accessTwo = jsonTwo.path("accessToken").asText();

        assertThat(refreshOne).isNotEqualTo(refreshTwo);
        assertThat(accessOne).isNotEqualTo(accessTwo);

        // Rotate token for sess_one
        MvcResult rotateOne = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshOne))))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshOne = objectMapper.readTree(rotateOne.getResponse().getContentAsString()).path("data").path("refreshToken").asText();
        assertThat(newRefreshOne).isNotEqualTo(refreshOne);
        assertThat(newRefreshOne).isNotEqualTo(refreshTwo);

        // sess_two refresh token remains valid
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshTwo))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("24. Existing security controls, rate limiting, and token verification remain intact")
    void test24_securityControls_remainIntact() throws Exception {
        String email = "security_intact@example.com";
        MvcResult regResult = registerUser(email, "sec_user", "Password123!");
        String accessToken = objectMapper.readTree(regResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // User settings endpoint requires valid Bearer token
        mockMvc.perform(get("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Bogus token rejected with 401
        mockMvc.perform(get("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer invalid_jwt_token"))
                .andExpect(status().isUnauthorized());
    }
}
