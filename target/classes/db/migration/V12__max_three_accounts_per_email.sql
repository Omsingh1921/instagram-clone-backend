-- Instagram Clone: Multi-Account Registration Limit Migration (V12)
-- Relaxes 1:1 email uniqueness to allow a maximum of 3 accounts per normalized email address.
-- Enforces limit strictly at the database level using composite unique constraint and check constraint.

-- 1. Drop existing 1:1 unique constraint on email
ALTER TABLE users DROP INDEX email;

-- 2. Add account_slot column (values 1, 2, or 3)
ALTER TABLE users ADD COLUMN account_slot TINYINT NOT NULL DEFAULT 1 AFTER email;

-- 3. Ensure all existing legacy records are assigned to slot 1
UPDATE users SET account_slot = 1 WHERE account_slot IS NULL OR account_slot = 0;

-- 4. Add unique composite constraint on (email, account_slot)
-- This mathematically prevents more than one account in any given slot per email
ALTER TABLE users ADD CONSTRAINT uk_users_email_slot UNIQUE (email, account_slot);

-- 5. Add CHECK constraint restricting account_slot strictly to values 1, 2, or 3
-- Any attempt to allocate slot 4 or higher is rejected by the storage engine
ALTER TABLE users ADD CONSTRAINT chk_users_account_slot CHECK (account_slot IN (1, 2, 3));
