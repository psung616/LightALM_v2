-- ADR-012 §A (Phase 20): 프로젝트별 커스텀 필드 정의/값 테이블 신설.
-- EAV(Entity-Attribute-Value) 패턴 — 프로젝트마다 동적 ALTER TABLE을 실행하지 않는다.
--
-- 주의(enumeration_set_id): ADR-012 §C(열거형, Phase 22)의 project_enumeration_sets 테이블은
-- 아직 존재하지 않는다(이번 Phase 범위 밖). 그래서 enumeration_set_id는 지금은 FK 제약 없이
-- NULL 허용 BIGINT로만 추가한다. Phase 22가 project_enumeration_sets를 만들 때 다음 제약을
-- 추가해야 한다:
--   ALTER TABLE custom_field_definitions
--     ADD CONSTRAINT fk_custom_field_definitions_enumeration_set
--     FOREIGN KEY (enumeration_set_id) REFERENCES project_enumeration_sets(id) ON DELETE SET NULL;

CREATE TABLE custom_field_definitions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    field_key VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    enumeration_set_id BIGINT NULL,
    required BOOLEAN NOT NULL DEFAULT false,
    default_value TEXT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_custom_field_definitions_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE')),
    CONSTRAINT chk_custom_field_definitions_data_type CHECK (data_type IN ('TEXT','NUMBER','DATE','BOOLEAN','SINGLE_SELECT','MULTI_SELECT')),
    CONSTRAINT chk_custom_field_definitions_status CHECK (status IN ('ACTIVE','DEPRECATED')),
    CONSTRAINT uq_custom_field_definitions_project_target_key UNIQUE (project_id, target_type, field_key)
);

CREATE INDEX idx_custom_field_definitions_project_target ON custom_field_definitions(project_id, target_type);

CREATE TABLE custom_field_values (
    id BIGSERIAL PRIMARY KEY,
    field_id BIGINT NOT NULL REFERENCES custom_field_definitions(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    value TEXT NULL,
    updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_custom_field_values_field_target UNIQUE (field_id, target_type, target_id)
);

CREATE INDEX idx_custom_field_values_target ON custom_field_values(target_type, target_id);
