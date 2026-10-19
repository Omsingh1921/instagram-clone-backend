package com.instagramclone.user.repository;

import com.instagramclone.user.entity.UserKnownDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserKnownDeviceRepository extends JpaRepository<UserKnownDevice, UUID> {

    Optional<UserKnownDevice> findByUserIdAndDeviceIdentifier(UUID userId, String deviceIdentifier);

    long countByUserId(UUID userId);
}
