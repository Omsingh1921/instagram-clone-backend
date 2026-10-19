package com.instagramclone.auth.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a short-lived, single-use proof of email OTP verification
 * issued prior to account and profile creation.
 */
@Entity
@Table(name = "signup_verification_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class SignupVerificationTicket extends BaseCreatedEntity {
@Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isConsumed();
    }
}
