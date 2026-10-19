-- Instagram Clone: Follow Requests Migration (V8)

CREATE TABLE IF NOT EXISTS follow_requests (
    id UUID NOT NULL PRIMARY KEY,
    requester_id UUID NOT NULL,
    target_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_follow_req_requester
        FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_req_target
        FOREIGN KEY (target_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_follow_req_pair UNIQUE (requester_id, target_id));
