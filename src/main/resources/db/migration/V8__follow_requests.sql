-- Instagram Clone: Follow Requests Migration (V8)

CREATE TABLE IF NOT EXISTS follow_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    requester_id BINARY(16) NOT NULL,
    target_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_follow_req_requester
        FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_req_target
        FOREIGN KEY (target_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_follow_req_pair UNIQUE (requester_id, target_id),

    INDEX idx_follow_req_requester (requester_id),
    INDEX idx_follow_req_target (target_id),
    INDEX idx_follow_req_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
