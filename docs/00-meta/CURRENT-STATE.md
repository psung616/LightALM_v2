> Owner: orchestrator (전체 세션이 매 Phase 종료 시 갱신) | Status: current | Last-reviewed: 2026-08-08
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
| Phase 16~19 (v3: 리뷰 사이클+베이스라인 / 위험 관리 / 문서 뷰+변형 관리 / 대시보드 위젯+리포트 내보내기) | 🔲 설계 완료, **구현 전** (2026-08-08 설계, ADR-008) |
| v4: 회원가입(Self-Signup) + 라이센스 파일 관리 | ✅ 완료, qa-tester 검증 통과 (2026-10-03, ADR-011) |
| v4: 프로젝트 Configuration(커스텀 필드/폼레이아웃/열거형/워크플로우) | ✅ **Phase 20~23 전부 구현 완료 — ADR-012 전체 완료(2026-10-05)**. Phase 20~21은 qa-tester 검증 통과(반려 1건 수정 후). Phase 22(열거형)는 구현 완료, qa-tester 검증 전. **Phase 23(워크플로우)은 qa-tester 1차 검증에서 치명적 버그(항목6, DRAFT→APPROVED AND조건 무력화) 반려 → developer가 수정 후 동일 방식(로컬 docker-compose 실제 Postgres+API)으로 재검증 완료(2026-10-05) — qa-tester 최종 재승인 전** (2026-10-03 설계, ADR-012) |
| v4: 프로젝트 사이드바 — PRD/SRS/Defect/TestCase 유형별 트리 | ✅ 완료, qa-tester 검증 통과 (2026-10-04, ADR-013) |
| v4: 시스템 테마(색상 템플릿) 설정 | ✅ 완료, qa-tester 검증 통과 (2026-10-05, ADR-014. 마이그레이션 `V16__create_system_theme_settings.sql`, `com.lightalm.theme` 패키지) |

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
| Jenkins | `https://jenkins.ondalprincess.synology.me/job/ALM_Pipeline/` |

## 6. 알려진 리스크 / TODO (아직 해결 안 됨)

- GitHub PAT, Jenkins API 토큰이 DB에 평문 저장됨 — 운영 전환 시 암호화 필요(07-integrations.md §7.4)
- DB 계정(`postgres/postgres`)이 `docker-compose.yml`/Jenkinsfile에 평문으로 커밋되어 있음 — Jenkins Credentials로 이전 필요(10-deployment.md 부록 E)
- Flyway `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`로 체크섬 검증을 우회 중 — 스키마 드리프트를 놓칠 수 있는 상태
- `synology` push가 곧바로 운영 배포로 이어지는데 별도의 배포 승인/검토 게이트가 없음(ADR-005 리스크 항목 참고)
- `synology` remote 저장소명(`ALM_Repository`)이 GitHub 저장소명(`LightALM_v2`)과 달라 혼동 가능성 있음
- `LICENSE_SIGNING_SECRET` 환경변수 기본값이 개발용 평문(`light-alm-dev-only-insecure-secret-change-in-production`)으로 설정돼 있음 — **운영 배포 전 반드시 고유한 값으로 교체 필요.** 교체하지 않으면 기본값을 아는 누구나 유효한 라이센스 파일을 위조할 수 있다(ADR-011, HMAC 대칭키 설계의 의도된 한계)
- 라이센스 만료/부재 시 복구 경로가 시스템 `ADMIN` 로그인 하나뿐임 — **최소 1개의 활성 ADMIN 계정을 항상 유지**해야 한다. 모든 ADMIN이 비활성화된 상태에서 라이센스까지 만료되면 아무도 새 라이센스를 올릴 수 없는 복구 불가 상태가 된다(ADR-011)
- **[해결됨, 2026-10-05]** `custom_field_definitions.enumeration_set_id` FK 미검증 리스크 — Phase 22(V15 마이그레이션)가 고아값 정리(`UPDATE ... SET enumeration_set_id = NULL WHERE NOT EXISTS (...)`) 후 `fk_custom_field_definitions_enumeration_set` FK(`ON DELETE SET NULL`)를 추가해 해소됐다(ADR-012 §C 각주).
- Phase 22(PRIORITY 열거형 확장) 구현 후 새로 생긴 리스크: `requirements`/`issues`/`test_cases`의 `priority` 컬럼은 더 이상 DB CHECK 제약이 없다 — 유효성 검증은 전부 `EnumerationValueValidator`(애플리케이션 레벨)에 의존한다. 이 validator를 거치지 않는 쓰기 경로(향후 추가되는 bulk import, 관리 스크립트 등)가 생기면 DB 안전망 없이 잘못된 값이 저장될 수 있다(ADR-012 §C "결과" 섹션에 이미 예견된 리스크).
- Phase 22는 백엔드/DB만 구현했다 — PROJECT_ADMIN이 PRIORITY 값을 프로젝트별로 확장해도, 프론트엔드 "열거형" 설정 탭과 요구사항/이슈/테스트케이스 생성·수정 화면의 PRIORITY 드롭다운은 아직 고정된 LOW/MEDIUM/HIGH/CRITICAL만 보여준다(API는 정상 동작하지만 화면에 반영되지 않음). 커스텀 필드 SINGLE_SELECT/MULTI_SELECT를 열거형 집합과 연결해 드롭다운으로 렌더링하는 것도 마찬가지로 미구현 상태라, Phase 20의 "자유 텍스트 입력" 임시 구현이 그대로 남아있다(ADR-012 §C 각주). 이미 API로 커스텀 PRIORITY 값(예: BLOCKER)이 저장된 항목을 수정 화면에서 열면 드롭다운에 해당 옵션이 없어 선택란이 비어 보일 수 있다 — 저장된 값 자체는 안전하게 유지되며 표시만 혼란스럽다(qa-tester 2026-10-05 확인).
- **[해결됨, 2026-10-05]** 커스텀 필드 생성 API(`POST .../config/custom-fields`)가 존재하지 않는 `enumerationSetId`를 애플리케이션 레벨에서 사전 검증하지 않아 Phase 22가 추가한 FK 제약 위반 시 `500 INTERNAL_ERROR`를 반환하던 문제(qa-tester 2026-10-05 발견) — `CustomFieldDefinitionService.create()`에 `FormLayoutService`와 동일한 패턴의 소속 검증(`ProjectEnumerationSetRepository.existsByIdAndProjectId`)을 추가해 `ValidationException`(400)으로 바뀌었다. `UpdateCustomFieldDefinitionRequest`에는 이 필드가 없어(생성 후 불변) 수정은 `create()` 경로에만 적용(ADR-012 §A 각주).
- **[확인 필요]** Phase 20~22 기간 동안 Testcontainers 기반 `*IT.java`(`RequirementListFilterIT`, `SequenceGenerationIT`, `TraceabilityLinkSourceTypeIT`)가 `mvn verify`로 실제 실행되어 통과한 기록이 없다 — developer와 qa-tester 둘 다 이 Windows 환경의 Docker Desktop/Testcontainers 호환성 문제로 `mvn test`(단위 테스트)까지만 확인했다. Docker가 정상 동작하는 환경(CI 등)에서 `mvn verify` 재확인 필요(ADR-007 원칙 참고).
- **[해결됨, 2026-10-05]** `@PreAuthorize("hasRole('ADMIN')")`가 비-ADMIN을 거부할 때 Spring Security 6.3+의 `AuthorizationDeniedException`(`AccessDeniedException` 하위 타입)을 `GlobalExceptionHandler`가 전용 처리하지 않아 **403이 아니라 500**이 응답되던 전역 버그 — ADR-014(시스템 테마 설정) 구현 중 `ThemeAdminController`를 비-ADMIN으로 실제 curl 호출해 재현했고, `LicenseAdminController` 등 `@PreAuthorize`를 쓰는 기존 모든 ADMIN 전용 컨트롤러에도 동일하게 영향을 주고 있었다. `GlobalExceptionHandler`에 `AccessDeniedException` 전용 핸들러(403/`FORBIDDEN`)를 추가해 해소했다(ADR-014 구현 각주 참고). 이 환경에서 Testcontainers 기반 `*IT.java`는 Docker Engine API 버전 불일치(`client version 1.32 is too old`)로 ryuk 사이드카가 기동하지 못해 여전히 실행할 수 없었다(§6의 "확인 필요" 항목과 동일 현상) — 대신 로컬 docker-compose Postgres + `mvnw spring-boot:run`으로 실제 Postgres에 대해 curl 수동 검증했다.
- **[수정 완료, UI 디스클레이머 처리]** 폼 레이아웃(Phase 21) 설정 화면에서 PROJECT_ADMIN이 CUSTOM(커스텀 필드)을 레이아웃에 배치해 저장해도, 요구사항/이슈/테스트케이스의 생성/수정 화면은 그 배치를 반영하지 않는다(STANDARD 필드만 동적으로 렌더링됨) — 저장/조회 API 자체는 정상 동작하지만 CUSTOM 배치의 실제 화면 효과는 아직 없다. qa-tester 반려 사유였고, 기능을 구현하지 않는 대신 `FormLayoutSettingsTab.tsx`에 Phase 20과 같은 톤의 명시적 디스클레이머(배치된 CUSTOM 필드 행에 "(생성/수정 화면에는 아직 미반영)" 태그, 추가 드롭다운 아래 안내문)를 추가해 사용자가 오인하지 않도록 처리했다 — **UI 안내로 반려는 해소됐지만, CUSTOM 필드 배치 자체가 생성/수정 폼에 영향을 주는 기능은 여전히 미구현 상태**다(ADR-012 §B 각주, 2026-10-05)

- 매핑되지 않은 경로(예: `/api/licenses`처럼 실제로는 `/api/admin/licenses`인 오타성 경로)로 인증된 사용자가 요청하면 `NoHandlerFoundException`이 전역 핸들러의 catch-all로 떨어져 500이 응답됨(404가 맞음) — ADR-014 검증 중 발견된 기존부터 있던 별개 문제, 비차단(qa-tester 2026-10-05)
- **[해결됨, 2026-10-05]** Phase 23(워크플로우 전이 규칙) qa-tester 실제 Postgres+API 검증에서 치명적 버그 발견 및 반려 — `ApprovalService.decide()`(`DRAFT→APPROVED` 실행 지점)가 lock-out 방지 우회가 포함된 `WorkflowTransitionPolicy.requireAllowedTransition(...)`을 그대로 호출해, `decide()`를 호출할 수 있는 모든 실제 호출자(항상 PROJECT_ADMIN 이상)가 그 우회 조건에 걸려 워크플로우 체크가 전혀 작동하지 않는 죽은 코드였다(화이트리스트에 `DRAFT→APPROVED`가 없어도 그대로 승인 성공). lock-out 우회가 없는 변형 `requireRegisteredTransition(...)`(actor 파라미터 자체 없음)을 추가해 `decide()`가 이를 쓰도록 수정했다 — 이 호출 지점은 `decide()` 자체가 이미 PROJECT_ADMIN+만 허용하므로 lock-out 시나리오가 없다(막혀도 PROJECT_ADMIN이 `config/workflow-rules`로 직접 규칙을 추가해 풀 수 있음). 로컬 docker-compose 실제 Postgres + `mvnw spring-boot:run`으로 구동한 실제 API에 PROJECT_ADMIN(시스템 ADMIN 아님) 계정으로 curl 재현: 화이트리스트에 미등록된 `DRAFT→APPROVED`는 `400 VALIDATION_ERROR`로 거부되고 요구사항/승인 요청 상태가 모두 원상태로 유지됨을 DB에서 직접 확인했고, 규칙을 추가한 뒤 재시도하면 `200`으로 성공해 요구사항이 `APPROVED`로 바뀜을 확인했다. 회귀 전이(규칙 미등록 프로젝트)도 `200`으로 정상 통과 확인. `mvnw test` 161개 통과(이전 157개 + 신규 4개). 상세는 ADR-012 §D 각주 참고.

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
