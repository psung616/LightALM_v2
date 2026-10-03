-- ADR-009: projectKey 검증 제약 완화 (VARCHAR(10) -> VARCHAR(20))
ALTER TABLE projects ALTER COLUMN project_key TYPE VARCHAR(20);
