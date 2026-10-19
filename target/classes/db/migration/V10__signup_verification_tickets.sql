-- Instagram Clone: Staged Registration Verification Tickets Migration (V10)
-- Stores cryptographically hashed, short-lived signup proofs bound to verified emails

CREATE TABLE IF NOT EXISTS signup_verification_tickets (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
