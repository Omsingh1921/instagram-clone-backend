package com.instagramclone.media.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that uploaded media URL is non-empty and formatted properly.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMediaUploadValidator.class)
@Documented
public @interface ValidMediaUpload {

    String message() default com.instagramclone.media.constants.MediaConstants.MSG_INVALID_MEDIA_URL;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
