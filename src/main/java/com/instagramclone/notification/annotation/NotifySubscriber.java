package com.instagramclone.notification.annotation;

import com.instagramclone.notification.enums.NotificationType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation marking service actions that dispatch notifications to recipients.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotifySubscriber {

    NotificationType type();
}
