package com.instagramclone.mail.repository;

import com.instagramclone.mail.entity.DynamicEmailTemplate;
import com.instagramclone.mail.enums.EmailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DynamicEmailTemplateRepository extends JpaRepository<DynamicEmailTemplate, UUID> {

    Optional<DynamicEmailTemplate> findByEmailType(EmailType emailType);

    boolean existsByEmailType(EmailType emailType);
}
