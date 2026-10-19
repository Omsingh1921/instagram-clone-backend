-- V15: Search History & Notification Media Preview Migration

CREATE TABLE IF NOT EXISTS search_histories (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    query VARCHAR(255) NULL,
    target_user_id UUID NULL,
    search_type VARCHAR(20) NOT NULL DEFAULT 'KEYWORD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_search_history_target_user
        FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE);

ALTER TABLE notifications
    ADD COLUMN media_preview_url VARCHAR(700) NULL;
