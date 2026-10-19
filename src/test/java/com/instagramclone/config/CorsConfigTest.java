package com.instagramclone.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    @DisplayName("Development - Allows configured localhost 5174")
    void testCorsAllowsLocalhost5174() {
        CorsConfig corsConfig = new CorsConfig();
        corsConfig.setAllowedOrigins(new String[]{
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174"
        });

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("Origin", "http://localhost:5174");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config, "CORS config must not be null");

        String matchedOrigin = config.checkOrigin("http://localhost:5174");
        assertEquals("http://localhost:5174", matchedOrigin, "Origin 5174 must be matched and allowed");
    }

    @Test
    @DisplayName("Development - Explicitly allows React frontend on http://localhost:5173 with credentials")
    void testDevelopmentCorsAllowsLocalhost5173() {
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config, "CORS config must not be null");

        String matchedOrigin = config.checkOrigin("http://localhost:5173");
        assertEquals("http://localhost:5173", matchedOrigin, "Origin http://localhost:5173 must be allowed");
        assertTrue(config.getAllowCredentials(), "Credentials must be enabled for allowed localhost origin");
    }

    @Test
    @DisplayName("Development - Explicitly allows React frontend on http://127.0.0.1:5173 with credentials")
    void testDevelopmentCorsAllows127001_5173() {
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("Origin", "http://127.0.0.1:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config, "CORS config must not be null");

        String matchedOrigin = config.checkOrigin("http://127.0.0.1:5173");
        assertEquals("http://127.0.0.1:5173", matchedOrigin, "Origin http://127.0.0.1:5173 must be allowed");
        assertTrue(config.getAllowCredentials(), "Credentials must be enabled for allowed 127.0.0.1 origin");
    }

    @Test
    @DisplayName("Development - Rejects arbitrary external origins")
    void testDevelopmentCorsRejectsExternalOrigin() {
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matchedOrigin = config.checkOrigin("https://evil.com");
        assertNull(matchedOrigin, "Arbitrary external origin https://evil.com must be rejected in development");
    }

    @Test
    @DisplayName("Development - Rejects subdomain bypass attempts like http://localhost.attacker.com")
    void testDevelopmentCorsRejectsSubdomainBypass() {
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matchedOrigin = config.checkOrigin("http://localhost.attacker.com");
        assertNull(matchedOrigin, "Subdomain bypass attempt must be rejected");
    }

    @Test
    @DisplayName("Development - Wildcard star is never added or combined with credentials")
    void testDevelopmentCorsNeverAllowsWildcardStar() {
        CorsConfig corsConfig = new CorsConfig();
        corsConfig.setAllowedOrigins(new String[]{"*"});

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        // Verify * is not matched as an origin
        assertNull(config.checkOrigin("https://random-site.com"));
        assertTrue(config.getAllowCredentials(), "Credentials remain enabled only for explicit localhost patterns");
    }

    @Test
    @DisplayName("Production - Explicitly approved production origins are accepted")
    void testProductionCorsAllowsApprovedOrigin() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{
                "https://instagram.example.com",
                "https://app.instagram.example.com"
        });

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matched = config.checkOrigin("https://instagram.example.com");
        assertEquals("https://instagram.example.com", matched, "Approved production origin must be matched");
        assertTrue(config.getAllowCredentials(), "AllowCredentials must remain enabled");
    }

    @Test
    @DisplayName("Production - Unapproved origins are rejected")
    void testProductionCorsRejectsUnapprovedOrigin() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{
                "https://instagram.example.com"
        });

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matched = config.checkOrigin("https://malicious-site.com");
        assertNull(matched, "Unapproved origin must not be allowed");
    }

    @Test
    @DisplayName("Production - Does not inherit localhost or local IP wildcard patterns")
    void testProductionCorsDoesNotInheritLocalhostWildcard() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{
                "https://instagram.example.com"
        });

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        assertNull(config.checkOrigin("http://localhost:3000"), "Production must not inherit localhost:3000");
        assertNull(config.checkOrigin("http://localhost:5173"), "Production must not inherit localhost:5173");
        assertNull(config.checkOrigin("http://127.0.0.1:8080"), "Production must not inherit 127.0.0.1");
    }

    @Test
    @DisplayName("Production - Reject wildcard origin '*' pattern")
    void testProductionCorsRejectsWildcard() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{"*"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Wildcard origin pattern"));
    }

    @Test
    @DisplayName("Production - Reject localhost origins in production configuration")
    void testProductionCorsRejectsLocalhostInProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{"http://localhost:5173"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Localhost origin"));
    }

    @Test
    @DisplayName("Production - Fails fast when allowed origins are missing or empty")
    void testProductionCorsFailsWhenOriginsMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{""});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Empty origin configuration is forbidden in production"));

        corsConfig.setAllowedOrigins(null);
        assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
    }

    @Test
    @DisplayName("Production - Reject origins without valid http/https scheme")
    void testProductionCorsRejectsInvalidScheme() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CorsConfig corsConfig = new CorsConfig(env);
        corsConfig.setAllowedOrigins(new String[]{"ftp://instagram.example.com"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Explicit scheme"));
    }
}
