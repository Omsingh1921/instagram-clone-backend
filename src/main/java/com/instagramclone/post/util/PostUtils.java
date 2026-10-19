package com.instagramclone.post.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain utility methods for Post processing, hashtag and mention extraction.
 */
public final class PostUtils {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9._]+)");

    private PostUtils() {
        // Prevent instantiation
    }

    /**
     * Extracts normalized hashtags (without '#', lowercase) from a caption string.
     */
    public static Set<String> extractHashtags(String caption) {
        if (caption == null || caption.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> tags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(caption);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Extracts user mentions (without '@', lowercase) from a caption string.
     */
    public static Set<String> extractMentions(String caption) {
        if (caption == null || caption.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> mentions = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(caption);
        while (matcher.find()) {
            mentions.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(mentions);
    }

    /**
     * Truncates caption for preview purposes with an ellipsis.
     */
    public static String truncateCaption(String caption, int maxLength) {
        if (caption == null) {
            return "";
        }
        if (caption.length() <= maxLength) {
            return caption;
        }
        return caption.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /**
     * Checks if a post contains multiple media items (carousel).
     */
    public static boolean isCarousel(int mediaCount) {
        return mediaCount > 1;
    }
}
