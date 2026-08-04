-- audit_logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_audit_logs_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE','RELEASE','PROJECT','USER','TRACEABILITY_LINK')),
    CONSTRAINT chk_audit_logs_action CHECK (action IN ('CREATE','UPDATE','STATUS_CHANGE','DELETE','APPROVE','REJECT'))
);

CREATE INDEX idx_audit_logs_project_id ON audit_logs(project_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
