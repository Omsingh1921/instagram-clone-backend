package com.instagramclone.mail.entity;

import com.instagramclone.common.entity.BaseAuditableEntity;
import com.instagramclone.mail.enums.EmailType;
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
@Table(name = "dynamic_email_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DynamicEmailTemplate extends BaseAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, unique = true, length = 50)
    private EmailType emailType;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "subject_template", nullable = false, length = 255)
    private String subjectTemplate;

    @Column(name = "body_html_template", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String bodyHtmlTemplate;

    @Column(name = "body_text_template", columnDefinition = "TEXT")
    private String bodyTextTemplate;

    @Column(name = "available_placeholders", length = 500)
    private String availablePlaceholders;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
