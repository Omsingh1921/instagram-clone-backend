package com.instagramclone.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO returned by username availability check and suggestion endpoint during registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing username availability and alternative suggestions")
public class UsernameCheckResponse {

    @Schema(description = "The normalized username evaluated", example = "john_doe")
    private String username;

    @Schema(description = "Whether the requested username is available for registration", example = "false")
    private boolean available;

    @Schema(description = "Informative status message", example = "Username is already taken")
    private String message;

    @Schema(description = "List of recommended available alternative usernames if taken or invalid", example = "[\"john_doe_2026\", \"the_john_doe\", \"john_doe_official\"]")
    private List<String> suggestions;
}
