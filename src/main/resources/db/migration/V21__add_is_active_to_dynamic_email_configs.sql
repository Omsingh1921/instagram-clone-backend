-- Migration V21: Add missing is_active column to dynamic_email_configs
ALTER TABLE dynamic_email_configs
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
