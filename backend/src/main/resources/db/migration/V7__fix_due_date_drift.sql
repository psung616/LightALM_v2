-- ALM_Project DB는 V1이 due_date 컬럼이 추가되기 전 버전으로 최초 적용되어 있었음.
-- 현재 엔티티/V1 정의와 실제 스키마를 맞추기 위한 보정 마이그레이션 (idempotent).
ALTER TABLE requirements ADD COLUMN IF NOT EXISTS due_date DATE;
ALTER TABLE issues ADD COLUMN IF NOT EXISTS due_date DATE;
