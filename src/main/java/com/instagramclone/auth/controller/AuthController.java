package com.instagramclone.auth.controller;

import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.common.constants.SecurityConstants;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.mail.dto.request.UnsubscribeRequest;
import com.instagramclone.mail.service.interfaces.IEmailPreferenceService;
import com.instagramclone.auth.constants.AuthConstants;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import com.instagramclone.auth.dto.response.*;
import com.instagramclone.user.dto.response.*;
import com.instagramclone.auth.service.interfaces.IAuthService;
import com.instagramclone.user.service.interfaces.IEmailChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AuthConstants.AUTH_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = AuthConstants.TAG_AUTH, description = AuthConstants.TAG_AUTH_DESCRIPTION)
public class AuthController {

    private final IAuthService authService;
    private final IEmailPreferenceService emailPreferenceService;
    private final IEmailChangeService emailChangeService;

    @RateLimit(category = AuthConstants.RATE_LIMIT_SIGNUP_REQUEST_OTP, limit = 5, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_SIGNUP_REQUEST_OTP)
    @Operation(summary = "Request email OTP for signup", description = "Initiates staged registration OTP verification for uncommitted user")
    public ResponseEntity<ApiResponse<Void>> requestSignupOtp(@Valid @RequestBody SignupRequestOtpRequest request) {
        authService.requestSignupOtp(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_SIGNUP_OTP_SENT, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_SIGNUP_VERIFY_OTP, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_SIGNUP_VERIFY_OTP)
    @Operation(summary = "Verify signup email OTP", description = "Verifies 6-digit OTP and issues a short-lived, single-use signup proof")
    public ResponseEntity<ApiResponse<SignupProofResponse>> verifySignupOtp(@Valid @RequestBody SignupVerifyOtpRequest request) {
        SignupProofResponse response = authService.verifySignupOtp(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_EMAIL_VERIFIED_SUCCESS, response));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_SIGNUP_REQUEST_OTP, limit = 3, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_SIGNUP_RESEND_OTP)
    @Operation(summary = "Resend signup email OTP", description = "Resends 6-digit OTP for staged registration subject to cooldown")
    public ResponseEntity<ApiResponse<Void>> resendSignupOtp(@Valid @RequestBody SignupRequestOtpRequest request) {
        authService.requestSignupOtp(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_SIGNUP_OTP_SENT, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_CHECK_USERNAME, limit = 60, windowSeconds = 60)
    @GetMapping(AuthConstants.PATH_CHECK_USERNAME)
    @Operation(summary = "Check username availability and get suggestions", description = "Checks whether a requested username is available and returns smart suggestions if taken or reserved")
    public ResponseEntity<ApiResponse<UsernameCheckResponse>> checkUsername(
            @RequestParam(value = "username", required = false) String username) {
        UsernameCheckResponse response = authService.checkUsername(username);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_REGISTER, limit = 5, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_REGISTER)
    @Operation(summary = "Register new user account", description = "Creates a new user and baseline profile from validated signup proof, issuing session tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success(AuthConstants.MSG_USER_REGISTERED_SUCCESS, response),
                HttpStatus.CREATED
        );
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_LOGIN, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_LOGIN)
    @Operation(summary = "Authenticate user credentials", description = "Validates username/email and password, issuing access and refresh JWTs")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_LOGIN_SUCCESS, response));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_REFRESH, limit = 20, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_REFRESH)
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_TOKEN_REFRESHED_SUCCESS, response));
    }

    @PostMapping(AuthConstants.PATH_LOGOUT)
    @Operation(summary = "Logout user", description = "Revokes refresh token and terminates authenticated session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(value = SecurityConstants.HEADER_AUTHORIZATION, required = false) String authHeader) {
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            accessToken = authHeader.substring(SecurityConstants.TOKEN_PREFIX_LENGTH).trim();
        }
        authService.logout(refreshToken, accessToken);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_LOGOUT_SUCCESS, null));
    }

    @PostMapping(AuthConstants.PATH_LOGOUT_ALL)
    @Operation(summary = "Logout user from all sessions", description = "Revokes all refresh tokens and terminates all authenticated sessions across all devices")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_ALL_SESSIONS_LOGGED_OUT_SUCCESS, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_VERIFY_EMAIL, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_VERIFY_EMAIL)
    @Operation(summary = "Verify account email address", description = "Validates the single-use verification token and marks account email as verified")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_EMAIL_VERIFIED_SUCCESS, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_RESEND_VERIFICATION, limit = 3, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_RESEND_VERIFICATION)
    @Operation(summary = "Resend email verification token", description = "Issues a new verification email with anti-enumeration protection")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_VERIFICATION_LINK_SENT, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_VERIFY_OTP, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_VERIFY_OTP)
    @Operation(summary = "Verify account with 6-digit email OTP", description = "Validates the single-use numeric OTP, marks account email as verified, and issues session tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_EMAIL_VERIFIED_SUCCESS, response));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_RESEND_OTP, limit = 3, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_RESEND_OTP)
    @Operation(summary = "Resend email OTP verification code", description = "Issues a fresh 6-digit OTP code with anti-enumeration protection and rate limiting")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_OTP_SENT_SUCCESS, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_FORGOT_PASSWORD, limit = 3, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_FORGOT_PASSWORD)
    @Operation(summary = "Request password reset", description = "Issues a single-use password reset link with anti-enumeration protection")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_PASSWORD_RESET_LINK_SENT, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_RESET_PASSWORD, limit = 5, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_RESET_PASSWORD)
    @Operation(summary = "Reset account password", description = "Validates single-use reset token, sets new password, and revokes all active sessions")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_PASSWORD_RESET_SUCCESS, null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_UNSUBSCRIBE, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_UNSUBSCRIBE)
    @Operation(summary = "Unsubscribe from optional notification category", description = "Validates cryptographically signed token and updates email preferences")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
        emailPreferenceService.unsubscribeWithToken(request.getUserId(), request.getEmailType(), request.getExpires(), request.getSignature());
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.formatUnsubscribeSuccess(request.getEmailType().name()), null));
    }

    @RateLimit(category = AuthConstants.RATE_LIMIT_CONFIRM_EMAIL, limit = 10, windowSeconds = 60)
    @PostMapping(AuthConstants.PATH_CONFIRM_EMAIL_CHANGE)
    @Operation(summary = "Confirm email address change", description = "Validates the blind confirmation token and switches account email")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(@Valid @RequestBody ConfirmEmailChangeRequest request) {
        emailChangeService.confirmEmailChange(request);
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.MSG_EMAIL_CHANGE_CONFIRMED, null));
    }
}
