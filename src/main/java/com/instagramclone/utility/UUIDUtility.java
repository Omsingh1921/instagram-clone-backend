package com.instagramclone.utility;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.exception.BadRequestException;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reusable UUID operations adhering to Section 49.4 (UUID Utility).
 * Strictly maintains UUID/string identifiers and prevents numeric/sequential ID corruptions.
 */
public final class UUIDUtility {

    private static final Pattern UUID_PATTERN = Pattern.compile(ValidationConstants.UUID_REGEX);

    private UUIDUtility() {}

    public static UUID parseOrThrow(String uuidString, String fieldName) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new BadRequestException(fieldName + " is required and cannot be empty");
        }
        String trimmed = uuidString.trim();
        if (!isValid(trimmed)) {
            throw new BadRequestException("Invalid UUID format for " + fieldName + ": " + uuidString);
        }
        return UUID.fromString(trimmed);
    }

    public static Optional<UUID> parseOptional(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return Optional.empty();
        }
        String trimmed = uuidString.trim();
        if (!isValid(trimmed)) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(trimmed));
    }

    public static boolean isValid(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return false;
        }
        String trimmed = uuidString.trim();
        if (trimmed.length() != 36) {
            return false;
        }
        if (!UUID_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        try {
            UUID parsed = UUID.fromString(trimmed);
            return parsed.toString().equalsIgnoreCase(trimmed);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static UUID randomUUID() {
        return UUID.randomUUID();
    }
}
