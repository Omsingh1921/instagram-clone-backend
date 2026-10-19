package com.instagramclone.user.repository;

import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.DigestFrequency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUserId(UUID userId);

    Page<UserSettings> findByEmailActivityDigestTrueAndDigestFrequency(
            DigestFrequency digestFrequency, Pageable pageable);
}
