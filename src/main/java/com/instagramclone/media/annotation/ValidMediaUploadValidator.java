package com.instagramclone.media.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for {@link ValidMediaUpload}.
 */
public class ValidMediaUploadValidator implements ConstraintValidator<ValidMediaUpload, String> {

    private static final int MAX_URL_LENGTH = 1000;

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.trim().length() <= MAX_URL_LENGTH;
    }
}
