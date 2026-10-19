package com.instagramclone.utility;

import java.util.Arrays;
import java.util.Optional;

/**
 * Reusable utility for safe, case-insensitive enum parsing and validation
 * across all domain modules (reports, media types, notifications, user statuses, etc.).
 */
public final class EnumUtility {

    private EnumUtility() {
        // Prevent instantiation
    }

    /**
     * Parses an enum constant by case-insensitive name matching.
     *
     * @param enumClass the enum class
     * @param value     the string representation
     * @param <T>       the enum type
     * @return the matched enum constant
     * @throws IllegalArgumentException if no match is found or value is null/blank
     */
    public static <T extends Enum<T>> T fromString(Class<T> enumClass, String value) {
        if (enumClass == null || value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enum class and value must not be null or blank");
        }

        String trimmed = value.trim();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }

        throw new IllegalArgumentException(
                "Invalid " + enumClass.getSimpleName() + " value: '" + value + "'. Allowed values: " +
                        Arrays.toString(enumClass.getEnumConstants())
        );
    }

    /**
     * Parses an enum constant by case-insensitive name matching, returning a default value if absent or invalid.
     *
     * @param enumClass    the enum class
     * @param value        the string representation
     * @param defaultValue the default fallback value
     * @param <T>          the enum type
     * @return the matched enum constant, or defaultValue
     */
    public static <T extends Enum<T>> T fromStringOrDefault(Class<T> enumClass, String value, T defaultValue) {
        if (enumClass == null || value == null || value.isBlank()) {
            return defaultValue;
        }

        String trimmed = value.trim();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }

        return defaultValue;
    }

    /**
     * Parses an enum constant returning an Optional if present and valid.
     *
     * @param enumClass the enum class
     * @param value     the string representation
     * @param <T>       the enum type
     * @return Optional containing matched constant, or empty
     */
    public static <T extends Enum<T>> Optional<T> fromStringOptional(Class<T> enumClass, String value) {
        if (enumClass == null || value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(trimmed)) {
                return Optional.of(constant);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a string matches any enum constant case-insensitively.
     *
     * @param enumClass the enum class
     * @param value     the string representation
     * @param <T>       the enum type
     * @return true if string represents a valid enum constant, false otherwise
     */
    public static <T extends Enum<T>> boolean isValid(Class<T> enumClass, String value) {
        return fromStringOptional(enumClass, value).isPresent();
    }
}
