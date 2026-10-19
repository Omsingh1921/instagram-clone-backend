-- Instagram Clone: Email Verification and Password Reset Tokens Migration (V5)
-- Conforms to Database Design Specification and UUID Identifier Standard

-- 1. Extend Users table with email verification status
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN email_verified_at TIMESTAMP NULL;

-- 2. Email Verification Tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);

-- 3. Password Reset Tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);
