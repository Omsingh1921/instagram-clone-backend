-- Migration V17: Dynamic Email Configurations by Category / Purpose
CREATE TABLE IF NOT EXISTS dynamic_email_configs (
    id UUID NOT NULL PRIMARY KEY,
    category VARCHAR(50) NOT NULL UNIQUE,
    purpose_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    from_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    reply_to VARCHAR(255) NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
