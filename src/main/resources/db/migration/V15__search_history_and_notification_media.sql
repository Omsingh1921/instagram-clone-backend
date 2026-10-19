-- V15: Search History & Notification Media Preview Migration

CREATE TABLE IF NOT EXISTS search_histories (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    query VARCHAR(255) NULL,
    target_user_id BINARY(16) NULL,
    search_type VARCHAR(20) NOT NULL DEFAULT 'KEYWORD',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_search_history_target_user
        FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_search_history_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE notifications
    ADD COLUMN media_preview_url VARCHAR(700) NULL;
