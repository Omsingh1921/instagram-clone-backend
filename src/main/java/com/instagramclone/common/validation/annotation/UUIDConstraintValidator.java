package com.instagramclone.common.validation.annotation;

import com.instagramclone.common.constants.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Production-grade validator for {@link ValidUUID}.
 * Strictly verifies UUID format without unsafe reflection or unchecked casts.
 */
public class UUIDConstraintValidator implements ConstraintValidator<ValidUUID, Object> {

    private static final Pattern UUID_PATTERN = Pattern.compile(ValidationConstants.UUID_REGEX);
    private boolean required;

    @Override
    public void initialize(ValidUUID constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return !required;
        }

        if (value instanceof UUID) {
            return true;
        }

        if (value instanceof CharSequence charSequence) {
            String str = charSequence.toString().trim();
            if (str.isEmpty()) {
                return !required;
            }
            return isValidUuidString(str);
        }

        if (value instanceof Iterable<?> iterable) {
            boolean hasElements = false;
            for (Object element : iterable) {
                hasElements = true;
                if (element == null) {
                    return false;
                }
                if (element instanceof UUID) {
                    continue;
                }
                if (element instanceof CharSequence cs) {
                    String str = cs.toString().trim();
                    if (str.isEmpty() || !isValidUuidString(str)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            if (!hasElements) {
                return !required;
            }
            return true;
        }

        // Unsupported object types fail deterministically
        return false;
    }

    private boolean isValidUuidString(String str) {
        if (str.length() != 36) {
            return false;
        }
        if (!UUID_PATTERN.matcher(str).matches()) {
            return false;
        }
        try {
            UUID parsed = UUID.fromString(str);
            return parsed.toString().equalsIgnoreCase(str);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
