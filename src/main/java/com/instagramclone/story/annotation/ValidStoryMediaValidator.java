package com.instagramclone.story.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidStoryMedia}.
 */
public class ValidStoryMediaValidator implements ConstraintValidator<ValidStoryMedia, String> {

    private static final int MAX_MEDIA_URL_LENGTH = 1000;

    @Override
    public boolean isValid(String mediaUrl, ConstraintValidatorContext context) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return false;
        }
        return mediaUrl.trim().length() <= MAX_MEDIA_URL_LENGTH;
    }
}
