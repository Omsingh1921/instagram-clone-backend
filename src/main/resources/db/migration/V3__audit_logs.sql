-- V3 Audit Logs Migration
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID NOT NULL,
    user_id UUID NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NULL,
    ip_address VARCHAR(45) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id));
