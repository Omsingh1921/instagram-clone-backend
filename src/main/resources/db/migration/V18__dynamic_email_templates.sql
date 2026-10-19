-- Migration V18: Dynamic Email Templates for Admin Customization
CREATE TABLE IF NOT EXISTS dynamic_email_templates (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email_type VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_html_template MEDIUMTEXT NOT NULL,
    body_text_template TEXT NULL,
    available_placeholders VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_dynamic_email_template_type (email_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
