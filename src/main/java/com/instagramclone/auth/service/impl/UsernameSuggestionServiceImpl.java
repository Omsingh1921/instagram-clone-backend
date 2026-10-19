package com.instagramclone.auth.service.impl;

import com.instagramclone.auth.constants.AuthConstants;
import com.instagramclone.auth.dto.response.UsernameCheckResponse;
import com.instagramclone.auth.service.interfaces.IUsernameSuggestionService;
import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.util.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Production implementation of username availability checking and intelligent suggestion generation.
 * Adheres to Instagram Clone validation constraints, reserved word rules, and single-query batch optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsernameSuggestionServiceImpl implements IUsernameSuggestionService {

    private final UserRepository userRepository;

    private static final Pattern USERNAME_PATTERN = Pattern.compile(ValidationConstants.USERNAME_REGEX);
    private static final int MAX_SUGGESTIONS = 5;
    private static final int TARGET_MIN_SUGGESTIONS = 4;
    private static final int MAX_BASE_LENGTH_FOR_SUFFIX = 20;

    /**
     * Set of reserved usernames that cannot be registered by standard users.
     */
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "administrator", "root", "system", "support", "help",
            "security", "instagram", "instagramclone", "official", "api", "auth",
            "null", "undefined", "explore", "direct", "stories", "reels",
            "settings", "terms", "privacy", "moderator", "verify", "verified"
    );

    @Override
    @Transactional(readOnly = true)
    public UsernameCheckResponse checkAndSuggest(String rawUsername) {
        if (rawUsername == null || rawUsername.isBlank()) {
            return UsernameCheckResponse.builder()
                    .username("")
                    .available(false)
                    .message(ValidationConstants.USERNAME_REQUIRED)
                    .suggestions(Collections.emptyList())
                    .build();
        }

        String normalized = UserUtils.normalizeUsername(rawUsername);

        boolean formatValid = USERNAME_PATTERN.matcher(normalized).matches();
        boolean isReserved = RESERVED_USERNAMES.contains(normalized);
        boolean existsInDb = formatValid && userRepository.existsByUsername(normalized);

        // Case 1: Desired username is valid, unreserved, and available
        if (formatValid && !isReserved && !existsInDb) {
            log.debug("Username [{}] is available for registration", normalized);
            return UsernameCheckResponse.builder()
                    .username(normalized)
                    .available(true)
                    .message(AuthConstants.MSG_USERNAME_AVAILABLE)
                    .suggestions(Collections.emptyList())
                    .build();
        }

        // Case 2: Desired username is unavailable (taken, reserved, or invalid format)
        String statusMessage;
        if (isReserved) {
            statusMessage = AuthConstants.MSG_USERNAME_RESERVED;
        } else if (!formatValid) {
            statusMessage = AuthConstants.MSG_USERNAME_INVALID;
        } else {
            statusMessage = AuthConstants.MSG_USERNAME_TAKEN;
        }

        log.debug("Username [{}] is not available (reason: {}). Generating recommendations.", normalized, statusMessage);
        List<String> suggestions = generateSuggestions(normalized);

        return UsernameCheckResponse.builder()
                .username(normalized)
                .available(false)
                .message(statusMessage)
                .suggestions(suggestions)
                .build();
    }

    private List<String> generateSuggestions(String input) {
        String base = sanitizeBase(input);
        Set<String> candidates = new LinkedHashSet<>();
        Random rng = new Random();

        // 1. Year variations
        addIfValid(candidates, base + "2026");
        addIfValid(candidates, base + "_2026");

        // 2. Curated prefixes
        addIfValid(candidates, "the_" + base);
        addIfValid(candidates, "real_" + base);
        addIfValid(candidates, "iam_" + base);
        addIfValid(candidates, "its_" + base);

        // 3. Curated suffixes
        addIfValid(candidates, base + "_official");
        addIfValid(candidates, base + "_live");
        addIfValid(candidates, base + "_daily");

        // 4. Random numbers & separator variations
        addIfValid(candidates, base + (rng.nextInt(90) + 10));
        addIfValid(candidates, base + "_" + (rng.nextInt(90) + 10));
        addIfValid(candidates, base + (rng.nextInt(900) + 100));
        addIfValid(candidates, base + "_" + (rng.nextInt(900) + 100));

        // 5. Underscore / dot character transposition
        if (base.contains("_")) {
            addIfValid(candidates, base.replace('_', '.'));
        }
        if (base.contains(".")) {
            addIfValid(candidates, base.replace('.', '_'));
        }

        // Batch query existing usernames in one database round-trip
        List<String> existingUsernames = userRepository.findExistingUsernamesIn(candidates);
        Set<String> takenSet = new HashSet<>(existingUsernames);

        List<String> available = candidates.stream()
                .filter(c -> !takenSet.contains(c))
                .limit(MAX_SUGGESTIONS)
                .collect(Collectors.toList());

        // Fallback: If heavy collision, generate randomized 4-digit numbers until target is reached
        int fallbackAttempts = 0;
        while (available.size() < TARGET_MIN_SUGGESTIONS && fallbackAttempts < 20) {
            fallbackAttempts++;
            String candidate = base + "_" + (rng.nextInt(9000) + 1000);
            if (isValidUsername(candidate) && !takenSet.contains(candidate) && !available.contains(candidate)) {
                if (!userRepository.existsByUsername(candidate)) {
                    available.add(candidate);
                } else {
                    takenSet.add(candidate);
                }
            }
        }

        return available;
    }

    private String sanitizeBase(String input) {
        if (input == null || input.isBlank()) {
            return "user";
        }

        // Replace spaces and special characters with underscore
        String sanitized = input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._]", "");

        // Collapse duplicate separators
        sanitized = sanitized.replaceAll("_+", "_").replaceAll("\\.+", ".");

        // Strip leading and trailing periods (forbidden by Instagram regex)
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        // If base is empty or too short, expand it
        if (sanitized.length() < ValidationConstants.USERNAME_MIN_LENGTH) {
            sanitized = "user_" + (new Random().nextInt(90) + 10);
        }

        // Limit base length so added suffixes stay within 30-character bound
        if (sanitized.length() > MAX_BASE_LENGTH_FOR_SUFFIX) {
            sanitized = sanitized.substring(0, MAX_BASE_LENGTH_FOR_SUFFIX);
            // Ensure trailing character after truncation is not a period
            sanitized = sanitized.replaceAll("\\.+$", "");
        }

        return sanitized;
    }

    private void addIfValid(Set<String> set, String candidate) {
        if (isValidUsername(candidate)) {
            set.add(candidate);
        }
    }

    private boolean isValidUsername(String username) {
        return username != null
                && username.length() >= ValidationConstants.USERNAME_MIN_LENGTH
                && username.length() <= ValidationConstants.USERNAME_MAX_LENGTH
                && USERNAME_PATTERN.matcher(username).matches()
                && !RESERVED_USERNAMES.contains(username);
    }
}
