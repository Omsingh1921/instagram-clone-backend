-- Migration V4: Media Metadata Table for Cloudinary Storage & Lifecycle Management
CREATE TABLE IF NOT EXISTS media (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(700) NOT NULL,
    resource_type VARCHAR(50) NOT NULL DEFAULT 'image',
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    format VARCHAR(20) NULL,
    width INT NULL,
    height INT NULL,
    duration DOUBLE NULL,
    file_size BIGINT NULL,
    folder VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_media_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_media_owner (owner_id),
    INDEX idx_media_public_id (public_id),
    INDEX idx_media_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
