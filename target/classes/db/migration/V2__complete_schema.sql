-- Instagram Clone: Complete Domain Schema Migration (V2)
-- Conforms to Database Design Specification and UUID Identifier Standard (standard.md)

-- 0. Identity Extension: Role Column

ALTER TABLE users
ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER';


-- 1. Social Graph: Follows Table

CREATE TABLE IF NOT EXISTS follows (
    id BINARY(16) NOT NULL PRIMARY KEY,
    follower_id BINARY(16) NOT NULL,
    following_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_follows_following
        FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_follows UNIQUE (follower_id, following_id),

    INDEX idx_follows_follower (follower_id),
    INDEX idx_follows_following (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. Content: Posts Table

CREATE TABLE IF NOT EXISTS posts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    caption TEXT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_posts_user_created (user_id, created_at),
    INDEX idx_posts_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. Content: Post Media Table

CREATE TABLE IF NOT EXISTS post_media (
    id BINARY(16) NOT NULL PRIMARY KEY,
    post_id BINARY(16) NOT NULL,
    media_url VARCHAR(700) NOT NULL,
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    display_order INT NOT NULL DEFAULT 0,
    width INT NULL,
    height INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,

    INDEX idx_post_media_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. Engagement: Likes Table

CREATE TABLE IF NOT EXISTS likes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_likes_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,

    CONSTRAINT uq_likes UNIQUE (user_id, post_id),

    INDEX idx_likes_post (post_id),
    INDEX idx_likes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. Engagement: Comments Table

CREATE TABLE IF NOT EXISTS comments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    post_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    parent_comment_id BINARY(16) NULL,
    text TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,

    INDEX idx_comments_post_created (post_id, created_at),
    INDEX idx_comments_user (user_id),
    INDEX idx_comments_parent (parent_comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 6. Engagement: Saved Posts Table

CREATE TABLE IF NOT EXISTS saved_posts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_saved_posts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_saved_posts_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,

    CONSTRAINT uq_saved_posts UNIQUE (user_id, post_id),

    INDEX idx_saved_posts_user (user_id),
    INDEX idx_saved_posts_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 7. Notifications Table

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    recipient_id BINARY(16) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    type VARCHAR(30) NOT NULL,
    post_id BINARY(16) NULL,
    comment_id BINARY(16) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_notifications_recipient (recipient_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 8. Messaging: Conversations Table

CREATE TABLE IF NOT EXISTS conversations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_conversations_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 9. Messaging: Conversation Members Table

CREATE TABLE IF NOT EXISTS conversation_members (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_at DATETIME NULL,

    CONSTRAINT fk_cm_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,

    CONSTRAINT fk_cm_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_conversation_member UNIQUE (conversation_id, user_id),

    INDEX idx_cm_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 10. Messaging: Messages Table

CREATE TABLE IF NOT EXISTS messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    content TEXT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    media_url VARCHAR(700) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,

    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_messages_conversation_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 11. Ephemeral: Stories Table

CREATE TABLE IF NOT EXISTS stories (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    media_url VARCHAR(700) NOT NULL,
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stories_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_stories_user_expires (user_id, expires_at),
    INDEX idx_stories_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 12. Ephemeral: Story Views Table

CREATE TABLE IF NOT EXISTS story_views (
    id BINARY(16) NOT NULL PRIMARY KEY,
    story_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_views_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,

    CONSTRAINT fk_story_views_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_story_views UNIQUE (story_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 13. Video: Reels Table

CREATE TABLE IF NOT EXISTS reels (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    video_url VARCHAR(700) NOT NULL,
    thumbnail_url VARCHAR(700) NULL,
    caption TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reels_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_reels_user_created (user_id, created_at),
    INDEX idx_reels_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 14. Settings: User Blocks Table

CREATE TABLE IF NOT EXISTS user_blocks (
    id BINARY(16) NOT NULL PRIMARY KEY,
    blocker_id BINARY(16) NOT NULL,
    blocked_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_blocks_blocker
        FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_blocks_blocked
        FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_user_blocks UNIQUE (blocker_id, blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 15. Settings: User Settings Table

CREATE TABLE IF NOT EXISTS user_settings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    notify_likes BOOLEAN NOT NULL DEFAULT TRUE,
    notify_comments BOOLEAN NOT NULL DEFAULT TRUE,
    notify_follows BOOLEAN NOT NULL DEFAULT TRUE,
    notify_messages BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 16. Admin & Moderation: Reports Table

CREATE TABLE IF NOT EXISTS reports (
    id BINARY(16) NOT NULL PRIMARY KEY,
    reporter_id BINARY(16) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BINARY(16) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reviewer_id BINARY(16) NULL,
    resolution_notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,

    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reports_reviewer
        FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_reports_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;