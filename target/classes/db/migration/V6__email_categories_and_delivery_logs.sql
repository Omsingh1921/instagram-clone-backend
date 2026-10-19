-- Instagram Clone: Multi-Category Email Management System Migration (V6)
-- Extends user_settings with granular email preferences and creates email_delivery_logs

-- 1. Extend user_settings table with email notification preferences
ALTER TABLE user_settings
ADD COLUMN email_social_follows BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN email_social_likes BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN email_social_comments BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN email_message_reminders BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN email_activity_digest BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN email_product_announcements BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN digest_frequency VARCHAR(20) NOT NULL DEFAULT 'WEEKLY';

-- 2. Create email_delivery_logs table for tracking delivery states, retries, and audits
CREATE TABLE IF NOT EXISTS email_delivery_logs (
    id UUID NOT NULL PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    user_id UUID NULL,
    email_type VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INT NOT NULL DEFAULT 1,
    last_error TEXT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_delivery_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL);
