-- ADR-013: traceability_links.source_type CHECK 제약에 'TEST_CASE' 추가 (target_type과 대칭성 확보)
ALTER TABLE traceability_links DROP CONSTRAINT IF EXISTS chk_traceability_links_source_type;
ALTER TABLE traceability_links ADD CONSTRAINT chk_traceability_links_source_type
    CHECK (source_type IN ('REQUIREMENT','ISSUE','TEST_CASE'));
