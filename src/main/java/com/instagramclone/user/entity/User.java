package com.instagramclone.user.entity;

import com.instagramclone.common.entity.BaseAuditableEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email_slot", columnNames = { "email", "account_slot" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseAuditableEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "account_slot", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TINYINT)
    @Builder.Default
    private Integer accountSlot = 1;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Profile profile;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Override
    public void softDelete() {
        super.softDelete();
        this.status = UserStatus.DELETED;
    }

    @Override
    public void restore() {
        super.restore();
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        setIsActive(false);
        this.status = UserStatus.DEACTIVATED;
    }

    public boolean isDeletedOrDeactivated() {
        return Boolean.TRUE.equals(getIsDeleted())
                || Boolean.FALSE.equals(getIsActive())
                || status == UserStatus.DELETED
                || status == UserStatus.DEACTIVATED
                || status == UserStatus.INACTIVE
                || status == UserStatus.SUSPENDED;
    }

    public boolean isWithinDeletionGracePeriod(int gracePeriodDays) {
        if (!Boolean.TRUE.equals(getIsDeleted()) || getDeletedAt() == null) {
            return false;
        }
        return getDeletedAt().plusDays(gracePeriodDays).isAfter(LocalDateTime.now());
    }

    public boolean isPermanentlyDeleted(int gracePeriodDays) {
        if (!Boolean.TRUE.equals(getIsDeleted())) {
            return false;
        }
        if (getDeletedAt() == null) {
            return true;
        }
        return !getDeletedAt().plusDays(gracePeriodDays).isAfter(LocalDateTime.now());
    }
}
