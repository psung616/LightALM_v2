-- ADR-012 §C (Phase 22): 프로젝트별 열거형(Enumeration) 설정.
--
-- 범위(§C.1):
--   포함  — (1) PRIORITY의 프로젝트별 값 확장(DB CHECK 완화 + 애플리케이션 레벨 검증으로 이전)
--           (2) 커스텀 필드(Phase 20)의 SINGLE_SELECT/MULTI_SELECT 전용 선택지 목록 관리
--   제외  — REQUIREMENT_STATUS/ISSUE_STATUS/TEST_CASE_STATUS 값 자체의 추가/삭제.
--           base_enum CHECK는 스키마상 4개 값을 모두 허용하도록 느슨하게 두되(향후 확장 여지),
--           서비스 레이어(EnumerationSetService)가 PRIORITY/NULL 외 값의 집합 생성을 거부한다.

CREATE TABLE project_enumeration_sets (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    enum_key VARCHAR(50) NOT NULL,
    base_enum VARCHAR(30) NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_enumeration_sets_base_enum CHECK (
        base_enum IS NULL OR base_enum IN ('PRIORITY','REQUIREMENT_STATUS','ISSUE_STATUS','TEST_CASE_STATUS')
    ),
    CONSTRAINT uq_project_enumeration_sets_project_key UNIQUE (project_id, enum_key)
);

CREATE INDEX idx_project_enumeration_sets_project_id ON project_enumeration_sets(project_id);

CREATE TABLE project_enumeration_values (
    id BIGSERIAL PRIMARY KEY,
    enumeration_set_id BIGINT NOT NULL REFERENCES project_enumeration_sets(id) ON DELETE CASCADE,
    value_key VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_enumeration_values_status CHECK (status IN ('ACTIVE','DEPRECATED')),
    CONSTRAINT uq_project_enumeration_values_set_key UNIQUE (enumeration_set_id, value_key)
);

CREATE INDEX idx_project_enumeration_values_set_id ON project_enumeration_values(enumeration_set_id);

-- §C.3: priority 컬럼의 DB CHECK 제약을 제거한다. 검증 책임은 이제
-- EnumerationValueValidator(com.lightalm.enumeration.service, 애플리케이션 레벨)로 이전된다.
-- 제약명은 V1__init.sql 원문을 직접 확인해 정확히 맞췄다(chk_requirements_priority 등).
-- status 컬럼 CHECK는 그대로 유지한다 — 상태값(Status) 자체의 확장은 이번 범위 밖이다(§C.1).
ALTER TABLE requirements DROP CONSTRAINT IF EXISTS chk_requirements_priority;
ALTER TABLE issues DROP CONSTRAINT IF EXISTS chk_issues_priority;
ALTER TABLE test_cases DROP CONSTRAINT IF EXISTS chk_test_cases_priority;

-- V13 각주: custom_field_definitions.enumeration_set_id는 지금까지 FK 제약 없이 BIGINT 값만
-- 보관해왔다(project_enumeration_sets 테이블이 없었기 때문). FK를 걸기 전에, 이미 저장돼 있을 수
-- 있는 고아 값(존재하지 않는 id를 가리키는 값)을 먼저 NULL로 정리한다(qa-tester 지적, Phase 20 각주).
UPDATE custom_field_definitions
SET enumeration_set_id = NULL
WHERE enumeration_set_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM project_enumeration_sets s WHERE s.id = custom_field_definitions.enumeration_set_id
  );

ALTER TABLE custom_field_definitions
    ADD CONSTRAINT fk_custom_field_definitions_enumeration_set
    FOREIGN KEY (enumeration_set_id) REFERENCES project_enumeration_sets(id) ON DELETE SET NULL;
