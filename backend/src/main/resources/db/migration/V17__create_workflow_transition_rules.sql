-- ADR-012 §D (Phase 23): 프로젝트별 워크플로우(상태 전이) 규칙.
--
-- 범용 워크플로우 엔진이 아니다 — "상태 A에서 상태 B로 전이 가능한가(참/거짓) + 최소 역할"만
-- 표현하는 화이트리스트 매트릭스다. 대상은 REQUIREMENT/ISSUE 두 target_type만(TEST_CASE 제외).
--
-- 적용 규칙(보수적 기본값, §D.3): 프로젝트+target_type에 규칙이 하나도 없으면 자유 전이(하위호환).
-- 규칙이 하나 이상 있으면 화이트리스트 모드로 전환된다. 이 판단은 서비스 레이어
-- (WorkflowTransitionPolicy)가 담당하므로 이 테이블 자체에는 "모드" 컬럼이 없다.

CREATE TABLE workflow_transition_rules (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    allowed_role VARCHAR(20) NULL,
    created_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_workflow_transition_rules_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_workflow_transition_rules_allowed_role CHECK (
        allowed_role IS NULL OR allowed_role IN ('PROJECT_ADMIN','MEMBER','VIEWER')
    ),
    CONSTRAINT chk_workflow_transition_rules_from_ne_to CHECK (from_status <> to_status),
    CONSTRAINT uq_workflow_transition_rules_project_target_from_to
        UNIQUE (project_id, target_type, from_status, to_status)
);

CREATE INDEX idx_workflow_transition_rules_project_target
    ON workflow_transition_rules(project_id, target_type);
