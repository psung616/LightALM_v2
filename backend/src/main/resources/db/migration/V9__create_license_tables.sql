-- ADR-011: 라이센스 파일 관리 서브시스템 — licenses 테이블 신설
CREATE TABLE IF NOT EXISTS licenses (
    id BIGSERIAL PRIMARY KEY,
    license_key VARCHAR(100) NOT NULL UNIQUE,
    organization_name VARCHAR(150) NOT NULL,
    license_type VARCHAR(20) NOT NULL CHECK (license_type IN ('TRIAL','STANDARD','ENTERPRISE')),
    seat_limit INTEGER NOT NULL CHECK (seat_limit > 0),
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUPERSEDED','REVOKED')),
    raw_file_name VARCHAR(255) NOT NULL,
    raw_payload TEXT NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT true,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_licenses_single_active ON licenses(status) WHERE status = 'ACTIVE';
