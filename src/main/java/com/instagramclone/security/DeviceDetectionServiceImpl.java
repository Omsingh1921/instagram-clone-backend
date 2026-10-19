package com.instagramclone.security;

import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserKnownDevice;
import com.instagramclone.user.repository.UserKnownDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceDetectionServiceImpl implements IDeviceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceDetectionServiceImpl.class);
    private final UserKnownDeviceRepository userKnownDeviceRepository;

    @Override
    @Transactional
    public DeviceInspectionResult inspectDevice(User user, String userAgent, String ipAddress) {
        String effectiveIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        String coarseDeviceName = resolveCoarseDeviceName(userAgent);
        String deviceIdentifier = computeDeviceIdentifier(user.getId().toString(), coarseDeviceName);

        Optional<UserKnownDevice> existingDeviceOpt =
                userKnownDeviceRepository.findByUserIdAndDeviceIdentifier(user.getId(), deviceIdentifier);

        if (existingDeviceOpt.isPresent()) {
            UserKnownDevice existing = existingDeviceOpt.get();
            existing.setLastLoginAt(LocalDateTime.now());
            existing.setLastIpAddress(effectiveIp);
            userKnownDeviceRepository.save(existing);
            return new DeviceInspectionResult(false, coarseDeviceName, deviceIdentifier, effectiveIp);
        }

        // Device is not recognized in user's known devices table
        long priorDevicesCount = userKnownDeviceRepository.countByUserId(user.getId());

        UserKnownDevice newDevice = UserKnownDevice.builder()
                .user(user)
                .deviceIdentifier(deviceIdentifier)
                .deviceName(coarseDeviceName)
                .lastIpAddress(effectiveIp)
                .lastLoginAt(LocalDateTime.now())
                .build();
        userKnownDeviceRepository.save(newDevice);

        // Send alert ONLY if user already has prior established devices
        // (Avoids false-positive alert on baseline/first login)
        boolean alertJustified = priorDevicesCount > 0;
        if (alertJustified) {
            log.info("New unrecognized device [{}] detected for user [{}] from IP [{}]",
                    coarseDeviceName, user.getUsername(), effectiveIp);
        }

        return new DeviceInspectionResult(alertJustified, coarseDeviceName, deviceIdentifier, effectiveIp);
    }

    @Override
    public String resolveCoarseDeviceName(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Web Browser (Direct Client)";
        }

        String os;
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iOS")) {
            os = "iOS";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Macintosh") || ua.contains("Mac OS")) {
            os = "macOS";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        } else {
            os = "Unknown OS";
        }

        String browser;
        if (ua.contains("Instagram")) {
            browser = "Instagram App";
        } else if (ua.contains("Edg")) {
            browser = "Microsoft Edge";
        } else if (ua.contains("Chrome") && !ua.contains("Chromium")) {
            browser = "Google Chrome";
        } else if (ua.contains("Safari") && !ua.contains("Chrome")) {
            browser = "Apple Safari";
        } else if (ua.contains("Firefox")) {
            browser = "Mozilla Firefox";
        } else {
            browser = "Browser";
        }

        return browser + " (" + os + ")";
    }

    private String computeDeviceIdentifier(String userId, String coarseDeviceName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((userId + ":" + coarseDeviceName.toLowerCase()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
