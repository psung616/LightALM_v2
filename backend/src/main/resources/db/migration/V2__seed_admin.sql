-- 최초 시스템 관리자 계정 시드.
-- 아이디: admin / 비밀번호: admin1234 (BCrypt 해시) — 최초 로그인 후 반드시 비밀번호를 변경할 것.
INSERT INTO users (username, password, email, full_name, system_role, enabled)
VALUES ('admin', '$2a$10$VAr6MPsL5fILrILAP0zaW.Ayg/0IaTDbtCaxgkaB5uJ8c5n8K3Spi', 'admin@lightalm.local', 'System Administrator', 'ADMIN', true);
