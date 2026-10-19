package com.instagramclone.engagement.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that comment text is non-empty and does not exceed maximum length.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCommentTextValidator.class)
@Documented
public @interface ValidCommentText {

    String message() default com.instagramclone.engagement.constants.EngagementConstants.MSG_INVALID_COMMENT;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
