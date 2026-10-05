> Owner: orchestrator · 실행은 developer | Status: current | Last-reviewed: 2026-10-06
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 8. 단계별 개발 순서 (Claude 구현 지침)

**중요**: 각 Phase는 이전 Phase가 컴파일/실행되는 상태에서 완료된 것으로 간주하고 진행한다. 각 Phase 종료 시 "완료 조건(Definition of Done)"을 스스로 점검한다. **DoD를 통과하면 그 즉시 02-architecture.md §2.5의 자동 Git 커밋/Push 정책에 따라 커밋 후 `origin`(`https://github.com/psung616/LightALM_v2.git`)에 push한다.** 이 커밋/push는 별도 확인 없이 각 Phase마다 자동으로 수행한다.

### Phase 0 — 프로젝트 초기화
1. `light-alm/` 루트에 `backend/`(Spring Initializr 구조 수동 생성 또는 `spring init` 사용: Web, Security, Data JPA, PostgreSQL Driver, Validation, Lombok, Flyway), `frontend/`(Vite React-TS 템플릿) 생성
2. `docker-compose.yml` 작성: `postgres:15` 서비스(포트 5432, DB명 `lightalm`, 계정 `lightalm/lightalm`)
3. `backend/src/main/resources/application.yml` 하나만 작성한다(02-architecture.md §2.4, 10-deployment.md 부록 A 참고). profile 분리 없이 `${DB_HOST:localhost}` 형태의 환경변수 기본값으로 DB 접속정보를 오버라이드 가능하게 하고, JPA `ddl-auto: validate`, Flyway를 활성화한다. Maven은 시스템에 설치하지 않고 **Maven Wrapper(`mvnw`/`mvnw.cmd`)를 프로젝트에 포함**시켜, 이후 모든 빌드/실행 명령은 `mvn` 대신 `mvnw`/`mvnw.cmd`로 실행한다.
4. `git init` → `.gitignore` 작성(02-architecture.md §2.2 구조 기준: `target/`, `node_modules/`, `dist/`, `.env` 등) → `git remote add origin https://github.com/psung616/LightALM_v2.git` → 최초 커밋(`Phase 0: 프로젝트 초기화`) → `git push -u origin main`(02-architecture.md §2.5 참고)
5. **DoD**: `docker compose up -d postgres` 후 `mvnw.cmd spring-boot:run`(Windows)으로 백엔드가 에러 없이 기동, `npm run dev`로 프론트가 기동. `git remote -v`에 `origin`이 `LightALM_v2` 저장소로 잡혀 있고 최초 커밋이 push되어 있음을 확인

### Phase 1 — DB 스키마 (Flyway) & JPA 엔티티
1. `db/migration/V1__init.sql`에 03-data-model.md §3의 모든 테이블 DDL 작성(제약조건 포함)
2. 03-data-model.md §3의 모든 엔티티 클래스 작성(Lombok `@Getter/@Setter/@Builder`, 연관관계는 지연 로딩 `FetchType.LAZY` 기본)
3. **DoD**: 애플리케이션 기동 시 Flyway 마이그레이션이 성공적으로 적용됨

### Phase 2 — 인증/인가 & 사용자 관리
1. `SecurityConfig` 작성(세션 기반, JSON 로그인 성공/실패 핸들러, CORS)
2. `CustomUserDetailsService`, 로그인/로그아웃/`me` API 구현
3. 사용자 CRUD API 구현(관리자 전용), 최초 관리자 계정을 Flyway 시드 데이터(`V2__seed_admin.sql`)로 삽입(예: `admin/admin1234`, 최초 로그인 후 변경 권장 안내)
4. **DoD**: Postman/curl로 로그인 → 세션 쿠키로 `/api/auth/me` 호출 성공

### Phase 3 — 프로젝트 & 멤버 관리
1. Project, ProjectMember 엔티티/리포지토리/서비스/컨트롤러 구현
2. 프로젝트 키/이슈·요구사항 시퀀스 채번 로직(동시성 고려: `SELECT ... FOR UPDATE` 또는 DB 시퀀스 활용)
3. `ProjectMemberService.requireRole()` 공통 권한 검사 유틸 구현
4. **DoD**: 프로젝트 생성 → 생성자가 자동 PROJECT_ADMIN으로 등록되는지 확인

### Phase 4 — 요구사항 (Requirement)
1. CRUD API + 계층 구조(상위/하위) 조회 API 구현. 요청/응답 DTO에 `dueDate`(신규, 03-data-model.md §3.4) 포함
2. `req_key` 자동 채번(`{projectKey}-R{seq}`)
3. **DoD**: 요구사항 생성/수정/삭제/계층 조회 API 전부 동작, 단위 테스트 작성

### Phase 5 — 이슈 (Issue)
1. CRUD API + 상태 변경 API(`PATCH .../status`) 구현. 요청/응답 DTO에 `dueDate`(신규, 03-data-model.md §3.5) 포함
2. `issue_key` 자동 채번(`{projectKey}-{seq}`)
3. **DoD**: 이슈 생성/수정/삭제/상태변경 API 전부 동작, 단위 테스트 작성

### Phase 6 — 추적성 & 댓글
1. TraceabilityLink CRUD + 매트릭스 조회 API 구현
2. **(신규)** 상위/하위 추적성 트리 API 구현: `GET .../requirements/{reqId}/traceability-tree`(04-api.md §4.5, §4.7, 05-frontend.md §5.4) — PostgreSQL 재귀 CTE(`WITH RECURSIVE`)로 조상 체인과 자손 트리를 각각 조회한 뒤, 자손 트리 각 노드에 `traceability_links`를 조인해 `linkedIssues`를 채운다
3. **(신규)** 개인화 대시보드 API 구현: `GET /api/me/dashboard`(04-api.md §4.11) — `project_members` 기준 내가 속한 프로젝트 범위에서 나에게 할당된 요구사항/이슈를 상태별 집계 + `dueDate` 기준 overdue/dueSoon 목록 + 프로젝트별 카운트로 반환
4. Comment CRUD 구현(다형 target_type/target_id 처리 공통 로직)
5. **DoD**: 요구사항-이슈 링크 생성 후 매트릭스 API에서 정상 반환 확인. 3단계 이상 깊이의 요구사항 계층(조부모-부모-자식)을 만들어 `traceability-tree` API가 조상 체인과 자손 트리를 정확히 반환하는지 확인. 서로 다른 두 프로젝트에 동일 사용자를 멤버로 넣고 각각 항목을 할당한 뒤 `/api/me/dashboard`가 두 프로젝트 항목을 모두 집계하는지 확인, `dueDate`를 과거/7일 이내/먼 미래로 나눠 넣고 `overdue`/`dueSoon` 분류가 맞는지 확인

### Phase 7 — GitHub 연동
1. GitHub API 클라이언트(`GithubApiClient`) 구현: 커밋 조회(`GET /repos/{owner}/{repo}/commits/{sha}`), PR 조회(`GET /repos/{owner}/{repo}/pulls/{number}`)
2. 수동 연결 API + Webhook 수신 API 구현(HMAC-SHA256 서명 검증 포함)
3. 커밋 메시지/PR 제목에서 `{PROJECT_KEY}-\d+` 패턴 정규식 파싱 로직 구현
4. **DoD**: 테스트용 GitHub 저장소로 실제 Webhook 호출(또는 curl로 모의 payload 전송) 시 `git_links` 레코드 생성 확인

### Phase 8 — Jenkins 연동
1. Jenkins API 클라이언트 구현: 빌드 트리거(Basic Auth POST)
2. Webhook 수신 API 구현(`X-Jenkins-Token` 검증), `jenkins_builds` upsert 로직
3. **DoD**: curl로 모의 Jenkins webhook payload 전송 시 빌드 레코드 생성/갱신 확인

### Phase 9 — 프론트엔드 기반 구축
1. Vite 프로젝트에 react-router-dom, axios, @tanstack/react-query, Tailwind, **mermaid**(05-frontend.md §5.5 Workflow 차트용) 설치/설정
2. axios 인스턴스(`withCredentials: true`, 401 응답 시 `/login`으로 리다이렉트하는 인터셉터) 구성
3. `AuthContext` + `ProtectedRoute` 구현 — 앱 마운트 시 `GET /api/auth/me`로 세션 확인, 확인 완료 전(`isLoading`)에는 라우트 대신 전체 화면 로딩 표시, 완료 후 인증 여부에 따라 원래 경로 렌더링 또는 `/login` 리다이렉트(05-frontend.md §5.3 참고)
4. 공통 레이아웃(05-frontend.md §5.3) 구현: `TopNavbar`(로고=홈 버튼, 내 작업/사용자 관리 링크, `/`·`/my-tasks`·`/admin/users`에서 사용)와 `ProjectLayout`(사이드바 + "← 프로젝트 목록" 링크, `/projects/:projectId/**`에서 사용) 두 컴포넌트로 구현한다. 05-frontend.md §5.1 라우트 테이블의 인증 필요 라우트는 경로에 맞는 레이아웃으로 감싼다
5. **DoD**: 로그인 → 프로젝트 목록 화면까지 라우팅 정상 동작. `/`을 포함한 임의의 화면에서 새로고침(F5)해도 로그인 상태가 유지된 채 해당 화면이 다시 정상적으로 나타나야 하며, 어느 화면에서든 헤더의 홈 버튼(로고) 또는 사이드바의 "← 프로젝트 목록"을 클릭하면 `/`로 이동해야 한다

### Phase 10 — 프론트엔드 화면 구현 (아래 순서 권장)
1. 로그인 화면
2. 프로젝트 목록/생성 화면
3. 프로젝트 대시보드
4. 요구사항 목록/상세 화면 — 상세 화면에 Workflow 차트 미니 위젯(05-frontend.md §5.5)과 "추적성 트리로 보기" 링크(05-frontend.md §5.4) 포함
5. 이슈 목록(칸반+테이블)/상세 화면 — 상세 화면에 Workflow 차트 미니 위젯(05-frontend.md §5.5) 포함
6. 추적성 매트릭스 화면(매트릭스 뷰 + 트리 뷰 토글, 05-frontend.md §5.4)
7. 프로젝트 설정 화면(멤버, GitHub/Jenkins 연동)
8. 사용자 관리 화면(관리자)
9. 내 작업 화면(개인화된 대시보드, 04-api.md §4.11·05-frontend.md §5.2) — 마감 임박/기한 초과 위젯 포함
10. **DoD**: 각 화면에서 대응하는 API가 정상 호출되고 로딩/에러 상태가 처리됨

### Phase 11 — 통합 점검 및 마무리
1. **`docker-compose.yml` 단일 파일**에 backend/frontend 서비스를 추가한다(10-deployment.md 부록 C의 `docker-compose.test.yml` 분리안은 실제로는 채택하지 않음 — 10-deployment.md 부록 B가 곧 로컬/테스트 겸용이다). `frontend`는 빌드 시 `VITE_API_BASE_URL` 등 build args를 받고, `frontend/nginx.conf`가 `/api/`를 `backend:8080`으로 프록시하도록 구성한다(02-architecture.md §2.4 참고, 필수).
2. README.md 작성: 로컬/사내 테스트 서버 실행 방법(02-architecture.md §2.4, 10-deployment.md 부록 A~B), 초기 관리자 계정, GitHub/Jenkins Webhook 설정 가이드, Git 저장소 정책(02-architecture.md §2.5·10-deployment.md 부록 D — `origin`은 `https://github.com/psung616/LightALM_v2`, 사내 Git 서버는 현재 비활성)을 안내한다.
3. 백엔드 단위 테스트(서비스 레이어 위주) 및 통합 테스트(컨트롤러, `@SpringBootTest` + Testcontainers 권장) 최소 커버리지 확보. **Windows + Docker Desktop 환경에서 Testcontainers가 기본 named pipe(`npipe:////./pipe/docker_engine`)를 찾지 못해 실패할 수 있음** — Docker Desktop이 실제로 쓰는 pipe(`npipe:////./pipe/dockerDesktopLinuxEngine`, `docker context inspect`로 확인)를 `DOCKER_HOST`로 지정해도 API 버전 협상 문제로 계속 실패하는 경우가 있었다. 이 경우 Testcontainers를 쓰는 통합 테스트는 파일명을 `*IT.java`로 짓고 `maven-failsafe-plugin`을 추가해 `mvn test`(surefire, 기본 포함 패턴 `*Test.java`)에서는 제외하고 `mvn verify`(failsafe)에서만 실행되도록 분리한다. 이렇게 하면 `mvn test`는 항상 빠르고 안정적으로 통과한다.
4. **DoD**: `docker compose up --build`로 전체 스택(postgres + backend + frontend) 기동 → 브라우저에서 로그인부터 요구사항/이슈/추적성·GitHub/Jenkins 연동 데이터 표시까지 End-to-End 시나리오 수동 검증 완료. (GitHub/Jenkins 연동 자체의 실제 API 호출/Webhook 왕복 검증은 Phase 7·8 단계에서 curl 모의 payload로 이미 별도 완료했다는 전제)

---

> Phase 0~11(원래의 Light ALM MVP)은 완료되어 이미 운영 중이다(git 커밋 이력 기준). 이번 v2 스코프 확장(01-scope.md §1.2)에서 새로 추가된 Phase 12(테스트케이스 & 테스트 실행), Phase 13(릴리스/버전 관리), Phase 14(변경 이력/감사 로그), Phase 15(승인 워크플로우)까지 네 개 Phase 모두 2026-08-04에 구현 완료됐다.

### Phase 12 — 테스트케이스 & 테스트 실행 ✅ 구현 완료 (2026-08-04)
1. `test_cases`/`test_runs`/`test_run_results` Flyway 마이그레이션 + JPA 엔티티 작성(03-data-model.md §3.11~3.13)
2. 04-api.md §4.12~4.13 API 구현
3. 05-frontend.md §5.6~5.7 프론트 화면 구현

### Phase 13 — 릴리스/버전 관리 ✅ 구현 완료 (2026-08-04)
1. `releases`/`release_items` 마이그레이션 + 엔티티 작성(03-data-model.md §3.14~3.15). 같은 마이그레이션에서 `ALTER TABLE test_runs ADD COLUMN release_id BIGINT REFERENCES releases(id) ON DELETE SET NULL;`도 실행한다(Phase 12는 `releases`가 아직 없어 이 컬럼 없이 `test_runs`를 생성했다 — 03-data-model.md §3.12 참고).
2. 04-api.md §4.14 API 구현(릴리스 노트 생성 로직 포함)
3. 05-frontend.md §5.8 프론트 화면 구현

### Phase 14 — 변경 이력/감사 로그 ✅ 구현 완료 (2026-08-04)
1. `audit_logs` 마이그레이션 + 엔티티 작성(03-data-model.md §3.16)
2. 서비스 레이어에 요구사항/이슈 생성·수정·상태변경·삭제 시 자동 기록 훅 추가 — 이번 Phase는 명시된 범위대로 Requirement/Issue만 기록하며, TestCase/Release/Project/User/TraceabilityLink는 `AuditTargetType`에 값만 정의해두고 실제로 기록 훅을 달지 않았다(범위 밖).
3. 04-api.md §4.15 API 구현
4. 05-frontend.md §5.9 프론트 화면 구현(요구사항/이슈 상세 화면 "이력" 카드)

### Phase 15 — 승인 워크플로우 ✅ 구현 완료 (2026-08-04)
1. `approval_requests` 마이그레이션 + 엔티티 작성(03-data-model.md §3.17). `requested_by`는 문서상 "NOT NULL + ON DELETE SET NULL"로 상충되게 적혀 있었는데, 이 저장소의 다른 모든 `created_by`류 FK와 동일하게 nullable + `ON DELETE SET NULL`로 통일해 구현했다.
2. 요구사항 상태 전이 로직에 `DRAFT→APPROVED` 게이트 추가(직접 전이 차단, 승인 요청 경유 강제) — `RequirementService.changeStatus`에서만 차단하며, 그 외 모든 전이는 여전히 자유 전이다.
3. 04-api.md §4.16 API 구현
4. 05-frontend.md §5.10 프론트 화면 구현(승인함 페이지, 요구사항 상세 화면의 "승인 요청" 버튼)
5. **의존성**: 승인 결정 시 `audit_logs`에 `STATUS_CHANGE`(승인 시에만) + `APPROVE`/`REJECT` 기록을 남기며, Phase 14의 `AuditLogService`를 재사용한다.

---

> Phase 16~19는 v3 확장(01-scope.md §1.2 v3 항목, 2026-08-08)이다. **2026-10-05 기준 Phase 16만 구현 완료(qa-tester 검증 통과), Phase 17~19는 아직 구현되지 않았다.** 착수 전 02-competitive-reference.md(참고 배경/저작권 준수 원칙)와 ADR-008을 먼저 확인할 것. architect 서브에이전트가 설계를 소유하고, developer 서브에이전트가 구현하며, qa-tester 서브에이전트가 검증한다(ROLES.md 참고).

### Phase 16 — 리뷰 사이클 & 베이스라인 ✅ 구현 완료 (2026-10-05, qa-tester 경미 1건 반려 → 수정 완료, 재검증 통과)

> **[2026-10-05 구현 각주]** 마이그레이션은 v4(Phase 20~23, V13~V17) 이후에 착수해 실제 번호가 **`V18__create_review_and_baseline_tables.sql`**(4개 테이블 한 파일)이다. 패키지 `com.lightalm.review`(`ReviewCycleCommandService`/`ReviewCycleQueryService`), `com.lightalm.baseline`(`BaselineCommandService`/`BaselineQueryService`/`BaselineDiffService` + `BaselineSnapshotFactory`/`BaselineSnapshotJsonCodec`). 단위 테스트 29개 추가(`mvnw test` 161 → 190, 전체 통과). DoD 두 항목은 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 developer가 실측 확인했다: (1) 참여자 2명 전원 APPROVE 후 사이클 CLOSED까지 진행해도 대상 요구사항 `status=DRAFT`·`updated_at` 불변, 비참여자 PROJECT_ADMIN·시스템 ADMIN의 결정 기록은 403, (2) 베이스라인(요구사항·이슈·테스트케이스) 생성 → 요구사항 제목/설명/상태/담당자 수정 + 이슈 삭제 → diff가 요구사항 MODIFIED(title·description·status·assignedToId 4필드 정확), 이슈 DELETED, 테스트케이스 UNCHANGED로 반환. 운영 공용 DB에는 적용하지 않았다. 설계 대비 차이점은 03-data-model.md §3.19·§3.21, 04-api.md §4.17~4.18, 05-frontend.md §5.11~5.12의 구현 각주 참고. **qa-tester의 독립 검증은 아직이다.**

1. `review_cycles`/`review_participants` 마이그레이션 + 엔티티 작성(03-data-model.md §3.18~3.19)
2. `baselines`/`baseline_items` 마이그레이션 + 엔티티 작성(§3.20~3.21). 스냅샷 저장 로직은 베이스라인 생성 시점에 대상 엔티티의 현재 필드 값을 JSONB로 직렬화하는 서비스 메서드로 구현
3. diff 계산 로직 구현(저장된 스냅샷 vs 현재 값을 필드 단위로 비교, 별도 테이블 없이 조회 시점에 계산)
4. 04-api.md §4.17~4.18 API 구현
5. 05-frontend.md §5.11~5.12 프론트 화면 구현
6. **DoD**: 리뷰 사이클 생성 후 여러 참여자가 각자 결정을 기록해도 대상(요구사항/이슈)의 status가 자동으로 바뀌지 않는지 확인(§3.19 원칙 검증 — qa-tester가 특히 이 항목을 확인할 것). 베이스라인 생성 후 원본 요구사항을 수정하고 diff API를 호출해 변경분이 정확히 반환되는지 확인

### Phase 17 — 위험 관리
1. `risks` 마이그레이션 + 엔티티 작성(03-data-model.md §3.22)
2. 기존 `traceability_links` CHECK 제약에 `'RISK'` 추가하는 마이그레이션 작성(**기존 V1~V7 파일은 수정하지 않고 새 `V{n+1}__` 파일에서 `ALTER TABLE`로 처리**, ADR-002 원칙)
3. risk_score(likelihood × impact) 계산 로직을 서비스 레이어에 구현(저장하지 않고 응답 시 계산)
4. 04-api.md §4.19 API 구현
5. 05-frontend.md §5.13 프론트 화면 구현(히트맵 뷰 포함)
6. **DoD**: 위험 생성 후 기존 §4.7 추적성 링크 API로 요구사항에 연결되는지 확인, likelihood/impact 조합별로 risk_score가 정확히 계산되는지 확인(1~9 범위)

### Phase 18 — 요구사항 문서 뷰 & 변형 관리
1. `requirements.order_index` 컬럼 추가 마이그레이션(§3.23 참고, 새 `V{n+1}__` 파일)
2. `variants`/`requirement_variants` 마이그레이션 + 엔티티 작성(§3.23)
3. 04-api.md §4.20 API 구현(document-view는 기존 계층 조회 로직을 order_index 정렬로 재구성)
4. 05-frontend.md §5.14 프론트 화면 구현
5. **DoD**: 3단계 이상 깊이의 요구사항 계층을 만들고 문서 뷰에서 순서대로 렌더링되는지 확인, 변형을 하나 만들어 일부 요구사항을 EXCLUDED로 지정한 뒤 `variantId` 쿼리로 필터링되는지 확인

### Phase 19 — 대시보드 위젯 & 리포트 내보내기
1. `dashboard_widget_configs` 마이그레이션 + 엔티티 작성(03-data-model.md §3.24)
2. Excel 내보내기(Apache POI 등) / PDF 내보내기 라이브러리 선정 및 서비스 구현(별도 저장 없이 실시간 스트리밍 응답)
3. 04-api.md §4.21 API 구현
4. 05-frontend.md §5.15 프론트 화면 구현(위젯 편집 모드, 내보내기 버튼)
5. **DoD**: 위젯 설정을 저장/재조회했을 때 순서·구성이 유지되는지 확인, 추적성 매트릭스를 Excel로 내보내 실제 파일이 열리고 데이터가 정확한지 확인

---

> 아래는 Phase 번호를 새로 부여하지 않은 v4 유지보수 항목이다(v4 Phase 20~23·ADR-011·ADR-013·ADR-014의 구현 기록은 각 ADR 본문과 CURRENT-STATE.md §1에 있다).

### v4 유지보수 — 권한 체계 정리 (ADR-015) ✅ 구현 완료 (2026-10-05), qa-tester 검증 통과 (2026-10-06)
1. 회원가입·라이센스 기능 제거: 백엔드 `com.lightalm.auth`·`com.lightalm.license` 패키지 22개 파일과 테스트 5개 파일 삭제, `SecurityConfig` `permitAll()` 정리, `JsonAuthenticationSuccessHandler` 라이센스 게이트 제거, `UserService.create()` 라이센스/시트 검사 제거, `GlobalExceptionHandler` 라이센스 핸들러 3개 제거, `UserRepository.countByEnabledTrue()` 제거, `application.yml`의 `light-alm.license.signing-secret` 제거. 프론트 `SignupPage`/`AdminLicensesPage`/`api/license.ts`/`types/license.ts` 삭제, 라우트·`TopNavbar` 링크·로그인 화면 회원가입 링크 제거. DB(`licenses`, V10 `LICENSE` CHECK)와 `AuditTargetType.LICENSE`는 보존
2. 역할 표시명: `frontend/src/auth/roleDisplayNames.ts` 신규, `AdminUsersPage`/`ProjectSettingsPage` 멤버 탭/`WorkflowRuleSettingsTab` 적용(워크플로우 최소 역할에서 VIEWER 신규 선택 불가)
3. 마지막 활성 System Admin 보호: `com.lightalm.user.service.SystemAdminRetentionPolicy`, `com.lightalm.user.domain.LastActiveAdminRemovalException`, `UserRepository.findEnabledAdminsForUpdate()`(`PESSIMISTIC_WRITE`), `GlobalExceptionHandler` `400 LAST_ACTIVE_ADMIN`, `AdminUsersPage` 실패 메시지 표시
4. 프로젝트 생성 System Admin 전용: `ProjectController.create()` `@PreAuthorize("hasRole('ADMIN')")`, 프론트 `/projects/new`를 `AdminRoute` 아래로 이동, `ProjectListPage` "새 프로젝트" 버튼·빈 목록 안내문 ADMIN 조건부
5. 마이그레이션 없음(V18 최신 유지)
6. 테스트: `mvnw test` 199 → 198개(삭제 18개: SelfSignupServiceTest 3, LicenseCommandServiceTest 2, LicenseEnforcementServiceTest 7, LicenseFileParserTest 3, LicenseSignatureVerifierTest 3 / 신규 17개: SystemAdminRetentionPolicyTest 5, UserServiceTest 8, ProjectControllerAuthorizationTest 3, GlobalExceptionHandlerTest +1). 동시 강등 통합 테스트 `LastActiveAdminConcurrencyIT`(Testcontainers, `mvn verify` 전용)는 작성·컴파일만 했고 이 환경에서는 실행하지 못함
7. **DoD**: ADR-015 "검증 (DoD)" 1~17. developer가 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 백엔드 항목을 실측했고(동시 강등은 psql 세션 락 + 병렬 curl), 화면 항목(8·9·15·16d)은 qa-tester 브라우저 확인 대상
8. **qa-tester 결과(2026-10-06)**: 판정 가능한 DoD 전부 PASS. 화면 항목 DoD 7·8·9·15·16d는 코드상 PASS(브라우저 미확인). `LastActiveAdminConcurrencyIT` 미실행(이 환경 Testcontainers 불가). 검증 중 별도 결함 2건 발견 → ADR-016으로 수정(아래)

### v4 유지보수 — 세션 사용자 상태 요청마다 재검증 + User lost update 방지 (ADR-016) ✅ 구현 완료 (2026-10-06, qa-tester 검증 통과)
1. 결함 1 [High](qa-tester ADR-015 검증 중 발견): 강등·비활성화된 사용자의 기존 세션이 이전 권한을 유지해 자기 복권(`PUT /api/users/{본인} {systemRole:ADMIN, enabled:true}` 200)·ADMIN 생성(201)·비활성 USER 쓰기(201)가 가능했음 → `com.lightalm.security.SessionPrincipalRefreshFilter`(`OncePerRequestFilter`, `SecurityConfig`에서 `AuthorizationFilter` 앞에 `addFilterBefore`, 빈 미등록). 요청마다 `UserRepository.findById` — 비활성/삭제면 세션 무효화 + `401`, `systemRole` 변경이면 principal/authorities 교체 + 세션 저장
2. 결함 2 [Low]: `User` 엔티티 전체 컬럼 UPDATE로 인한 lost update(활성 ADMIN 0명 가능) → `com.lightalm.domain.User`에 `@DynamicUpdate`
3. 마이그레이션 없음(V18 최신 유지)
4. 테스트: `mvnw test` 198 → 208개(신규 10개: SessionPrincipalRefreshFilterTest 7, SessionPrincipalRefreshMethodSecurityTest 2, UserDynamicUpdateMappingTest 1)
5. **DoD**(qa-tester 판정): (a) ADMIN 세션 유지 중 다른 ADMIN이 그 사용자를 `USER`+`enabled=false`로 바꾸면, 기존 세션의 자기 복권 `PUT`·`POST /api/users`·`GET /api/auth/me`가 모두 `401`, DB 불변 (b) `USER`로만 강등(활성 유지)하면 기존 세션의 `@PreAuthorize` ADMIN API(`GET/POST /api/users`, `PUT /api/users/{id}`, `POST /api/projects`)가 `403`, `GET /api/auth/me`가 `systemRole:USER`, 멤버가 아닌 프로젝트 조회 `403`(서비스 `isAdmin()` 반영). 재승격하면 같은 세션에서 다시 `200` (c) 비활성화된 USER의 기존 세션으로 요구사항 생성·조회 `401` (d) 정상 로그인 200, 잘못된 비밀번호 401, 비활성 계정 로그인 401, CSRF 토큰 없는 POST 403, 비인증 `/api/auth/me` 401, `/api/public/theme` 200, 로그아웃 후 `/api/auth/me` 401 (e) email만 수정하는 `PUT /api/users/{id}`의 SQL이 `update users set email=?,updated_at=? where id=?`(system_role/enabled 미포함). developer가 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 (a)~(e) 실측
