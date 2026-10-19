-- Migration V18: Dynamic Email Templates for Admin Customization
CREATE TABLE IF NOT EXISTS dynamic_email_templates (
    id UUID NOT NULL PRIMARY KEY,
    email_type VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_html_template TEXT NOT NULL,
    body_text_template TEXT NULL,
    available_placeholders VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
