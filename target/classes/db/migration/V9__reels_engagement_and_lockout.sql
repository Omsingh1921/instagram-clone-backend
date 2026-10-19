-- Instagram Clone: Reels Engagement and Account Lockout Migration (V9)

-- 1. Create reel_likes table
CREATE TABLE IF NOT EXISTS reel_likes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    reel_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_likes_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT uk_reel_likes UNIQUE (user_id, reel_id),

    INDEX idx_reel_likes_reel (reel_id),
    INDEX idx_reel_likes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create reel_comments table
CREATE TABLE IF NOT EXISTS reel_comments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    reel_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    text TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_comments_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_reel_comments_reel_created (reel_id, created_at),
    INDEX idx_reel_comments_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create reel_views table for durable deduplicated view tracking
CREATE TABLE IF NOT EXISTS reel_views (
    id BINARY(16) NOT NULL PRIMARY KEY,
    reel_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reel_views_reel
        FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,

    CONSTRAINT fk_reel_views_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uk_reel_views UNIQUE (reel_id, user_id),

    INDEX idx_reel_views_reel (reel_id),
    INDEX idx_reel_views_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Add progressive account lockout columns to users table
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN lockout_until DATETIME NULL;

-- 5. Add reel_id column to notifications table with foreign key and indices
ALTER TABLE notifications ADD COLUMN reel_id BINARY(16) NULL;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_reel FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE;
ALTER TABLE notifications ADD INDEX idx_notifications_reel (reel_id);
ALTER TABLE notifications ADD INDEX idx_notifications_dedup (recipient_id, actor_id, type, is_read);
