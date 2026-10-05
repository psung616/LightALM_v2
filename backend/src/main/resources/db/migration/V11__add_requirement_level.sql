-- ADR-013: requirements.requirement_level 컬럼 추가 (PRD/SRS 문서 레벨 구분, 기존 데이터는 'SRS'로 백필)
ALTER TABLE requirements ADD COLUMN IF NOT EXISTS requirement_level VARCHAR(10) NOT NULL DEFAULT 'SRS';
ALTER TABLE requirements DROP CONSTRAINT IF EXISTS chk_requirements_requirement_level;
ALTER TABLE requirements ADD CONSTRAINT chk_requirements_requirement_level
    CHECK (requirement_level IN ('PRD','SRS'));
