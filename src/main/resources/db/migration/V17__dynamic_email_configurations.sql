-- Migration V17: Dynamic Email Configurations by Category / Purpose
CREATE TABLE IF NOT EXISTS dynamic_email_configs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    category VARCHAR(50) NOT NULL UNIQUE,
    purpose_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    from_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    reply_to VARCHAR(255) NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_dynamic_email_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
