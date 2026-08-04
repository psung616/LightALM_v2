-- approval_requests
CREATE TABLE approval_requests (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    requested_status VARCHAR(20) NOT NULL,
    requested_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    comment TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    CONSTRAINT chk_approval_requests_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_approval_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);

CREATE INDEX idx_approval_requests_project_id ON approval_requests(project_id);
CREATE INDEX idx_approval_requests_target ON approval_requests(target_type, target_id);
