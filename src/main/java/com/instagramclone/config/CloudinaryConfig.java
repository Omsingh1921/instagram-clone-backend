package com.instagramclone.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.instagramclone.integration.cloudinary.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated Cloudinary client configuration adhering to Section 4.
 */
@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);
    private final CloudinaryProperties properties;

    @Bean
    public Cloudinary cloudinary() {
        if (properties.isConfigured()) {
            log.info("Cloudinary client initialized successfully for cloud-name: {}", properties.getCloudName());
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", properties.getCloudName(),
                    "api_key", properties.getApiKey(),
                    "api_secret", properties.getApiSecret(),
                    "secure", true
            ));
        } else {
            log.info("Cloudinary credentials not active. Resilient local media storage fallback active.");
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "fallback-cloud",
                    "api_key", "fallback-key",
                    "api_secret", "fallback-secret",
                    "secure", true
            ));
        }
    }
}
