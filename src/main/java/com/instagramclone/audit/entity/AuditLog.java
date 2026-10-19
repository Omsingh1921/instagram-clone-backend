package com.instagramclone.audit.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuditLog extends BaseCreatedEntity {
@Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 50, nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
