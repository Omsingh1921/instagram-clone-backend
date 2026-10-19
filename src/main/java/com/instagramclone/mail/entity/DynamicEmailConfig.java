package com.instagramclone.mail.entity;

import com.instagramclone.common.entity.BaseAuditableEntity;
import com.instagramclone.mail.enums.EmailCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "dynamic_email_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DynamicEmailConfig extends BaseAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, unique = true, length = 50)
    private EmailCategory category;

    @Column(name = "purpose_name", nullable = false, length = 100)
    private String purposeName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "from_email", nullable = false, length = 255)
    private String fromEmail;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "reply_to", length = 255)
    private String replyTo;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
}
