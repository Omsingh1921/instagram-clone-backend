package com.instagramclone.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation to sanitize and normalize String input fields in request DTOs.
 * Safely trims whitespace while automatically preserving passwords, tokens, OTPs,
 * and user multiline formatting (captions, comments, bios).
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TrimValidatorService.class)
@Documented
public @interface TrimValidator {

    String message() default com.instagramclone.common.constants.ValidationConstants.TRIM_SUCCESS_MESSAGE;

    String[] exclude() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
