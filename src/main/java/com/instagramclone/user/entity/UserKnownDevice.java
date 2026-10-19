package com.instagramclone.user.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_known_devices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_device", columnNames = {"user_id", "device_identifier"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class UserKnownDevice extends BaseCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_identifier", nullable = false, length = 64)
    private String deviceIdentifier;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "last_ip_address", length = 45)
    private String lastIpAddress;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;
}
