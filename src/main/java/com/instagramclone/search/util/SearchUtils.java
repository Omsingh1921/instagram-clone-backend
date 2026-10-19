package com.instagramclone.search.util;

import java.util.Locale;

/**
 * Utility methods for sanitizing search input and normalizing hashtag queries.
 */
public final class SearchUtils {

    private SearchUtils() {
        // Prevent instantiation
    }

    /**
     * Sanitizes search queries by removing dangerous wildcard injections and leading/trailing whitespace.
     */
    public static String cleanQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().replaceAll("[%_\\\\]", "");
    }

    /**
     * Checks if a query is intended as a hashtag search.
     */
    public static boolean isHashtagSearch(String query) {
        return query != null && query.trim().startsWith("#");
    }

    /**
     * Normalizes a tag query by stripping leading '#' and converting to lowercase.
     */
    public static String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        String cleaned = tag.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }
}
