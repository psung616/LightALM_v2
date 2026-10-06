> Owner: orchestrator (전체 세션이 매 Phase 종료 시 갱신) | Status: current | Last-reviewed: 2026-10-06
> 상위 문서: [SPEC.md](SPEC.md)

## 이 문서의 목적

개별 섹션 문서(01-scope.md ~ 10-deployment.md)에는 시간이 지나며 "당시엔 이랬으나 나중에 바뀐" 서술이 본문에 섞여 있을 수 있다(예: 배포 상태, Git remote 정책). **이 문서는 지금 이 순간 유효한 사실만 담고, 상충하는 서술이 있으면 이 문서를 우선한다.** 이 문서를 갱신할 때는 근거가 된 원본 문서/ADR을 함께 표기한다.

작업을 시작하기 전 이 문서를 먼저 읽고, 필요하면 관련 섹션 문서로 들어가 상세를 확인하는 순서를 권장한다.

---

## 1. 구현 진행 상태

| 항목 | 상태 |
|---|---|
| Phase 0~11 (Light ALM MVP: 인증, 프로젝트, 요구사항, 이슈, 추적성, GitHub/Jenkins 연동, 프론트엔드) | ✅ 완료, 운영 중 |
| Phase 12 (테스트케이스 & 테스트 실행) | ✅ 완료 (2026-08-04) |
| Phase 13 (릴리스/버전 관리) | ✅ 완료 (2026-08-04) |
| Phase 14 (변경 이력/감사 로그) | ✅ 완료 (2026-08-04) |
| Phase 15 (승인 워크플로우) | ✅ 완료 (2026-08-04) |
| Phase 16 (v3: 리뷰 사이클 + 베이스라인) | ✅ 구현 완료. qa-tester 검증에서 **경미(Low) 1건 반려**(잘못된 enum 값·본문 누락 등 역직렬화 오류가 400이 아닌 500) → `GlobalExceptionHandler` 수정 완료 → **qa-tester 재검증 통과**(2026-10-05) (2026-10-05, ADR-008. 마이그레이션 `V18__create_review_and_baseline_tables.sql`, `com.lightalm.review`·`com.lightalm.baseline` 패키지. developer가 로컬 docker-compose Postgres + curl로 DoD 2항목 실측 확인) |
| Phase 17~19 (v3: 위험 관리 / 문서 뷰+변형 관리 / 대시보드 위젯+리포트 내보내기) | 🔲 설계 완료, **구현 전** (2026-08-08 설계, ADR-008) |
| v4: 회원가입(Self-Signup) + 라이센스 파일 관리 | ⛔ **ADR-015로 제거됨**(2026-10-05). 2026-10-03 구현·qa 통과(ADR-011) 후 기능 전체 삭제. DB `licenses` 테이블(V9)·`audit_logs` LICENSE CHECK(V10)·`AuditTargetType.LICENSE`만 보존 |
| v4: 프로젝트 Configuration(커스텀 필드/폼레이아웃/열거형/워크플로우) | ✅ **Phase 20~23 전부 구현 완료 — ADR-012 전체 완료(2026-10-05)**. Phase 20~21은 qa-tester 검증 통과(반려 1건 수정 후). **Phase 22(열거형)는 qa-tester 검증에서 Medium 2건 반려**(폐기된 PRIORITY 값 보유 항목의 다른 필드 수정 차단, `enumKey=PRIORITY`+baseEnum 생략 집합이 프로젝트 전체 priority 쓰기 차단) → developer 수정 → **qa-tester 재검증 통과**(2026-10-05). Phase 23(워크플로우)은 1차 검증의 치명적 버그(항목6) 반려·수정 후 **qa-tester 최종 재승인 완료**(2026-10-05) (2026-10-03 설계, ADR-012) |
| v4: 프로젝트 사이드바 — PRD/SRS/Defect/TestCase 유형별 트리 | ✅ 완료, qa-tester 검증 통과 (2026-10-04, ADR-013) |
| v4: 시스템 테마(색상 템플릿) 설정 | ✅ 완료, qa-tester 검증 통과 (2026-10-05, ADR-014. 마이그레이션 `V16__create_system_theme_settings.sql`, `com.lightalm.theme` 패키지) |
| v4 유지보수: 권한 체계 정리(ADR-015) — 역할 표시명, 회원가입·라이센스 제거, 마지막 활성 System Admin 보호, 프로젝트 생성 System Admin 전용 | ✅ 구현 완료, **qa-tester 검증 통과**(2026-10-06 — 판정 가능한 DoD 전부 PASS. 화면 항목 DoD 7·8·9·15·16d는 코드상 PASS·**브라우저 미확인**, `LastActiveAdminConcurrencyIT` **미실행**. 검증 중 발견한 결함 2건은 ADR-016으로 수정) (2026-10-05, ADR-015. 마이그레이션 없음 — V18 최신 유지. 신규 `com.lightalm.user` 패키지(`SystemAdminRetentionPolicy`, `LastActiveAdminRemovalException`), 프론트 `frontend/src/auth/roleDisplayNames.ts`. `mvnw test` 198개 통과(199 − 삭제 18 + 신규 17), 프론트 tsc/build/lint 통과(경고 4건 기존 그대로). developer가 로컬 docker-compose Postgres + curl로 백엔드 DoD 실측) |
| v4 유지보수: 세션 사용자 상태 요청마다 재검증 + `User` lost update 방지(ADR-016) — qa-tester가 ADR-015 검증 중 발견한 [High] 강등·비활성 사용자 기존 세션의 권한 유지(자기 복권·ADMIN 생성), [Low] 활성 ADMIN 0명 가능 lost update | ✅ 구현 완료, **qa-tester 검증 통과** (2026-10-06, ADR-016. 마이그레이션 없음 — V18 최신 유지. 신규 `com.lightalm.security.SessionPrincipalRefreshFilter`(`AuthorizationFilter` 앞, 요청마다 `users` PK 조회 → 비활성/삭제 401+세션 무효화, 역할 변경 시 principal/authorities 교체), `User`에 `@DynamicUpdate`. `mvnw test` 208개 통과(198 + 신규 10). developer가 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 qa 재현 시나리오·회귀 실측) |
| v4 유지보수: 운영 DB priority CHECK 드리프트 보정(V19) + `409 DATA_INTEGRITY_VIOLATION` | ✅ 구현·로컬 재현 검증 완료, **qa-tester 검증 통과**(2026-10-06, 임시 DB 독립 재현·대조군 미삭제·롤백·멱등성 확인, `mvnw test` 209개), 운영 배포 후 확인 대기. 운영에서 PRIORITY 열거형 커스텀 값(`BLOCKER`) 요구사항 생성이 500 — 운영 공용 DB가 예전 V1로 만들어져 priority CHECK 이름이 달라 V15의 `DROP CONSTRAINT IF EXISTS chk_..._priority`가 no-op였던 것이 원인(추정, 로컬 재현으로 동일 증상 확인). 마이그레이션 `V19__drop_legacy_priority_check_constraints.sql`(이름 무관 priority 단일 컬럼 CHECK DROP, 멱등) — **최신 마이그레이션 V19**. `GlobalExceptionHandler`에 `DataIntegrityViolationException`→409 추가. `mvnw test` 209개. 새 ADR 없음 — ADR-012 §C.3 각주, 10-deployment.md 부록 G |

근거: 08-dev-phases.md

## 2. 배포 상태

- **운영 서비스**: `https://alm.ondalprincess.synology.me/` — **실제로 서비스 중** (사내 Gitea `synology` remote push → Jenkins `ALM_Pipeline` 자동 배포, 호스트 포트 8888)
- **로컬 개발**: `http://localhost:5173`(프론트) / `http://localhost:8080`(백엔드) — `docker compose up --build`로 기동, 검증 완료
- ⚠️ 02-architecture.md §2.4에는 이 도메인이 "아직 연결되지 않음(미완료)"이라는 문단이 남아있는데, 이는 **낡은 서술**이다. ADR-006 참고.

근거: 10-deployment.md 부록 E, ADR-006

## 3. DB 접속 정보

| 항목 | 값 |
|---|---|
| Host | `ondalprincess.synology.me` |
| Port | `55432` |
| DB | `ALM_Project` |
| 계정 | `postgres` / `postgres` |
| 비고 | 로컬 개발용 자체 DB가 아니라 **사내 공용 Postgres**를 그대로 씀(KPI 집계 등 다른 도구가 같은 DB를 봄). 로컬/사내 테스트 서버 구분 없이 이 DB 하나만 사용 중 |

근거: 02-architecture.md §2.4, 10-deployment.md 부록 B·E, ADR-002

## 4. Git 원격 저장소 — **반드시 최신 기준으로 확인**

| remote | URL | 용도 |
|---|---|---|
| `origin` | `https://github.com/psung616/LightALM_v2.git` | 소스 백업. push해도 자동 배포 없음 |
| `synology` | `https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository` | **운영 배포 트리거.** push하면 Jenkins가 자동 배포 |

**운영 서버에 반영하려면 반드시 `synology`에도 push해야 한다.** `origin`에만 push하면 배포되지 않는다.

⚠️ 08-dev-phases.md와 02-architecture.md §2.5 본문에는 "각 Phase마다 `origin`에만 push한다"는 더 이전 정책이 그대로 남아있다. **이는 ADR-005(2026-08-08)로 대체됐다** — 지금부터 Phase/기능을 추가로 구현할 때는 `origin` + `synology` 둘 다 push해야 실제 서비스에 반영된다.

근거: 10-deployment.md 부록 D(2026-08-08 갱신), ADR-004, ADR-005

## 5. 계정 정보

| 항목 | 값 |
|---|---|
| 최초 관리자 | `admin` / `admin1234` (V2__seed_admin.sql 시드값, 로그인 후 변경 권장) |
| 계정·프로젝트 생성 | **System Admin(`ADMIN`)만** — 계정은 `POST /api/users`(공개 회원가입 없음), 프로젝트는 `POST /api/projects`(ADMIN 전용). 활성 ADMIN은 애플리케이션이 항상 1명 이상 유지(`400 LAST_ACTIVE_ADMIN`). 역할 화면 표시명: ADMIN=System Admin, USER=User, PROJECT_ADMIN=Project Admin, MEMBER=Project Assignable, VIEWER=Project User (ADR-015) |
| Jenkins | `https://jenkins.ondalprincess.synology.me/job/ALM_Pipeline/` |

## 6. 알려진 리스크 / TODO (아직 해결 안 됨)

- GitHub PAT, Jenkins API 토큰이 DB에 평문 저장됨 — 운영 전환 시 암호화 필요(07-integrations.md §7.4)
- DB 계정(`postgres/postgres`)이 `docker-compose.yml`/Jenkinsfile에 평문으로 커밋되어 있음 — Jenkins Credentials로 이전 필요(10-deployment.md 부록 E)
- Flyway `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`로 체크섬 검증을 우회 중 — 스키마 드리프트를 놓칠 수 있는 상태
- **[운영 확인 필요 — V19, 2026-10-06]** 운영 공용 DB의 priority CHECK 드리프트(예전 이름 제약이 남아 커스텀 PRIORITY 값 생성 500)는 `V19__drop_legacy_priority_check_constraints.sql`로 보정했지만 **로컬 재현 환경에서만 검증**했다(운영 DB 직접 조회는 금지 범위라 운영의 실제 제약명은 추정). `synology` 배포 후 10-deployment.md 부록 G의 확인 절차(Flyway NOTICE에 삭제된 제약명, `pg_constraint` 조회, 프로젝트 4 BLOCKER 생성 201)를 수행해야 한다. V19가 아무 제약도 지우지 않았는데 여전히 실패하면, 이제 응답은 500이 아니라 `409 DATA_INTEGRITY_VIOLATION`이고 서버 WARN 로그에 실제 위반 제약명이 남으므로 그것으로 재조사한다. 운영과 로컬 스키마가 다를 수 있다는 위 Flyway 체크섬 우회 리스크의 실제 발현 사례이며, 다른 컬럼/제약에도 유사 드리프트가 남아 있을 가능성은 배제되지 않았다(전체 스키마 비교 미실시)
- **[조치 권고 — 스키마 드리프트 전수 점검]** V19는 priority CHECK만 보정함. 운영과 로컬의 전체 스키마를 `pg_dump --schema-only`(조회만)로 비교해 다른 드리프트를 찾고, 그 결과에 따라 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` 해제를 검토해야 함(qa-tester 2026-10-06 권고).
- **[Low] 409 WARN 로그의 행 데이터** — `DataIntegrityViolationException` 처리 시 WARN 로그에 PostgreSQL 원문("Failing row contains (...)")이 남아 행 데이터(개인정보 가능)가 서버 로그에 기록될 수 있음. 응답에는 노출되지 않음(qa-tester 2026-10-06).
- `synology` push가 곧바로 운영 배포로 이어지는데 별도의 배포 승인/검토 게이트가 없음(ADR-005 리스크 항목 참고)
- `synology` remote 저장소명(`ALM_Repository`)이 GitHub 저장소명(`LightALM_v2`)과 달라 혼동 가능성 있음
- **[해결됨 — 기능 제거, ADR-015, 2026-10-05]** ~~`LICENSE_SIGNING_SECRET` 환경변수 기본값이 개발용 평문(`light-alm-dev-only-insecure-secret-change-in-production`)으로 설정돼 있음 — **운영 배포 전 반드시 고유한 값으로 교체 필요.** 교체하지 않으면 기본값을 아는 누구나 유효한 라이센스 파일을 위조할 수 있다(ADR-011, HMAC 대칭키 설계의 의도된 한계)~~ — 라이센스 기능과 `light-alm.license.signing-secret` 설정이 삭제되어 위조할 대상 자체가 없다
- **[해결됨 — 기능 제거, ADR-015, 2026-10-05]** ~~라이센스 만료/부재 시 복구 경로가 시스템 `ADMIN` 로그인 하나뿐임 — **최소 1개의 활성 ADMIN 계정을 항상 유지**해야 한다. 모든 ADMIN이 비활성화된 상태에서 라이센스까지 만료되면 아무도 새 라이센스를 올릴 수 없는 복구 불가 상태가 된다(ADR-011)~~ — 라이센스 게이트가 없어졌고, 대신 "활성 ADMIN 0명" 상태 자체를 애플리케이션이 막는다(아래 ADR-015 항목)
- **[해결됨, 2026-10-05]** `custom_field_definitions.enumeration_set_id` FK 미검증 리스크 — Phase 22(V15 마이그레이션)가 고아값 정리(`UPDATE ... SET enumeration_set_id = NULL WHERE NOT EXISTS (...)`) 후 `fk_custom_field_definitions_enumeration_set` FK(`ON DELETE SET NULL`)를 추가해 해소됐다(ADR-012 §C 각주).
- Phase 22(PRIORITY 열거형 확장) 구현 후 새로 생긴 리스크: `requirements`/`issues`/`test_cases`의 `priority` 컬럼은 더 이상 DB CHECK 제약이 없다 — 유효성 검증은 전부 `EnumerationValueValidator`(애플리케이션 레벨)에 의존한다. 이 validator를 거치지 않는 쓰기 경로(향후 추가되는 bulk import, 관리 스크립트 등)가 생기면 DB 안전망 없이 잘못된 값이 저장될 수 있다(ADR-012 §C "결과" 섹션에 이미 예견된 리스크).
- Phase 22는 백엔드/DB만 구현했다 — PROJECT_ADMIN이 PRIORITY 값을 프로젝트별로 확장해도, 프론트엔드 "열거형" 설정 탭과 요구사항/이슈/테스트케이스 생성·수정 화면의 PRIORITY 드롭다운은 아직 고정된 LOW/MEDIUM/HIGH/CRITICAL만 보여준다(API는 정상 동작하지만 화면에 반영되지 않음). 커스텀 필드 SINGLE_SELECT/MULTI_SELECT를 열거형 집합과 연결해 드롭다운으로 렌더링하는 것도 마찬가지로 미구현 상태라, Phase 20의 "자유 텍스트 입력" 임시 구현이 그대로 남아있다(ADR-012 §C 각주). 이미 API로 커스텀 PRIORITY 값(예: BLOCKER)이 저장된 항목을 수정 화면에서 열면 드롭다운에 해당 옵션이 없어 선택란이 비어 보일 수 있다 — 저장된 값 자체는 안전하게 유지되며 표시만 혼란스럽다(qa-tester 2026-10-05 확인).
- **[해결됨, 2026-10-05]** 커스텀 필드 생성 API(`POST .../config/custom-fields`)가 존재하지 않는 `enumerationSetId`를 애플리케이션 레벨에서 사전 검증하지 않아 Phase 22가 추가한 FK 제약 위반 시 `500 INTERNAL_ERROR`를 반환하던 문제(qa-tester 2026-10-05 발견) — `CustomFieldDefinitionService.create()`에 `FormLayoutService`와 동일한 패턴의 소속 검증(`ProjectEnumerationSetRepository.existsByIdAndProjectId`)을 추가해 `ValidationException`(400)으로 바뀌었다. `UpdateCustomFieldDefinitionRequest`에는 이 필드가 없어(생성 후 불변) 수정은 `create()` 경로에만 적용(ADR-012 §A 각주).
- **[확인 필요]** Phase 20~22 기간 동안 Testcontainers 기반 `*IT.java`(`RequirementListFilterIT`, `SequenceGenerationIT`, `TraceabilityLinkSourceTypeIT`)가 `mvn verify`로 실제 실행되어 통과한 기록이 없다 — developer와 qa-tester 둘 다 이 Windows 환경의 Docker Desktop/Testcontainers 호환성 문제로 `mvn test`(단위 테스트)까지만 확인했다. Docker가 정상 동작하는 환경(CI 등)에서 `mvn verify` 재확인 필요(ADR-007 원칙 참고).
- **[해결됨, 2026-10-05]** `@PreAuthorize("hasRole('ADMIN')")`가 비-ADMIN을 거부할 때 Spring Security 6.3+의 `AuthorizationDeniedException`(`AccessDeniedException` 하위 타입)을 `GlobalExceptionHandler`가 전용 처리하지 않아 **403이 아니라 500**이 응답되던 전역 버그 — ADR-014(시스템 테마 설정) 구현 중 `ThemeAdminController`를 비-ADMIN으로 실제 curl 호출해 재현했고, `LicenseAdminController` 등 `@PreAuthorize`를 쓰는 기존 모든 ADMIN 전용 컨트롤러에도 동일하게 영향을 주고 있었다. `GlobalExceptionHandler`에 `AccessDeniedException` 전용 핸들러(403/`FORBIDDEN`)를 추가해 해소했다(ADR-014 구현 각주 참고). 이 환경에서 Testcontainers 기반 `*IT.java`는 Docker Engine API 버전 불일치(`client version 1.32 is too old`)로 ryuk 사이드카가 기동하지 못해 여전히 실행할 수 없었다(§6의 "확인 필요" 항목과 동일 현상) — 대신 로컬 docker-compose Postgres + `mvnw spring-boot:run`으로 실제 Postgres에 대해 curl 수동 검증했다.
- **[수정 완료, UI 디스클레이머 처리]** 폼 레이아웃(Phase 21) 설정 화면에서 PROJECT_ADMIN이 CUSTOM(커스텀 필드)을 레이아웃에 배치해 저장해도, 요구사항/이슈/테스트케이스의 생성/수정 화면은 그 배치를 반영하지 않는다(STANDARD 필드만 동적으로 렌더링됨) — 저장/조회 API 자체는 정상 동작하지만 CUSTOM 배치의 실제 화면 효과는 아직 없다. qa-tester 반려 사유였고, 기능을 구현하지 않는 대신 `FormLayoutSettingsTab.tsx`에 Phase 20과 같은 톤의 명시적 디스클레이머(배치된 CUSTOM 필드 행에 "(생성/수정 화면에는 아직 미반영)" 태그, 추가 드롭다운 아래 안내문)를 추가해 사용자가 오인하지 않도록 처리했다 — **UI 안내로 반려는 해소됐지만, CUSTOM 필드 배치 자체가 생성/수정 폼에 영향을 주는 기능은 여전히 미구현 상태**다(ADR-012 §B 각주, 2026-10-05)

- Phase 16(리뷰 사이클/베이스라인) 구현 후 새로 생긴 리스크(2026-10-05):
  - **운영 DB 미적용**: `V18__create_review_and_baseline_tables.sql`은 로컬 docker-compose Postgres에만 적용·검증했다. `synology` push(운영 배포) 시 운영 공용 DB(`ALM_Project`)에 처음 적용된다 — 신규 테이블 4개 CREATE만 있어 기존 데이터에 영향은 없지만, 이 DB를 함께 쓰는 KPI 집계 등 다른 도구에도 테이블이 생긴다.
  - **고아 리뷰 사이클**: `review_cycles.target_id`에 FK가 없어(다형 연관, 기존 `comments` 등과 동일) 대상 요구사항/이슈를 삭제해도 리뷰 사이클·참여자 행이 DB에 남는다. 목록 API가 대상 존재를 먼저 검증(404)하므로 화면/API로는 노출되지 않지만 정리 로직은 없다. (`baseline_items`는 diff의 "대상 삭제됨" 표시를 위해 의도적으로 남긴다.)
  - **감사 로그 미연동**: 리뷰 사이클 생성/결정/닫기, 베이스라인 생성은 `audit_logs`에 기록되지 않는다 — `audit_logs.target_type` CHECK에 해당 값이 없고 설계 문서에도 요구가 없어 의도적으로 제외(03-data-model.md §3.21 구현 각주). 필요해지면 CHECK 확장 마이그레이션 + ADR 필요.
  - 베이스라인 diff는 호출할 때마다 항목 수만큼 원본 테이블을 개별 조회한다(N건 → N회 `findById`). 수백 건 규모 베이스라인에서는 응답이 느려질 수 있다 — 현재 규모에선 비차단.
  - 베이스라인 스냅샷에는 커스텀 필드 값(ADR-012 §A)이 포함되지 않는다(표준 필드만).
  - **[해결됨, 2026-10-05]** 잘못된 요청 본문(JSON 문법 오류, 본문 누락, enum에 없는 값 — 예: baselines `itemRefs[].targetType:"FOO"`/소문자 `"requirement"`, review-cycles `decision:"FOO"`)이 `HttpMessageNotReadableException`으로 전역 핸들러 catch-all에 떨어져 400이 아니라 500으로 응답되던 문제(developer 발견, qa-tester Phase 16 검증에서 Low로 반려) — `GlobalExceptionHandler`에 `HttpMessageNotReadableException`·`MethodArgumentTypeMismatchException`(예: `/baselines/abc`)·`MissingServletRequestParameterException` 핸들러를 추가해 모두 `400 VALIDATION_ERROR`로 응답한다. enum 오류는 메시지에 필드 경로와 허용값을 담는다. 로컬 Postgres + curl로 qa 재현 4건 + 경로 변수/문법 오류 2건 모두 400 확인(04-api.md §4.1 각주).
- **[해결됨, 2026-10-05]** Phase 22 qa-tester 반려 2건(Medium) — (1) 폐기(DEPRECATED)된 PRIORITY 값을 가진 요구사항/이슈/테스트케이스는 priority를 건드리지 않는 다른 필드 수정까지 400으로 막혔다 → update 경로는 값이 기존과 같으면 ACTIVE 검증 생략(`EnumerationValueValidator.requireValidValueForChange`). (2) `enumKey=PRIORITY`인데 baseEnum을 생략한 집합(값 0개)이 만들어지면 그 프로젝트의 모든 priority 쓰기(기본값 MEDIUM 포함)가 400이 되고 집합 삭제 API가 없어 복구 불가 → 생성 시 `enumKey=PRIORITY` ⇔ `baseEnum=PRIORITY` 강제(어긋나면 400). 스키마 변경 없음. **남은 한계**: 이 수정 이전에 이미 생성된 불일치 집합은 자동 정리되지 않는다(로컬 검증 DB 프로젝트 14에 1건 존재 — 로컬이라 무관). 운영 DB에 같은 데이터가 있는지는 확인하지 않았다(운영 DB 접속 금지) — 있다면 DB에서 직접 정리 필요. 상세는 ADR-012 §C 각주.
- 매핑되지 않은 경로(예: `/api/licenses`처럼 실제로는 `/api/admin/licenses`인 오타성 경로)로 인증된 사용자가 요청하면 `NoHandlerFoundException`이 전역 핸들러의 catch-all로 떨어져 500이 응답됨(404가 맞음) — ADR-014 검증 중 발견된 기존부터 있던 별개 문제, 비차단(qa-tester 2026-10-05)
- **[해결됨, 2026-10-05]** Phase 23(워크플로우 전이 규칙) qa-tester 실제 Postgres+API 검증에서 치명적 버그 발견 및 반려 — `ApprovalService.decide()`(`DRAFT→APPROVED` 실행 지점)가 lock-out 방지 우회가 포함된 `WorkflowTransitionPolicy.requireAllowedTransition(...)`을 그대로 호출해, `decide()`를 호출할 수 있는 모든 실제 호출자(항상 PROJECT_ADMIN 이상)가 그 우회 조건에 걸려 워크플로우 체크가 전혀 작동하지 않는 죽은 코드였다(화이트리스트에 `DRAFT→APPROVED`가 없어도 그대로 승인 성공). lock-out 우회가 없는 변형 `requireRegisteredTransition(...)`(actor 파라미터 자체 없음)을 추가해 `decide()`가 이를 쓰도록 수정했다 — 이 호출 지점은 `decide()` 자체가 이미 PROJECT_ADMIN+만 허용하므로 lock-out 시나리오가 없다(막혀도 PROJECT_ADMIN이 `config/workflow-rules`로 직접 규칙을 추가해 풀 수 있음). 로컬 docker-compose 실제 Postgres + `mvnw spring-boot:run`으로 구동한 실제 API에 PROJECT_ADMIN(시스템 ADMIN 아님) 계정으로 curl 재현: 화이트리스트에 미등록된 `DRAFT→APPROVED`는 `400 VALIDATION_ERROR`로 거부되고 요구사항/승인 요청 상태가 모두 원상태로 유지됨을 DB에서 직접 확인했고, 규칙을 추가한 뒤 재시도하면 `200`으로 성공해 요구사항이 `APPROVED`로 바뀜을 확인했다. 회귀 전이(규칙 미등록 프로젝트)도 `200`으로 정상 통과 확인. `mvnw test` 161개 통과(이전 157개 + 신규 4개). 상세는 ADR-012 §D 각주 참고.
- ADR-015(권한 체계 정리) 구현 후 새로 생긴 리스크/주의(2026-10-05):
  - **DB 직접 수정은 막지 못함**: 마지막 활성 System Admin 보호는 애플리케이션 레벨(`SystemAdminRetentionPolicy`)이다. `psql` 등으로 `users`를 직접 UPDATE하면 활성 ADMIN이 0명이 될 수 있고, 그때 복구는 DB 직접 UPDATE뿐이다: `UPDATE users SET enabled=true, system_role='ADMIN' WHERE username='<계정>';` (10-deployment.md 부록 F)
  - **[해결됨 — ADR-016, 2026-10-06]** ~~**세션 권한 즉시 미반영(기존부터 있던 한계)**: `UserPrincipal`이 로그인 시 세션에 저장되므로, ADMIN 해제·비활성화된 사용자도 세션이 끝날 때까지 이전 권한을 유지한다~~ — qa-tester가 실제로 자기 복권(`PUT /api/users/{본인}` 200)·ADMIN 생성(`POST /api/users` 201)·비활성 USER 쓰기(201)로 악용 가능함을 재현해 High로 반려. `SessionPrincipalRefreshFilter`가 요청마다 DB의 `enabled`/`system_role`을 재검증해 비활성/삭제면 세션 무효화 + 401, 역할 변경이면 principal/authorities를 교체한다. 로컬 실측으로 같은 시나리오가 401/403으로 막힘을 확인
  - **[해결됨 — ADR-016, 2026-10-06]** `User` 엔티티 전체 컬럼 UPDATE로 email/fullName만 수정하는 요청이 동시 커밋된 `system_role`/`enabled`를 되돌려 활성 ADMIN이 0명이 될 수 있던 lost update(qa-tester Low) — `@DynamicUpdate`로 변경 컬럼만 UPDATE(`update users set email=?,updated_at=? where id=?` 로그 확인). 두 트랜잭션 동시 재현 테스트는 하지 않음(SQL 형태로 판정)
  - **브라우저 미확인(qa-tester 2026-10-06)**: ADR-015 화면 항목 DoD 7·8·9·15·16d는 코드상 PASS이나 실제 브라우저로 확인되지 않았다 — 운영 배포 전 또는 Playwright 등이 가능한 환경에서 확인 필요
  - **운영 배포 전 확인 필요**: 운영 DB의 활성 ADMIN 수(1명이면 배포 후 그 계정은 해제·비활성화 불가 — 의도된 동작이므로 운영자에게 사전 안내). 배포 후 USER는 프로젝트를 만들 수 없고(403), `/signup` 경로가 사라진다(기존 가입 계정은 일반 USER로 유지)
  - **동시 강등 통합 테스트 미실행**(qa-tester 2026-10-06 검증에서도 미실행): `LastActiveAdminConcurrencyIT`(Testcontainers)는 작성·컴파일만 했고 이 환경에서 실행하지 못했다(위 "확인 필요" 항목과 같은 Docker/Testcontainers 호환성 문제). 대신 로컬 Postgres에서 psql 세션이 활성 ADMIN 행 락을 잡은 동안 API 요청이 `Lock` 대기 → 커밋 후 재평가로 `400`이 나는 것과, 병렬 curl 3회 모두 정확히 1건만 성공함을 확인했다
- ADR-016(세션 사용자 상태 재검증) 구현 후 새로 생긴 리스크/주의(2026-10-06):
  - 인증된 모든 요청에 `users` PK SELECT 1회가 추가된다(현재 규모에서 비차단)
  - 반영 시점은 "다음 요청부터"다 — 강등·비활성화 시점에 이미 필터를 통과해 실행 중인 요청은 이전 권한으로 끝난다
  - 비밀번호 변경은 다른 세션을 끊지 않는다(기존 동작 유지, 범위 밖)
  - **운영 배포 영향**: 배포 직후부터 비활성 계정의 기존 세션은 다음 요청에서 401로 로그아웃되고, 역할이 바뀐 계정은 새 역할로 동작한다(의도된 동작)
  - `licenses` 테이블과 `audit_logs`의 LICENSE 행은 공용 DB에 쓰이지 않는 데이터로 남는다. DROP하려면 별도 ADR + 새 마이그레이션 + 다른 도구의 참조 여부 확인 필요

## 갱신 이력
- 2026-08-08: 문서 구조 2차 개편과 함께 최초 작성. ADR-001~007 정리 및 02-architecture.md §2.4 낡은 경고에 상호참조 추가
- 2026-10-04: §1 ADR-012 행 갱신 — Phase 20(커스텀 필드) 구현 완료(마이그레이션 V13, `com.lightalm.customfield` 패키지). Phase 21~23은 아직 미구현
- 2026-10-05: §6에 Phase 20 관련 리스크(`enumeration_set_id` FK 미검증) 추가. qa-tester 반려 사유였던 "DEPRECATED 필드 값 저장 영구 거부" 버그를 수정(기존 값 수정은 허용, 신규 생성만 거부) — ADR-012 §A 각주 참고
- 2026-10-05: §1 ADR-012 행 갱신 — Phase 21(폼 레이아웃) 구현 완료(마이그레이션 V14, `com.lightalm.formlayout` 패키지). 설계 대비 차이점은 ADR-012 §B 각주 참고. qa-tester 검증은 아직 진행 전
- 2026-10-05: qa-tester 검증 통과(반려 1건: CUSTOM 필드 배치가 생성/수정 화면에 무동작이면서 아무 안내도 없던 문제). `FormLayoutSettingsTab.tsx`에 디스클레이머 추가해 수정, §1/§6 갱신 — ADR-012 §B 각주 참고
- 2026-10-05: §1 ADR-012 행 갱신 — Phase 22(열거형) 구현 완료(마이그레이션 V15, `com.lightalm.enumeration` 패키지). `priority` 필드를 Java enum에서 String으로 전면 변경(EnumerationValueValidator가 유일한 검증 지점)하는 리팩터링 포함. §6의 "enumeration_set_id FK 미검증" 리스크를 해결됨으로 갱신, PRIORITY 컬럼 DB 안전망 상실 리스크와 프론트엔드 미반영(의도된 범위 제한) 리스크를 신규 기록. 설계 대비 차이점은 ADR-012 §C 각주 참고. qa-tester 검증은 아직 진행 전
- 2026-10-05: qa-tester 권고(Phase 22 후속 보완) 반영 — `CustomFieldDefinitionService.create()`에 `enumerationSetId` 소속 검증 추가(500→400). §6 해당 항목을 해결됨으로 갱신. ADR-012 §A 각주 참고
- 2026-10-05: §1에 "시스템 테마(색상 템플릿) 설정" 구현 완료 반영(ADR-014, 마이그레이션 V16, `com.lightalm.theme` 패키지). 구현 중 발견한 전역 버그(`@PreAuthorize` 거부 시 403 대신 500 응답)를 `GlobalExceptionHandler`에 핸들러 추가로 해소 — §6에 기록. `mvnw test` 136개 전체 통과, 프론트엔드 `tsc`/`build`/`lint` 통과. qa-tester 검증은 아직 진행 전(브라우저 기반 시각 검증 포함)
- 2026-10-05: §1 ADR-012 행 갱신 — Phase 23(워크플로우 전이 규칙) 구현 완료로 **ADR-012 전체 완료**(마이그레이션 V17, `com.lightalm.workflow` 패키지). 기존 `RequirementService.changeStatus`/`IssueService.changeStatus`/`ApprovalService.decide()`(DRAFT→APPROVED 실제 실행 지점)에 `WorkflowTransitionPolicy` 훅을 추가했다. 규칙 미등록 프로젝트는 100% 기존과 동일하게 자유 전이로 동작(회귀 없음), 시스템 ADMIN/PROJECT_ADMIN은 화이트리스트와 무관하게 항상 전이 가능(lock-out 방지). `mvnw test` 157개 전체 통과(기존 136개 + 신규 21개), 프론트엔드 `tsc`/`build`/`lint` 통과. §6에 "실제 Postgres 환경 curl 검증 미수행" 리스크 신규 기록. 설계 대비 차이점은 ADR-012 §D 각주 참고
- 2026-10-05: qa-tester가 Phase 23을 실제 Postgres+API로 검증해 항목1~5,7,8 PASS, 항목6(DRAFT→APPROVED AND조건)에서 치명적 버그 발견 반려 — `ApprovalService.decide()`가 lock-out 우회 포함 메서드를 호출해 워크플로우 체크가 실제로는 절대 작동하지 않는 죽은 코드였다. lock-out 우회 없는 `requireRegisteredTransition(...)`을 신설해 `decide()`가 이를 쓰도록 수정, 로컬 docker-compose 실제 Postgres+`mvnw spring-boot:run`+curl로 developer가 동일 방식 재검증(화이트리스트 미등록 시 400 거부+전체 롤백, 등록 후 200 성공, 무규칙 프로젝트는 여전히 200 — 모두 확인). `mvnw test` 161개 통과(이전 157개+신규 4개). §6의 "실제 Postgres 검증 미수행" 리스크를 해결됨으로 갱신, 이번 버그 수정 내역으로 교체. 설계 대비 차이점은 ADR-012 §D 각주(qa-tester 반려 및 수정 각주) 참고. qa-tester의 최종 재승인은 아직 이 작업 턴에 포함되지 않음
- 2026-10-05: §1의 "Phase 16~19" 행을 분리 — **Phase 16(리뷰 사이클+베이스라인) 구현 완료, qa-tester 검증 전**, Phase 17~19는 여전히 구현 전(ADR-008). 마이그레이션 `V18__create_review_and_baseline_tables.sql`, 패키지 `com.lightalm.review`·`com.lightalm.baseline`(docs/CLAUDE.md 신규 코드 규칙: Command/Query 서비스 분리, 요청 DTO record, Summary/Detail 응답 분리). `mvnw test` 190개 전체 통과(기존 161개 + 신규 29개), 프론트엔드 `tsc`/`build` 통과·`lint` 신규 경고 0건(기존 경고 4건 유지). 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 DoD 2항목(리뷰 결정/닫기 후 대상 status 불변, 베이스라인 diff 정확성)을 실측 확인 — 운영 공용 DB에는 적용하지 않음. §6에 Phase 16 관련 리스크(운영 DB 미적용, 고아 리뷰 사이클, 감사 로그 미연동, diff N회 조회, 커스텀 필드 미포함) 기록. 설계 대비 차이점은 03-data-model.md §3.19·§3.21, 04-api.md §4.17~4.18, 05-frontend.md §5.11~5.12 구현 각주 참고
- 2026-10-05: qa-tester 반려 3건 수정 — (Phase 16, Low) 역직렬화 오류(enum 값 오류·본문 누락·JSON 문법 오류)·경로 변수 타입 오류가 500이던 문제를 `GlobalExceptionHandler`에 400 핸들러 추가로 해소, (Phase 22, Medium) 폐기된 PRIORITY 값 보유 항목의 수정 차단 해소(`requireValidValueForChange`), (Phase 22, Medium) `enumKey=PRIORITY`/`baseEnum` 불일치 집합 생성 거부. 스키마 변경 없음(V19 미생성). `mvnw test` 199개 통과(이전 190개 + 신규 9개). 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 qa 재현 시나리오 전부 재확인. §1: Phase 16·22는 "반려 수정 완료, qa-tester 재확인 전", Phase 23은 **qa-tester 최종 재승인 완료**로 갱신. §6 해당 항목 해결됨 처리. 상세는 ADR-012 §C 각주, 04-api.md §4.1 각주
- 2026-10-05: qa-tester 재검증 — Phase 16(역직렬화 오류 4건 모두 400, DoD (a) 대상 status 불변 회귀 재확인) **PASS**, Phase 22(폐기 값 보유 항목 수정 허용·재변경/신규 생성 거부, enumKey/baseEnum 불일치 조합 거부) **PASS**. `mvnw test` 199개 통과, 프론트 build 통과. 수정 전에 생성된 불일치 PRIORITY 집합이 남는 한계는 그대로(§6) — 운영 DB에 해당 집합이 있는지는 배포 전 별도 확인 필요
- 2026-10-05: ADR-015(권한 체계 정리) 구현 — §1에 회원가입·라이센스 행을 "ADR-015로 제거됨"으로 바꾸고 ADR-015 행 추가(구현 완료, qa-tester 검증 전). §5에 "계정·프로젝트 생성은 System Admin만, 활성 ADMIN 최소 1명 유지, 역할 표시명" 추가. §6의 `LICENSE_SIGNING_SECRET`·라이센스 복구 경로 리스크를 [해결됨 — 기능 제거]로, DB 직접 수정 시 활성 ADMIN 0명 가능·세션 권한 미반영·운영 배포 전 확인 사항·동시성 IT 미실행 리스크 신규 기록. 마이그레이션 없음. `mvnw test` 198개 통과(199 − 18 + 17)
- 2026-10-06: qa-tester ADR-015 검증 결과 반영 — §1 ADR-015 행을 **qa-tester 검증 통과**로(판정 가능한 DoD 전부 PASS, 화면 DoD 7·8·9·15·16d 브라우저 미확인, `LastActiveAdminConcurrencyIT` 미실행). 검증 중 발견된 결함 2건을 ADR-016으로 수정 — [High] 강등·비활성 사용자 기존 세션의 권한 유지(자기 복권·ADMIN 생성) → `SessionPrincipalRefreshFilter`(요청마다 `users` 재조회), [Low] `User` lost update → `@DynamicUpdate`. §1에 ADR-016 행 추가(구현 완료, qa-tester 검증 전), §6의 "세션 권한 즉시 미반영"을 [해결됨 — ADR-016]으로, lost update 해결·브라우저 미확인·ADR-016 신규 리스크(요청당 SELECT 1회, 다음 요청부터 반영, 비밀번호 변경은 세션 유지, 배포 직후 비활성 계정 세션 401) 기록. 마이그레이션 없음. `mvnw test` 208개 통과(198 + 10)
- 2026-10-06: qa-tester ADR-016 검증 통과 — DoD (a)~(e) 전부 PASS(강등+비활성 기존 세션 자기 복권·ADMIN 생성 401, 강등만 403·재승격 시 200, 비활성 USER 쓰기 401, 인증 회귀 없음, `@DynamicUpdate` 실제 SQL이 변경 컬럼만 UPDATE). `mvnw test` 208개 통과. ADR-015 핵심(마지막 ADMIN 400, 프로젝트 생성 403/201, 동시 상호 강등 6라운드) 회귀 재확인. 비차단 관찰 2건 문서 반영: permitAll 경로라도 인증 세션 쿠키가 있으면 재검증되어 비활성 사용자에게 1회 401(06-auth §6.4), `PESSIMISTIC_WRITE`의 실제 SQL은 `FOR NO KEY UPDATE`(04-api, ADR-015 각주)
- 2026-10-06: 운영 실측 버그(PRIORITY 열거형 커스텀 값 `BLOCKER`로 요구사항 생성 시 500) 원인을 운영 공용 DB 스키마 드리프트(예전 V1의 priority CHECK 이름 불일치로 V15 DROP이 no-op)로 판단해 보정 — `V19__drop_legacy_priority_check_constraints.sql` + `GlobalExceptionHandler`의 `DataIntegrityViolationException`→`409 DATA_INTEGRITY_VIOLATION`. §1에 행 추가(qa-tester 검증 전, 운영 미배포, 최신 마이그레이션 V19), §6에 배포 후 운영 확인 필요 항목 추가. 로컬 docker-compose Postgres에서 운영 상태 재현(500) → 핸들러만(409) → V19 적용(201, 나머지 CHECK 유지) 검증. `mvnw test` 209개 통과. ADR-012 §C.3 각주, 03-data-model.md, 04-api.md §4.1, 10-deployment.md 부록 G, changelog 동기화
- 2026-10-06: qa-tester V19 드리프트 보정, 409 핸들러 검증 통과 — 임시 DB(`lightalm_v19test`)에서 다른 이름, 자동 이름, 대소문자 혼합 이름의 priority CHECK만 삭제되고 type/status/requirement_level CHECK, 다중 컬럼 CHECK, UNIQUE, 다른 스키마, 다른 테이블 제약은 유지됨, 실패 시 전체 롤백, 재실행 멱등 확인. §6에 스키마 드리프트 전수 점검 권고, 409 WARN 로그 행 데이터 기록 항목 추가
