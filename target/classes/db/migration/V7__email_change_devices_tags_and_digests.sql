-- Instagram Clone: Email Change, Known Devices, and Media Tags Migration (V7)

-- 1. Create email_change_requests table for secure two-step email address modification
CREATE TABLE IF NOT EXISTS email_change_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    new_email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_change_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_email_change_user (user_id),
    INDEX idx_email_change_status (status),
    INDEX idx_email_change_token (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create user_known_devices table for privacy-conscious login security and new-device detection
CREATE TABLE IF NOT EXISTS user_known_devices (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    device_identifier VARCHAR(64) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    last_ip_address VARCHAR(45) NULL,
    last_login_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_known_device_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_device UNIQUE (user_id, device_identifier),

    INDEX idx_user_device_user (user_id),
    INDEX idx_user_device_identifier (device_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create post_media_tags table for user tagging in photos and reels
CREATE TABLE IF NOT EXISTS post_media_tags (
    id BINARY(16) NOT NULL PRIMARY KEY,
    post_media_id BINARY(16) NOT NULL,
    tagged_user_id BINARY(16) NOT NULL,
    x_percent DOUBLE NULL,
    y_percent DOUBLE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_media_tags_media
        FOREIGN KEY (post_media_id) REFERENCES post_media(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_tags_user
        FOREIGN KEY (tagged_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_media_tagged_user UNIQUE (post_media_id, tagged_user_id),

    INDEX idx_media_tags_media (post_media_id),
    INDEX idx_media_tags_user (tagged_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
