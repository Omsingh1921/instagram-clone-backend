package com.instagramclone.mail.repository;

import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, UUID> {

    Page<EmailDeliveryLog> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    Page<EmailDeliveryLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<EmailDeliveryLog> findByUserIdAndEmailTypeOrderByCreatedAtDesc(UUID userId, EmailType emailType);

    long countByStatus(EmailDeliveryStatus status);
}
