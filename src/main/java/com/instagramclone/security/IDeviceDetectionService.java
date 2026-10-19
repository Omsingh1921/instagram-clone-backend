package com.instagramclone.security;

import com.instagramclone.user.entity.User;

public interface IDeviceDetectionService {

    record DeviceInspectionResult(
            boolean newDeviceAlertJustified,
            String deviceName,
            String deviceIdentifier,
            String ipAddress
    ) {}

    /**
     * Inspects login metadata in a privacy-conscious manner using coarse User-Agent attributes.
     * Evaluates existing known device history and returns whether a NEW_DEVICE_LOGIN alert is justified.
     */
    DeviceInspectionResult inspectDevice(User user, String userAgent, String ipAddress);

    /**
     * Resolves a coarse, privacy-safe device name from a User-Agent string.
     */
    String resolveCoarseDeviceName(String userAgent);
}
