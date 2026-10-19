package com.instagramclone.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class CorsConfig {

    private final Environment environment;

    @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174}")
    private String[] allowedOrigins;

    public CorsConfig(@Autowired(required = false) Environment environment) {
        this.environment = environment;
    }

    public CorsConfig() {
        this(null);
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public boolean isProductionEnvironment() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> configuredOrigins = new ArrayList<>();
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                if (origin != null && !origin.trim().isEmpty()) {
                    configuredOrigins.add(origin.trim());
                }
            }
        }

        boolean isProduction = isProductionEnvironment();

        if (isProduction) {
            if (configuredOrigins.isEmpty()) {
                throw new IllegalStateException(
                        "Production CORS configuration requires explicit allowed origins (e.g. via CORS_ALLOWED_ORIGINS or app.security.cors.allowed-origins). " +
                        "Empty origin configuration is forbidden in production."
                );
            }

            for (String origin : configuredOrigins) {
                if (origin.equals("*") || origin.contains("*")) {
                    throw new IllegalStateException(
                            "Wildcard origin pattern '" + origin + "' is strictly forbidden in production with allowCredentials=true."
                    );
                }
                if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
                    throw new IllegalStateException(
                            "Localhost origin '" + origin + "' is forbidden in production environment."
                    );
                }
                if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                    throw new IllegalStateException(
                            "CORS origin '" + origin + "' is invalid. Explicit scheme (http:// or https://) is required."
                    );
                }
            }

            // In production: only allow explicitly configured and validated origins (exact match)
            configuration.setAllowedOrigins(configuredOrigins);
            log.info("Configured production CORS with {} explicit allowed origins: {}", configuredOrigins.size(), configuredOrigins);
        } else {
            // In development / test: explicitly allow React frontend (5173) and dynamic localhost Vite ports
            List<String> patterns = new ArrayList<>();
            patterns.add("http://localhost:5173");
            patterns.add("http://127.0.0.1:5173");
            patterns.add("http://localhost:*");
            patterns.add("http://127.0.0.1:*");
            for (String origin : configuredOrigins) {
                // Never combine credentials with arbitrary wildcard '*'
                if (!"*".equals(origin) && !patterns.contains(origin)) {
                    patterns.add(origin);
                }
            }
            configuration.setAllowedOriginPatterns(patterns);
            log.info("Configured development CORS with allowed origin patterns: {}", patterns);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
