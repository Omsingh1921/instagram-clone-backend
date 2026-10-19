package com.instagramclone.config;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DataInitializerTest extends BaseIntegrationTest {

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        dataInitializer.setSeedAdmin(true);
    }

    @Test
    @DisplayName("Should seed default superadmin account when enabled and user does not exist")
    void shouldSeedDefaultAdminAccount() {
        // Given database is clean (no admin user)
        assertThat(userRepository.findByUsername("admin")).isEmpty();

        // When
        dataInitializer.seedDefaultAdmin();

        // Then
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertThat(adminOpt).isPresent();

        User admin = adminOpt.get();
        assertThat(admin.getEmail()).isEqualTo("admin@instagram.com");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches("Admin@12345", admin.getPasswordHash())).isTrue();

        assertThat(admin.getProfile()).isNotNull();
        assertThat(admin.getProfile().getDisplayName()).isEqualTo("System Administrator");

        Optional<UserSettings> settingsOpt = userSettingsRepository.findByUserId(admin.getId());
        assertThat(settingsOpt).isPresent();
        assertThat(settingsOpt.get().isPrivate()).isFalse();
    }

    @Test
    @DisplayName("Should be idempotent and not duplicate admin user if already present")
    void shouldBeIdempotentOnSubsequentRuns() {
        // First run
        dataInitializer.seedDefaultAdmin();
        long initialCount = userRepository.count();

        // Second run
        dataInitializer.seedDefaultAdmin();
        long secondCount = userRepository.count();

        assertThat(secondCount).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Should verify dynamic email configurations and templates are seeded")
    void shouldSeedConfigsAndTemplates() {
        dataInitializer.seedEmailConfigurations();
        dataInitializer.seedEmailTemplates();

        assertThat(dynamicEmailConfigRepository.count()).isEqualTo(5);
        assertThat(dynamicEmailTemplateRepository.count()).isGreaterThanOrEqualTo(17);
    }
}
