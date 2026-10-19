-- Instagram Clone: Reels Engagement and Account Lockout Migration (V9)

-- 1. Create reel_likes table
CREATE TABLE IF NOT EXISTS reel_likes (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_likes_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT uk_reel_likes UNIQUE (user_id, reel_id));

-- 2. Create reel_comments table
CREATE TABLE IF NOT EXISTS reel_comments (
    id UUID NOT NULL PRIMARY KEY,
    reel_id UUID NOT NULL,
    user_id UUID NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_comments_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);

-- 3. Create reel_views table for durable deduplicated view tracking
CREATE TABLE IF NOT EXISTS reel_views (
    id UUID NOT NULL PRIMARY KEY,
    reel_id UUID NOT NULL,
    user_id UUID NOT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_views_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_views_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uk_reel_views UNIQUE (reel_id, user_id));

-- 4. Add progressive account lockout columns to users table
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN lockout_until TIMESTAMP NULL;

-- 5. Add reel_id column to notifications table with foreign key and indices
ALTER TABLE notifications ADD COLUMN reel_id UUID NULL;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_reel FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_notifications_reel ON notifications (reel_id);
CREATE INDEX IF NOT EXISTS idx_notifications_dedup ON notifications (recipient_id, actor_id, type, is_read);
