package com.instagramclone.post.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates post caption length and content restrictions.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCaptionValidator.class)
@Documented
public @interface ValidCaption {

    String message() default com.instagramclone.post.constants.PostConstants.MSG_INVALID_CAPTION;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
