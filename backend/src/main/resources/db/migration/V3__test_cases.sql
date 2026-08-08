-- projects: 테스트케이스 키 채번용 시퀀스 컬럼 추가
ALTER TABLE projects ADD COLUMN test_case_seq INTEGER NOT NULL DEFAULT 0;

-- test_cases
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    requirement_id BIGINT REFERENCES requirements(id) ON DELETE SET NULL,
    tc_key VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    preconditions TEXT,
    steps TEXT NOT NULL,
    expected_result TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_test_cases_tc_key UNIQUE (tc_key),
    CONSTRAINT chk_test_cases_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_test_cases_status CHECK (status IN ('DRAFT','READY','DEPRECATED'))
);

CREATE INDEX idx_test_cases_project_id ON test_cases(project_id);
CREATE INDEX idx_test_cases_requirement_id ON test_cases(requirement_id);

-- test_runs
-- NOTE: release_id (FK -> releases.id) is intentionally omitted here because the `releases` table
-- does not exist yet (it is created in Phase 13). Phase 13's migration will
-- ALTER TABLE test_runs ADD COLUMN release_id BIGINT REFERENCES releases(id) ON DELETE SET NULL.
CREATE TABLE test_runs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_test_runs_status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED'))
);

CREATE INDEX idx_test_runs_project_id ON test_runs(project_id);

-- test_run_results
CREATE TABLE test_run_results (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    test_case_id BIGINT NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    result VARCHAR(20) NOT NULL DEFAULT 'NOT_RUN',
    actual_result TEXT,
    executed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    executed_at TIMESTAMP,
    CONSTRAINT uq_test_run_results UNIQUE (test_run_id, test_case_id),
    CONSTRAINT chk_test_run_results_result CHECK (result IN ('NOT_RUN','PASS','FAIL','BLOCKED','SKIPPED'))
);

CREATE INDEX idx_test_run_results_test_run_id ON test_run_results(test_run_id);

-- v2 확장: traceability_links/comments가 TEST_CASE를 소스/타겟으로 허용하도록 CHECK 제약 확장
-- IF EXISTS + 두 이름 모두 시도: V1이 명시적 이름 없이 적용된 DB(Postgres 자동 생성 이름)도 대응
ALTER TABLE traceability_links DROP CONSTRAINT IF EXISTS chk_traceability_links_source_type;
ALTER TABLE traceability_links DROP CONSTRAINT IF EXISTS traceability_links_source_type_check;
ALTER TABLE traceability_links ADD CONSTRAINT chk_traceability_links_source_type CHECK (source_type IN ('REQUIREMENT','ISSUE','TEST_CASE'));

ALTER TABLE traceability_links DROP CONSTRAINT IF EXISTS chk_traceability_links_target_type;
ALTER TABLE traceability_links DROP CONSTRAINT IF EXISTS traceability_links_target_type_check;
ALTER TABLE traceability_links ADD CONSTRAINT chk_traceability_links_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE'));

ALTER TABLE comments DROP CONSTRAINT IF EXISTS chk_comments_target_type;
ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_target_type_check;
ALTER TABLE comments ADD CONSTRAINT chk_comments_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE'));
