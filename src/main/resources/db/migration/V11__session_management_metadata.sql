-- Instagram Clone: Session Management & Tracking Metadata Migration (V11)
-- Adds stable session identity, client metadata, and activity tracking to refresh_tokens

-- 1. Add session_id column initially as nullable to allow backfilling of existing records
ALTER TABLE refresh_tokens ADD COLUMN session_id BINARY(16) NULL AFTER user_id;

-- 2. Backfill existing legacy records: assign session_id = id so each has a valid, unique 16-byte session identifier
UPDATE refresh_tokens SET session_id = id WHERE session_id IS NULL;

-- 3. Now enforce NOT NULL constraint on session_id
ALTER TABLE refresh_tokens MODIFY COLUMN session_id BINARY(16) NOT NULL;

-- 4. Add metadata columns for device name, IP address, user agent, and activity tracking
ALTER TABLE refresh_tokens ADD COLUMN device_name VARCHAR(100) NULL AFTER token_hash;
ALTER TABLE refresh_tokens ADD COLUMN ip_address VARCHAR(45) NULL AFTER device_name;
ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(500) NULL AFTER ip_address;
ALTER TABLE refresh_tokens ADD COLUMN last_activity_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

-- 5. Add indexes for active session retrieval and session-level revocation
ALTER TABLE refresh_tokens ADD INDEX idx_refresh_tokens_session_id (session_id);
ALTER TABLE refresh_tokens ADD INDEX idx_refresh_tokens_user_session (user_id, session_id);
ALTER TABLE refresh_tokens ADD INDEX idx_refresh_tokens_user_active (user_id, revoked_at, expires_at);
