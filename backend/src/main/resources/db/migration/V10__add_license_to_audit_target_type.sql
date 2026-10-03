-- ADR-011: audit_logs.target_type CHECK 제약에 'LICENSE' 추가 (라이센스 업로드 감사 기록용)
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_target_type;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_target_type_check;
ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_target_type
    CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE','RELEASE','PROJECT','USER','TRACEABILITY_LINK','LICENSE'));
