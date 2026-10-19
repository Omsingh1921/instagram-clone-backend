package com.instagramclone.auth.service.interfaces;

import com.instagramclone.auth.dto.request.*;
import com.instagramclone.auth.dto.response.*;

public interface IAuthService {

    void requestSignupOtp(SignupRequestOtpRequest request);

    SignupProofResponse verifySignupOtp(SignupVerifyOtpRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    void logout(String refreshToken, String accessToken);

    void logoutAll();

    void verifyEmail(VerifyEmailRequest request);

    void resendVerification(ResendVerificationRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    UsernameCheckResponse checkUsername(String username);
}
