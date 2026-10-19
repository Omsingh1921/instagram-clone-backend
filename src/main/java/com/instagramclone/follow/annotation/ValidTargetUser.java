package com.instagramclone.follow.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a target user UUID is non-null and valid.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTargetUserValidator.class)
@Documented
public @interface ValidTargetUser {

    String message() default com.instagramclone.follow.constants.FollowConstants.MSG_INVALID_TARGET_USER;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
