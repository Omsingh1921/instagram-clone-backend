-- Instagram Clone: Staged Registration Verification Tickets Migration (V10)
-- Stores cryptographically hashed, short-lived signup proofs bound to verified emails

CREATE TABLE IF NOT EXISTS signup_verification_tickets (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_signup_tickets_email (email),
    INDEX idx_signup_tickets_hash (token_hash),
    INDEX idx_signup_tickets_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
