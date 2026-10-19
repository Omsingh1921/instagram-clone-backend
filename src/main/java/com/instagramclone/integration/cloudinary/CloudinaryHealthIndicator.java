package com.instagramclone.integration.cloudinary;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot Actuator Health Indicator for Cloudinary cloud media storage.
 */
@Component
@RequiredArgsConstructor
public class CloudinaryHealthIndicator implements HealthIndicator {

    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public Health health() {
        if (cloudinaryProperties == null || !cloudinaryProperties.isConfigured()) {
            return Health.up()
                    .withDetail("service", "Cloudinary")
                    .withDetail("status", "STANDBY_LOCAL_FALLBACK")
                    .withDetail("message", "Cloudinary credentials not provided; system operating in local storage fallback mode")
                    .build();
        }

        return Health.up()
                .withDetail("service", "Cloudinary")
                .withDetail("cloudName", cloudinaryProperties.getCloudName())
                .withDetail("status", "CONFIGURED")
                .build();
    }
}
