package com.instagramclone.auth.service.interfaces;

import com.instagramclone.auth.dto.response.UsernameCheckResponse;

/**
 * Service contract for checking username availability during registration
 * and generating smart, authentic alternative recommendations when a username is taken.
 */
public interface IUsernameSuggestionService {

    /**
     * Evaluates a requested username, checking availability against active/inactive
     * user records, reserved handles, and validation constraints.
     * Generates a list of valid, available suggestions if unavailable.
     *
     * @param rawUsername the raw input typed by the user
     * @return UsernameCheckResponse with availability flag, explanatory message, and suggestions
     */
    UsernameCheckResponse checkAndSuggest(String rawUsername);
}
