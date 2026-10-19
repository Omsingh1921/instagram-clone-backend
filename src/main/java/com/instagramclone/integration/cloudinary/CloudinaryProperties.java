package com.instagramclone.integration.cloudinary;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cloudinary configuration properties adhering to Sections 3 & 4.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private boolean fallbackOnFailure = false;
    private int maxRetries = 2;
    private long retryBackoffMs = 500L;

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank()
                && !"your_cloud_name".equalsIgnoreCase(cloudName)
                && !"test-cloud".equalsIgnoreCase(cloudName)
                && !"demo".equalsIgnoreCase(cloudName);
    }
}
