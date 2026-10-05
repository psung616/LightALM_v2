# ADR (Architecture Decision Record) 목록

각 문서 본문에 섞여 있던 "이전엔 이랬으나 실제로는 이렇게 바뀌었다" 류의 결정 이력을 여기로 분리했다. **새로운 구조/정책 결정을 내릴 때는 반드시 여기에 번호를 이어서 ADR을 추가한 뒤 진행한다** (담당: architect).

| 번호 | 제목 | 상태 | 요약 |
|---|---|---|---|
| [ADR-001](ADR-001-환경변수-오버라이드-방식-채택.md) | 환경변수 오버라이드 방식 채택 | Accepted | Spring profile 분리 대신 `${VAR:default}` 단일 설정 |
| [ADR-002](ADR-002-사내-공용-Postgres-사용.md) | 사내 공용 Postgres 사용 | Accepted | 로컬 postgres 컨테이너 대신 NAS 공용 DB 사용 |
| [ADR-003](ADR-003-사내Git서버-origin전환-시도.md) | 사내 Git 서버 origin 전환 시도 | Superseded by ADR-004 | 실행되지 않고 폐기된 시도 (이력 기록용) |
| [ADR-004](ADR-004-단일-origin-LightALM_v2-확정.md) | 단일 origin(LightALM_v2) 확정 | Accepted, Extended by ADR-005 | GitHub 저장소 하나만 자동 push 대상 |
| [ADR-005](ADR-005-synology-remote-운영배포트리거.md) | synology remote 추가 | **Accepted (현재 유효)** | push 시 Jenkins 자동 운영 배포 트리거 (2026-08-08) |
| [ADR-006](ADR-006-Jenkinsfile-기반-운영배포-확정.md) | Jenkinsfile 기반 운영 배포 확정 | **Accepted (현재 유효)** | docker-compose 아님. 02-architecture.md §2.4의 낡은 "미완료" 경고 해소 |
| [ADR-007](ADR-007-Testcontainers-IT분리.md) | Testcontainers IT 분리 | Accepted | `*IT.java` + failsafe로 통합 테스트 분리 |
| [ADR-008](ADR-008-v3-경쟁ALM툴-참고-스코프확장.md) | v3 확장 — 업계 ALM 기능 참고 | **Accepted (설계 완료, 구현 전)** | 업계 상용 ALM 툴에서 흔한 기능 카테고리 참고, 저작권은 개념 수준으로 한정 (2026-08-08, 1회 유실 후 재작성) |
| [ADR-009](ADR-009-projectKey-제약완화.md) | projectKey 검증 제약 완화 | **Accepted (설계 완료, 구현 전)** | 대문자 전용 3~10자 → 대문자로 시작하는 대문자/숫자/언더바 3~20자(`^[A-Z][A-Z0-9_]{2,19}$`), SQL Injection은 파라미터 바인딩으로 별도 방어 중임을 확인 (2026-09-04) |
| [ADR-010](ADR-010-다형연관-검증로직-통합.md) | 다형 연관(target_type/target_id) 검증 로직 통합 | **Accepted (설계 완료, 구현 전)** | Comment/GitLink/JenkinsBuild/Release/Traceability 5곳 중복 검증을 `PolymorphicTargetValidator`(신규 `service.support` 패키지)로 통합, 예외를 404로 통일(다형연관 다른 프로젝트 소속 케이스 포함), `TEST_CASE` 미분기 버그 수정. `traceability-links` 생성 API의 "다른 프로젝트 소속" 응답이 400→404로 변경됨 (2026-09-06) |
| [ADR-011](ADR-011-회원가입-및-라이센스관리-스코프확장.md) | 회원가입(Self-Signup) + 라이센스 파일 관리 — 스코프 확장 제안 | **Superseded by ADR-015** (2026-10-03 구현 완료 → 2026-10-05 기능 제거) | `/api/auth/signup`(공개) 추가, `users` 테이블 변경 없음. 신규 `licenses` 테이블(시트수/만료일/라이센스타입, HMAC 서명 검증, 단일 ACTIVE 보장) + 라이센스 유효성 게이트(비-ADMIN 로그인 차단, ADMIN 예외)·시트 한도 게이트(신규 계정 생성만 차단) 분리 설계. `01-scope.md` 미반영 (2026-10-03) |
| [ADR-012](ADR-012-프로젝트-Configuration-영역.md) | 프로젝트별 Configuration 영역(필드/폼 레이아웃/열거형/워크플로우) — 비스코프 재검토 | **Accepted (설계 완료, 구현 전)** | `01-scope.md` §1.3의 "커스텀 필드 제외"·"커스텀 워크플로우 엔진 제외" 재검토(제품 오너 명시적 요청). 4개 독립 기능(Phase 20~23 제안)으로 분리: 커스텀 필드(EAV 패턴), 폼 레이아웃, 열거형(PRIORITY 확장 — DB CHECK 제거하고 애플리케이션 검증으로 이전), 워크플로우 전이 화이트리스트(미설정 시 자유 전이 유지, PROJECT_ADMIN/ADMIN lock-out 예외). `01-scope.md` 미반영 (2026-10-03) |
| [ADR-013](ADR-013-작업항목-유형별-사이드바-트리패널.md) | 프로젝트 사이드바 — PRD/SRS/Defect/TestCase 작업 항목 트리 패널 | **Accepted (설계 완료, 구현 전)** | 옵션 비교 후 "최소 변경(기존 인프라 재사용)" 채택 — 범용 WorkItem 모델 재설계는 반려. `requirements.requirement_level`(PRD/SRS) 컬럼 1개만 추가, Defect는 `issues.type='BUG'` 필터, TestCase는 변경 없음. `traceability_links.source_type` CHECK에 `TEST_CASE` 추가(대칭성 보강, 신규 TargetType 아님). `ProjectLayout.tsx` 사이드바에 지연 로딩 아코디언(`WorkItemTreePanel`) 추가, 전역 탐색기 아님. 스코프 확장 승인 불필요로 판단 (2026-10-03) |
| [ADR-014](ADR-014-시스템-테마-색상-템플릿-설정.md) | 시스템 테마(색상 템플릿) 설정 — ADMIN 지정 브랜드 컬러 프리셋 + 라이트/다크 모드 | **Accepted (설계 완료, 구현 전)** | "SYSTEM_ADMIN"은 기존 `ADMIN` 역할로 해석. 색상 프리셋(DEFAULT/RED/BLUE/GREEN/PURPLE 5종, 기업명·실제 브랜드 컬러 비사용)은 **시스템 전역 설정**(ADMIN 전용, 신규 단일 행 테이블 `system_theme_settings`), 라이트/다크 모드는 **개인 선호**(localStorage 전용, DB/API 없음)로 분리 — 조직 아이덴티티 vs 개인 시각 선호라는 성격 차이에 근거. 기존 화면 27개 파일의 하드코딩 색상 리트로핏은 범위 밖(다크모드는 TopNavbar+신규 관리자 화면까지만), `index.css` DEFAULT 값은 기존과 100% 동일(하위 호환) (2026-10-05) |
| [ADR-015](ADR-015-권한체계-표시명정리-및-회원가입-라이센스-제거.md) | 권한 체계 정리 — 역할 표시명 변경, 회원가입·라이센스 제거, 마지막 System Admin 보호, 프로젝트 생성 System Admin 전용화 | **Accepted (구현 완료, qa-tester 검증 통과 2026-10-06)** — Supersedes ADR-011 | 역할 값(`ADMIN`/`USER`/`PROJECT_ADMIN`/`MEMBER`/`VIEWER`)은 그대로 두고 화면 표시명만 System Admin/User/Project Admin/Project Assignable/Project User로(`frontend/src/auth/roleDisplayNames.ts` 한 곳). 회원가입·라이센스 기능 전체 제거(DB `licenses`·`AuditTargetType.LICENSE`는 보존). 활성 ADMIN 최소 1명 불변식(`400 LAST_ACTIVE_ADMIN`, `SystemAdminRetentionPolicy` + `PESSIMISTIC_WRITE` 행 락). `POST /api/projects` ADMIN 전용(`@PreAuthorize`). 마이그레이션 없음(V18 유지) (2026-10-05) |
| [ADR-016](ADR-016-세션-사용자상태-요청마다-재검증.md) | 세션 사용자 상태(활성 여부·시스템 역할) 요청마다 재검증 + `User` 변경 컬럼만 UPDATE | **Accepted (구현 완료, qa-tester 검증 통과)** | qa-tester가 ADR-015 검증 중 발견한 결함 수정. [High] 강등·비활성화된 사용자의 기존 세션이 이전 권한 유지(자기 복권·ADMIN 생성 가능) → `SessionPrincipalRefreshFilter`(`AuthorizationFilter` 앞)가 요청마다 `users` PK 조회: 비활성/삭제면 세션 무효화 + 401, `systemRole` 변경이면 principal/authorities 교체 + 세션 저장. 대안(SessionRegistry로 세션 만료)은 DB 직접 수정·배포 전 세션을 놓쳐 반려. [Low] `User` 전체 컬럼 UPDATE의 lost update로 활성 ADMIN 0명 가능 → `@DynamicUpdate`(`@Version`은 마이그레이션 필요해 반려). 마이그레이션 없음(V18 유지) (2026-10-06) |

## ADR 작성 규칙
- 파일명: `ADR-{번호}-{짧은-제목}.md`
- 필수 섹션: 맥락(Context) → 결정(Decision) → 결과(Consequences)
- 이전 결정을 뒤집는 경우 반드시 `Supersedes` / `Superseded-by`를 명시하고, 이전 ADR의 Status를 `Superseded by ADR-{N}`으로 갱신한다
- 이 README의 표에도 한 줄 추가한다
