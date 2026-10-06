-- ADR-012 §C 운영 드리프트 보정 (2026-10-06).
--
-- 원인:
--   V15__create_enumeration_tables.sql은 priority 컬럼 CHECK를 V1/V3 원문의 제약명
--   (chk_requirements_priority / chk_issues_priority / chk_test_cases_priority)으로
--   `DROP CONSTRAINT IF EXISTS` 했다. 그런데 운영 공용 DB(ALM_Project)는 V1이 지금 원문과 다른
--   예전 버전으로 최초 적용된 DB다(V7__fix_due_date_drift.sql과 같은 드리프트, ADR-002 /
--   10-deployment.md 부록 E — SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false로 체크섬 검증 우회 중).
--   예전 V1은 CHECK를 이름 없이 컬럼 인라인으로 걸어 PostgreSQL 자동 이름
--   (예: requirements_priority_check)이 붙었을 가능성이 높다 — V3가
--   traceability_links_source_type_check 같은 자동 이름을 DROP하는 것도 같은 흔적이다.
--   그 결과 V15의 IF EXISTS가 조용히 아무것도 지우지 않고 LOW/MEDIUM/HIGH/CRITICAL CHECK가 남아,
--   운영에서 PRIORITY 열거형에 추가한 커스텀 값(예: BLOCKER)으로 요구사항을 만들면 CHECK 위반 → 500.
--
-- 조치:
--   requirements / issues / test_cases 에서 "priority 컬럼 하나만을 대상으로 하는 CHECK 제약"을
--   이름과 무관하게 모두 DROP한다. 판별은 제약명이 아니라 pg_constraint.conkey(제약이 참조하는
--   컬럼 번호 배열)가 정확히 [priority 컬럼]인지로 한다 — type/status/requirement_level 등
--   다른 컬럼의 CHECK는 conkey가 달라 절대 대상이 되지 않는다. 정의문(pg_get_constraintdef)에
--   'priority'가 포함되는지도 이중 확인한다.
--
-- 멱등: 대상 제약이 없으면(로컬/정상 DB, 또는 재실행) 아무 것도 하지 않는다.
-- 검증 책임은 V15 이후 그대로 EnumerationValueValidator(애플리케이션 레벨)에 있다.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.conrelid::regclass AS table_name,
               c.conname           AS constraint_name,
               pg_get_constraintdef(c.oid) AS definition
        FROM pg_constraint c
        JOIN pg_class t     ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_attribute a ON a.attrelid = c.conrelid
                           AND a.attname = 'priority'
                           AND NOT a.attisdropped
        WHERE c.contype = 'c'
          AND n.nspname = current_schema()
          AND t.relname IN ('requirements', 'issues', 'test_cases')
          AND c.conkey = ARRAY[a.attnum]::smallint[]
          AND pg_get_constraintdef(c.oid) ILIKE '%priority%'
    LOOP
        RAISE NOTICE 'V19: dropping legacy priority CHECK % on % (%)',
            r.constraint_name, r.table_name, r.definition;
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.table_name, r.constraint_name);
    END LOOP;
END
$$;
