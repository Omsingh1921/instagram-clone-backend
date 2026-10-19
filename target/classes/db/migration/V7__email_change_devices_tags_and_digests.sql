-- Instagram Clone: Email Change, Known Devices, and Media Tags Migration (V7)

-- 1. Create email_change_requests table for secure two-step email address modification
CREATE TABLE IF NOT EXISTS email_change_requests (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    new_email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_change_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);

-- 2. Create user_known_devices table for privacy-conscious login security and new-device detection
CREATE TABLE IF NOT EXISTS user_known_devices (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    device_identifier VARCHAR(64) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    last_ip_address VARCHAR(45) NULL,
    last_login_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_known_device_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_device UNIQUE (user_id, device_identifier));

-- 3. Create post_media_tags table for user tagging in photos and reels
CREATE TABLE IF NOT EXISTS post_media_tags (
    id UUID NOT NULL PRIMARY KEY,
    post_media_id UUID NOT NULL,
    tagged_user_id UUID NOT NULL,
    x_percent DOUBLE PRECISION NULL,
    y_percent DOUBLE PRECISION NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_media_tags_media
        FOREIGN KEY (post_media_id) REFERENCES post_media(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_tags_user
        FOREIGN KEY (tagged_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_media_tagged_user UNIQUE (post_media_id, tagged_user_id));
