package com.instagramclone.mail.repository;

import com.instagramclone.mail.entity.DynamicEmailConfig;
import com.instagramclone.mail.enums.EmailCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DynamicEmailConfigRepository extends JpaRepository<DynamicEmailConfig, UUID> {

    Optional<DynamicEmailConfig> findByCategory(EmailCategory category);

    boolean existsByCategory(EmailCategory category);
}
