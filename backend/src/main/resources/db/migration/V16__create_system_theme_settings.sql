-- ADR-014: 시스템 전역 테마(색상 프리셋) 설정.
--
-- 항상 정확히 1행만 존재하는 단일 행 테이블(id는 1로 고정). 라이트/다크 모드는
-- 서버에 저장하지 않으므로(개인 브라우저 localStorage 선호, ADR-014 §1) 컬럼이 없다.
--
-- idempotent 형태(ADR-002/006 원칙): 재실행해도 오류가 나지 않도록
-- CREATE TABLE IF NOT EXISTS + INSERT ... ON CONFLICT DO NOTHING을 쓴다.

CREATE TABLE IF NOT EXISTS system_theme_settings (
    id BIGINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    color_preset VARCHAR(20) NOT NULL DEFAULT 'DEFAULT'
        CHECK (color_preset IN ('DEFAULT','RED','BLUE','GREEN','PURPLE')),
    updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 배포 직후에도 공개 조회 API(GET /api/public/theme)가 항상 유효한 응답을 반환하고
-- 기존 운영 화면 색상이 바뀌지 않도록 DEFAULT 프리셋 1행을 시드한다(하위 호환성).
INSERT INTO system_theme_settings (id, color_preset)
VALUES (1, 'DEFAULT')
ON CONFLICT (id) DO NOTHING;
