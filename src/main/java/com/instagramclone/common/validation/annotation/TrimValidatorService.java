package com.instagramclone.common.validation.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production-grade implementation of {@link TrimValidator}.
 * Safely sanitizes string inputs while strictly safeguarding:
 * 1. Exact-match credentials (passwords, tokens, OTP codes, signup proofs).
 * 2. Multi-line user-generated text (captions, bios, comments, chat content).
 */
@Component
public class TrimValidatorService implements ConstraintValidator<TrimValidator, Object> {

    private static final Logger log = LoggerFactory.getLogger(TrimValidatorService.class);

    /**
     * Exact-match security credentials that must NEVER be trimmed, collapsed, or mutated.
     */
    private static final Set<String> IMMUTABLE_SECURITY_FIELDS = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "signuptoken",
            "refreshtoken",
            "code",
            "signature",
            "secret",
            "key",
            "hash"
    );

    /**
     * User-generated fields where paragraph line breaks (\n) must be preserved.
     */
    private static final Set<String> MULTILINE_CONTENT_FIELDS = Set.of(
            "caption",
            "bio",
            "text",
            "content",
            "description",
            "notes"
    );

    private Set<String> explicitlyExcludedFields = new HashSet<>();

    @Override
    public void initialize(TrimValidator constraintAnnotation) {
        if (constraintAnnotation.exclude() != null && constraintAnnotation.exclude().length > 0) {
            this.explicitlyExcludedFields = Arrays.stream(constraintAnnotation.exclude())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                if (field.getType().equals(String.class)) {
                    String fieldNameLower = field.getName().toLowerCase();

                    // Security Guard: Never mutate passwords, OTP codes, or tokens
                    if (IMMUTABLE_SECURITY_FIELDS.contains(fieldNameLower)
                            || explicitlyExcludedFields.contains(fieldNameLower)) {
                        continue;
                    }

                    field.setAccessible(true);
                    try {
                        Object value = field.get(obj);
                        if (value instanceof String str) {
                            if (MULTILINE_CONTENT_FIELDS.contains(fieldNameLower)) {
                                // For captions, bios, comments: trim outer whitespace only, preserve newlines
                                field.set(obj, str.trim());
                            } else {
                                // For general single-line attributes: trim and collapse redundant spaces
                                field.set(obj, str.trim().replaceAll("\\s+", " "));
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        log.debug("Failed to access field [{}] during trim validation: {}", field.getName(), ex.getMessage());
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        return true;
    }
}
