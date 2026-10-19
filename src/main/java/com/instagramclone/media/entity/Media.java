package com.instagramclone.media.entity;

import com.instagramclone.common.entity.BaseAuditableEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reusable Media entity adhering to Sections 1, 11 & 12 (Database Design & Public ID Strategy).
 * Persists Cloudinary asset metadata without storing raw binary data in MySQL.
 */
@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Media extends BaseAuditableEntity {
@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private User owner;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Column(name = "secure_url", nullable = false, length = 700)
    private String secureUrl;

    @Column(name = "resource_type", nullable = false, length = 50)
    @Builder.Default
    private String resourceType = "image";

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "folder", length = 255)
    private String folder;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "READY";
}
