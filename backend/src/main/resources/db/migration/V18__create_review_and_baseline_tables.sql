-- ADR-008 (Phase 16): 리뷰 사이클(Review Cycle) + 베이스라인(Baseline).
--
-- review_cycles/review_participants: 기존 승인 워크플로우(approval_requests, Phase 15)와는
-- 완전히 별개 기능이다. 여러 검토자가 각자 승인/반려/의견만 남기는 "의견 수집" 용도이며,
-- 참여자의 decision 값이 대상(요구사항/이슈)의 status를 자동으로 바꾸는 로직은 서비스 레이어에도
-- 두지 않는다(03-data-model.md §3.19, 01-scope.md §1.3 원칙).
--
-- baselines/baseline_items: 베이스라인 생성 시점의 대상 필드 값을 JSONB 스냅샷으로 얼려서 저장한다.
-- diff(비교) 결과는 저장하지 않고, 조회 시점에 서비스 레이어가 snapshot과 현재 값을 비교해 계산한다.

CREATE TABLE review_cycles (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    closed_at TIMESTAMP NULL,
    CONSTRAINT chk_review_cycles_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_review_cycles_status CHECK (status IN ('OPEN','CLOSED'))
);

CREATE INDEX idx_review_cycles_target ON review_cycles(project_id, target_type, target_id);

CREATE TABLE review_participants (
    id BIGSERIAL PRIMARY KEY,
    review_cycle_id BIGINT NOT NULL REFERENCES review_cycles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    decision VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment TEXT NULL,
    decided_at TIMESTAMP NULL,
    CONSTRAINT chk_review_participants_decision CHECK (decision IN ('PENDING','APPROVE','REJECT','COMMENT_ONLY')),
    CONSTRAINT uq_review_participants_cycle_user UNIQUE (review_cycle_id, user_id)
);

CREATE INDEX idx_review_participants_cycle ON review_participants(review_cycle_id);

CREATE TABLE baselines (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    created_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_baselines_project ON baselines(project_id);

CREATE TABLE baseline_items (
    id BIGSERIAL PRIMARY KEY,
    baseline_id BIGINT NOT NULL REFERENCES baselines(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    snapshot JSONB NOT NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_baseline_items_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE')),
    CONSTRAINT uq_baseline_items_baseline_target UNIQUE (baseline_id, target_type, target_id)
);

CREATE INDEX idx_baseline_items_baseline ON baseline_items(baseline_id);
