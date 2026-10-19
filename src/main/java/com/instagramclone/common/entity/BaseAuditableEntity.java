package com.instagramclone.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Full audit mapped superclass for mutable entities.
 * Extends BaseCreatedEntity and automatically manages updated_at audit timestamp,
 * active status, and soft-delete state (patterned after Renaissance AuditData).
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseAuditableEntity extends BaseCreatedEntity {

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Flag indicating whether the entity is deleted. Defaults to {@code false}.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = Boolean.FALSE;

    /**
     * Flag indicating whether the entity is active. Defaults to {@code true}.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    /**
     * Timestamp when the entity was soft deleted. Null if entity is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersistAuditData() {
        if (isDeleted == null) {
            isDeleted = Boolean.FALSE;
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    public void softDelete() {
        this.isDeleted = Boolean.TRUE;
        this.isActive = Boolean.FALSE;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = Boolean.FALSE;
        this.isActive = Boolean.TRUE;
        this.deletedAt = null;
    }
}
