-- ADR-012 §B (Phase 21): 프로젝트+target_type별 폼 레이아웃(섹션+필드 배치) 테이블 신설.
-- 프로젝트+target_type당 레이아웃은 정확히 1개(UNIQUE(project_id, target_type)) — §B.1.
--
-- standard_field_key의 유효값 집합은 DB CHECK로 걸지 않는다(target_type마다 표준 필드 집합이
-- 다르고 여러 Java 클래스에 흩어져 있어 단일 CHECK로 표현 불가 — §B.2 각주). 대신 서비스 레이어의
-- StandardFieldKeyRegistry가 생성/수정 시 검증한다.

CREATE TABLE form_layouts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_form_layouts_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE','TEST_CASE')),
    CONSTRAINT uq_form_layouts_project_target UNIQUE (project_id, target_type)
);

CREATE TABLE form_layout_sections (
    id BIGSERIAL PRIMARY KEY,
    form_layout_id BIGINT NOT NULL REFERENCES form_layouts(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_form_layout_sections_form_layout ON form_layout_sections(form_layout_id);

CREATE TABLE form_layout_fields (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES form_layout_sections(id) ON DELETE CASCADE,
    field_source VARCHAR(20) NOT NULL,
    standard_field_key VARCHAR(50) NULL,
    custom_field_id BIGINT NULL REFERENCES custom_field_definitions(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    visible BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_form_layout_fields_field_source CHECK (field_source IN ('STANDARD','CUSTOM')),
    CONSTRAINT chk_form_layout_fields_source_shape CHECK (
        (field_source = 'STANDARD' AND standard_field_key IS NOT NULL AND custom_field_id IS NULL)
        OR
        (field_source = 'CUSTOM' AND custom_field_id IS NOT NULL AND standard_field_key IS NULL)
    )
);

CREATE INDEX idx_form_layout_fields_section ON form_layout_fields(section_id);
