package com.instagramclone.config;

import com.instagramclone.mail.service.interfaces.IDynamicEmailConfigService;
import com.instagramclone.mail.service.interfaces.IDynamicEmailTemplateService;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Automatically seeds default application data on startup:
 * 1. Default SuperAdmin registration (credentials configurable via app.admin.*)
 * 2. Default dynamic email sender configurations for all EmailCategories
 * 3. Default dynamic email templates for all EmailTypes
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final IDynamicEmailConfigService dynamicEmailConfigService;
    private final IDynamicEmailTemplateService dynamicEmailTemplateService;

    @Value("${app.admin.seed:true}")
    private boolean seedAdmin;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@instagram.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.display-name:System Administrator}")
    private String adminDisplayName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting application default data initialization...");
        seedEmailConfigurations();
        seedEmailTemplates();
        seedDefaultAdmin();
        log.info("Application default data initialization completed.");
    }

    public void seedEmailConfigurations() {
        try {
            dynamicEmailConfigService.initDefaultConfigs();
            log.info("[DATA INITIALIZER] Dynamic email configurations verified/seeded.");
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Could not initialize dynamic email configs: {}", ex.getMessage());
        }
    }

    public void seedEmailTemplates() {
        try {
            dynamicEmailTemplateService.initDefaultTemplates();
            log.info("[DATA INITIALIZER] Dynamic email templates verified/seeded.");
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Could not initialize dynamic email templates: {}", ex.getMessage());
        }
    }

    public void setSeedAdmin(boolean seedAdmin) {
        this.seedAdmin = seedAdmin;
    }

    public void seedDefaultAdmin() {
        seedDefaultAdmin(false);
    }

    public void seedDefaultAdmin(boolean force) {
        if (!seedAdmin && !force) {
            log.info("[DATA INITIALIZER] Default admin seeding is disabled by configuration (app.admin.seed=false).");
            return;
        }

        String normalizedUsername = adminUsername.trim().toLowerCase();
        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            log.info("[DATA INITIALIZER] Default admin user [{}] already exists. Skipping creation.", normalizedUsername);
            return;
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("[DATA INITIALIZER] An account with email [{}] already exists under another username. Skipping default admin seeding.", normalizedEmail);
            return;
        }

        User adminUser = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .accountSlot(1)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .failedLoginAttempts(0)
                .build();

        Profile profile = Profile.builder()
                .user(adminUser)
                .displayName(adminDisplayName != null && !adminDisplayName.isBlank() ? adminDisplayName.trim() : "System Administrator")
                .bio("Official Instagram Clone System Administrator")
                .build();

        adminUser.setProfile(profile);

        User savedAdmin = userRepository.save(adminUser);

        if (userSettingsRepository.findByUserId(savedAdmin.getId()).isEmpty()) {
            UserSettings settings = UserSettings.builder()
                    .user(savedAdmin)
                    .isPrivate(false)
                    .notifyLikes(true)
                    .notifyComments(true)
                    .notifyFollows(true)
                    .notifyMessages(true)
                    .emailSocialFollows(true)
                    .emailSocialLikes(true)
                    .emailSocialComments(true)
                    .emailMessageReminders(true)
                    .emailActivityDigest(false)
                    .emailProductAnnouncements(false)
                    .build();
            userSettingsRepository.save(settings);
        }

        log.info("""
                
                ================================================================================
                [DATA INITIALIZER] Default SuperAdmin Account Successfully Seeded!
                  - Username:     {}
                  - Email:        {}
                  - Password:     {}
                  - Role:         {}
                  - Status:       ACTIVE (Email Verified)
                ================================================================================
                """, normalizedUsername, normalizedEmail, adminPassword, Role.ROLE_ADMIN);
    }
}
