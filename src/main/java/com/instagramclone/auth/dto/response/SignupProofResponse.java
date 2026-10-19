package com.instagramclone.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing single-use signup proof issued upon successful email OTP verification.
 * The signupToken contains 32 cryptographically random bytes formatted as a 64-character lowercase hex string.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single-use signup proof response issued upon successful email OTP verification")
public class SignupProofResponse {

    @Schema(
            description = "Cryptographically secure 256-bit signup token (64-character lowercase hex string)",
            example = "4a7f2c89e1b3d56789abcdef0123456789abcdef0123456789abcdef01234567"
    )
    private String signupToken;

    @Schema(description = "Token validity lifetime in seconds", example = "600")
    private Long expiresIn;
}
