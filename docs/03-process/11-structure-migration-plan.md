> Owner: architect | Status: proposed (ADR 승인 전) | Last-reviewed: 2026-09-06
> 상위 문서: [SPEC.md](../00-meta/SPEC.md) · 용어: [GLOSSARY.md](../00-meta/GLOSSARY.md)

## 이 문서의 목적

Phase 0~15가 계층형 패키지 구조(`domain`/`repository`/`service`/`web`/`dto`)로 완성된 상태에서, **기능별 패키지 구조로 점진 전환**하기 위한 실행 계획이다. 이 문서는 완료 후 폐기한다(규약 자체는 `conventions/`에 남는다).

⚠️ **이 계획은 `09-quality-testing.md §9`와 `02-architecture.md §2.2`를 무효화한다.** 착수 전 ADR을 먼저 작성하고, `ROLES.md §4` 원칙에 따라 두 문서의 소유자(qa-tester / architect) 조정을 오케스트레이터가 수행한다.

---

## 1. 왜 바꾸는가 / 어디까지만 바꾸는가

### 1.1 현재 구조의 문제

계층형(`service/`에 모든 서비스, `domain/`에 모든 엔티티)은 **공통 폐쇄 원칙(CCP, Martin)** 을 위반한다. "요구사항 승인 기능을 고친다"는 하나의 변경이 `domain/`, `repository/`, `service/`, `dto/`, `web/` 다섯 폴더를 동시에 건드리게 만든다. Phase 12~15에서 엔티티가 9개→17개로 늘었고 v3에서 24개가 되면, 각 폴더가 수십 개 파일의 **논리적 응집(2단계)** 덩어리가 된다.

AI 코딩 세션 관점에서는 더 직접적인 문제가 있다. 기능 하나를 수정하려면 다섯 폴더의 파일을 모두 컨텍스트에 넣어야 하고, 관련 없는 파일까지 딸려 들어와 엉뚱한 곳을 수정할 확률이 올라간다.

### 1.2 하지 않을 것 — 전면 헥사고날 전환

`01-scope.md §1.1`("무거운 ALM 툴 없이")과 `02-competitive-reference.md`의 "Light 철학과의 긴장 관계" 메모를 존중한다. 포트/어댑터를 전 도메인에 도입하면 도메인 POJO ↔ JPA 엔티티 매핑 코드가 두 배로 늘어나며, 이는 이 프로젝트가 스스로 경계해온 방향이다.

**따라서 계층은 유지하고 묶는 축만 바꾼다.** 의존성 역전(DIP)은 값이 확실한 한 곳 — 외부 연동 — 에만 국소 적용한다.

### 1.3 적용 범위 요약

| 영역 | 적용 | 이유 |
|---|---|---|
| 기능별 패키지 분리 | ✅ 전체 | CCP. 변경 파급 국소화 |
| 계층 서브패키지(`api`/`domain`/`repository`/`service`) 유지 | ✅ | 기존 코드 이동 비용 최소 |
| 상세 파일 명명 규칙 | ✅ Service/Controller/DTO 중심 | 엔티티는 현행 유지 |
| 의존성 역전(포트/어댑터) | ⚠️ `integration/github`, `integration/jenkins` 만 | 외부 API 테스트 어려움 + 자체호스팅 Git 호환성 리스크(02-architecture.md §2.4 경고) |
| 도메인 POJO ↔ JPA 엔티티 분리 | ❌ 하지 않음 | Light 철학 위반, 비용 대비 효과 낮음 |

---

## 2. 목표 패키지 구조 (백엔드)

```
com.lightalm/
├── LightAlmApplication.java
│
├── shared/                                  ← 도메인 지식 없는 것만. "common/util" 이름 금지
│   ├── config/          CorsConfig, WebConfig, JacksonConfig
│   ├── security/        SecurityConfig, LightAlmUserDetailsService,
│   │                    JsonAuthenticationSuccessHandler, JsonAuthenticationFailureHandler
│   ├── error/           GlobalExceptionHandler, ApiErrorResponse,
│   │                    ResourceNotFoundException, ForbiddenException, ValidationException
│   └── model/           TargetType, TargetRef, Priority, PageResponse
│
├── user/
│   ├── api/             UserAdminController, dto/
│   ├── domain/          User, SystemRole
│   ├── repository/      UserRepository
│   └── service/         UserAccountService, UserQueryService
│
├── project/
│   ├── api/             ProjectController, ProjectMemberController,
│   │                    ProjectIntegrationSettingController, dto/
│   ├── domain/          Project, ProjectMember, ProjectStatus, ProjectRole
│   ├── repository/      ProjectRepository, ProjectMemberRepository
│   └── service/         ProjectService, ProjectMemberService,
│                        ProjectKeySequenceAllocator          ← 채번(동시성 주의)
│
├── requirement/
│   ├── api/             RequirementController,
│   │                    RequirementHierarchyController,
│   │                    RequirementDocumentViewController      (v3)
│   │                    dto/
│   ├── domain/          Requirement, RequirementType, RequirementStatus
│   ├── repository/      RequirementRepository
│   └── service/         RequirementCommandService,
│                        RequirementQueryService,
│                        RequirementHierarchyService,
│                        RequirementStatusTransitionPolicy
│
├── issue/               (requirement과 동일한 형태)
│
├── traceability/
│   ├── api/             TraceabilityLinkController, TraceabilityMatrixController,
│   │                    TraceabilityTreeController, dto/
│   ├── domain/          TraceabilityLink, LinkType
│   ├── repository/      TraceabilityLinkRepository
│   └── service/         TraceabilityLinkService,
│                        TraceabilityMatrixQueryService,
│                        TraceabilityTreeQueryService,
│                        TraceabilityTargetValidator          ← FK 없는 다형 연관 검증
│
├── comment/
├── testcase/            TestCase, TestRun, TestRunResult
├── release/             Release, ReleaseItem, ReleaseNoteGenerator
├── audit/               AuditLog, AuditAction, AuditLogRecorder(append-only), AuditLogQueryService
├── approval/            ApprovalRequest, ApprovalStatus,
│                        RequirementApprovalService, ApprovalInboxQueryService
├── dashboard/           ProjectDashboardQueryService, MyWorkDashboardQueryService
│
├── integration/
│   ├── github/
│   │   ├── GitHubApiPort.java                ← 인터페이스 (DIP 적용 지점)
│   │   ├── GitHubRestApiClient.java          ← RestClient 구현
│   │   ├── GitHubWebhookController.java
│   │   ├── GitHubWebhookSignatureVerifier.java
│   │   ├── GitHubCommitKeyParser.java        ← 정규식 (테스트 필수 대상)
│   │   ├── GitLink.java, GitLinkRepository.java
│   │   └── GitLinkService.java
│   └── jenkins/
│       ├── JenkinsApiPort.java               ← 인터페이스
│       ├── JenkinsRestApiClient.java
│       ├── JenkinsWebhookController.java
│       ├── JenkinsWebhookTokenVerifier.java
│       ├── JenkinsBuild.java, JenkinsBuildRepository.java
│       └── JenkinsBuildService.java
│
└── (v3 신규 — 처음부터 이 구조로 작성)
    ├── review/          ReviewCycle, ReviewParticipant, ...
    ├── baseline/        Baseline, BaselineItem, BaselineDiffService
    ├── risk/            Risk, RiskLikelihood, RiskImpact, RiskScoreCalculator
    ├── variant/         Variant, RequirementVariant, Applicability
    └── report/          TraceabilityMatrixExcelExporter, AuditLogPdfExporter
```

**패키지 간 규칙**
1. 기능 패키지끼리 **서비스를 직접 호출하지 않는다.** 필요하면 `shared/` 또는 인터페이스를 통한다.
   - 현실적 예외: `project/ProjectMemberService.requireRole(...)`은 모든 기능이 호출한다(`06-auth.md`). 이는 인가 관심사이므로 **`shared/security/`로 이동**을 검토한다.
2. `audit/AuditLogRecorder`도 모든 기능이 호출한다. 직접 의존 대신 **도메인 이벤트 + `@TransactionalEventListener`** 로 뒤집는 것을 권장한다(별도 ADR 필요, 6단계 참고).
3. `shared/`에 도메인 단어가 등장하면 잘못 배치된 것이다.

---

## 3. 상세 파일명 — 현재 → 목표 매핑

### 3.1 분리가 필요한 지점

> 2026-09-04 실제 코드 조사 결과, 서비스 클래스 크기는 전부 적정 범위였다.
> 이 계획의 초점은 서비스 분해가 아니라 (1) 패키지를 기능별로 재편하는 것과
> (2) 엔티티 @Setter 제거, (3) Response DTO 분리다.

| 현재 | 목표 | 근거 |
|---|---|---|
| `ProjectService`의 채번 로직(`nextRequirementKey`/`nextIssueKey`/`nextTestCaseKey`) | `ProjectKeySequenceAllocator` | 크기 문제가 아니라 **동시성 테스트 격리**. `SequenceGenerationIT`가 이미 채번 로직을 대상으로 존재한다. 채번 로직만 별도 클래스로 분리하면 이 통합 테스트의 대상 범위가 명확해지고, 나머지 `ProjectService` 로직에 대한 단위 테스트가 Testcontainers 등 동시성 검증 인프라에 얽매이지 않는다 |
| 목록 API의 `XxxResponse`(상세 조회와 동일한 풀 필드셋을 내려줌) | `XxxSummaryResponse`(목록) / `XxxDetailResponse`(단건) | 크기나 성능 문제가 아니라 **CRP(공통 재사용 원칙) 위반**. 51개 Response DTO 전수 조사 결과 목록 API가 상세 조회와 동일한 필드셋을 내려주고 있어, 목록만 필요한 클라이언트도 상세 전용 필드에 불필요하게 의존하게 된다 |

### 3.2 Controller 분해

| 현재(추정) | 목표 |
|---|---|
| `RequirementController`(CRUD+계층+승인 혼재) | `RequirementController`(CRUD)<br>`RequirementHierarchyController`(트리)<br>`approval/ApprovalController`(승인은 approval 패키지로) |
| `TraceabilityController` | `TraceabilityLinkController`<br>`TraceabilityMatrixController`<br>`TraceabilityTreeController` |

### 3.3 DTO — 용도별 분리

만능 `RequirementDto` 하나는 목록 조회에도 전 필드를 끌고 온다(CRP 위반).

| 현재(추정) | 목표 |
|---|---|
| `RequirementDto` | `RequirementCreateRequest`<br>`RequirementUpdateRequest`<br>`RequirementStatusChangeRequest`<br>`RequirementSearchCondition`<br>`RequirementSummaryResponse`(목록)<br>`RequirementDetailResponse`(단건)<br>`RequirementTreeNodeResponse`(§5.4 트리) |
| `IssueDto` | 동일 패턴 |
| `ApprovalDto` | `ApprovalRequestCreateRequest`<br>`ApprovalDecisionRequest`<br>`ApprovalRequestSummaryResponse` |

### 3.4 예외

`shared/error/`의 범용 예외(`ResourceNotFoundException` 등)는 유지하되, 도메인 규칙 위반은 기능 패키지에 구체 예외를 둔다.

| 추가 | 위치 |
|---|---|
| `RequirementNotFoundException` | `requirement/domain/` |
| `RequirementAlreadyApprovedException` | `approval/domain/` |
| `InvalidRequirementStatusTransitionException` | `requirement/domain/` |
| `DuplicateTraceabilityLinkException` | `traceability/domain/` |
| `GitHubApiException`, `JenkinsApiException` | `integration/*/` |

> `07-integrations.md §7.3`의 에러 코드(`GITHUB_API_ERROR`, `JENKINS_API_ERROR`)와 예외 클래스를 1:1 대응시킨다.

### 3.5 Flyway 마이그레이션

기존 `V1__init.sql` ~ `V8__widen_project_key.sql` 형식을 그대로 이어간다. **날짜 접두 방식으로 바꾸지 않는다**(정렬·체크섬 일관성).

```
V9__add_order_index_to_requirements.sql        (§3.23 order_index)
V10__add_risk_to_traceability_target_type.sql  (§3.22 CHECK 제약 갱신)
V11__create_review_cycle_tables.sql
```

기존 파일(V1~V8)은 이미 운영 DB에 적용된 이력이 있으므로 그대로 두고(수정 금지, ADR-002/006, `10-deployment.md` 부록 E), 앞으로 새로 추가하는 파일(V9~)부터 다음 패턴을 적용한다: `V{n}__{동사}_{대상}.sql` — 소문자 snake_case, 동사로 시작(`create`/`add`/`alter`/`seed`/`backfill`).

---

## 4. 목표 구조 (프론트엔드)

`09-quality-testing.md §9`의 현행 규약(`pages/RequirementList/`, `api/requirement.ts`)을 기능별로 재편한다.

```
frontend/src/
├── app/                 AppRouter.tsx, AuthProvider.tsx, QueryClientProvider.tsx
├── features/            ← 백엔드 패키지명과 1:1로 맞춘다
│   ├── requirement/
│   │   ├── api/         requirementApi.ts, useRequirementDetailQuery.ts,
│   │   │                useUpdateRequirementMutation.ts
│   │   ├── model/       requirementStatus.ts, requirement.types.ts
│   │   ├── components/  RequirementSummaryCard.tsx,
│   │   │                RequirementStatusWorkflowChart.tsx      (§5.5, mermaid)
│   │   │                RequirementTraceabilityTree.tsx         (§5.4)
│   │   │                RequirementStatusBadge.tsx
│   │   └── pages/       RequirementListPage.tsx, RequirementDetailPage.tsx
│   ├── issue/
│   ├── traceability/    TraceabilityMatrixTable.tsx
│   ├── testcase/        TestRunExecutionPage.tsx
│   ├── release/
│   ├── audit/           AuditLogViewerPage.tsx
│   ├── approval/        ApprovalInboxPage.tsx
│   ├── dashboard/       MyWorkDashboardPage.tsx, ProjectDashboardPage.tsx
│   └── (v3) review/, baseline/, risk/, variant/
├── shared/
│   ├── ui/              DataTable.tsx, StatusBadge.tsx, ConfirmDialog.tsx
│   ├── api/             httpClient.ts          ← 기존 api/client.ts
│   └── lib/             formatDate.ts
└── types/
```

**규칙**
- 컴포넌트 파일 `PascalCase.tsx`(컴포넌트명과 일치), 그 외 모듈 `camelCase.ts`
- `features/A`가 `features/B`를 직접 import 금지 → 공통은 `shared/`
- `shared/ui`에 도메인 단어 금지
- 배럴 `index.ts`는 기능 폴더당 최대 1개 (에디터 탭이 전부 `index`가 되는 것 방지)
- 상태 문자열 상수는 백엔드 enum과 값이 동일해야 한다(GLOSSARY §3)

---

## 5. 실행 순서

**한 PR에 한 단계만.** rename과 로직 변경을 절대 섞지 않는다(Fowler, "두 개의 모자").

| 단계 | 작업 | 위험도 | 검증 |
|---|---|---|---|
| **0** | ADR 작성 — 이 계획의 채택 근거, `09-quality-testing.md §9`·`02-architecture.md §2.2` 무효화 명시 | — | 오케스트레이터 승인 |
| **1** | `GLOSSARY.md` 확정 | 없음 | 코드 변경 0 |
| **2** | **v3 신규 기능(Phase 16~19)을 새 구조로 작성** | 낮음 | 기존 코드 미변경 |
| **3** | Service/Controller/DTO **rename만** (§3.1~3.3) | 낮음 | IDE 자동 rename, 테스트 그린 |
| **4** | 비대한 Service **분해** (로직 이동, 시그니처 유지) | 중간 | 기존 단위 테스트 통과 |
| **5** | 패키지 **이동** (`git mv`, import만 변경) | 중간 | 컴파일 + `mvnw test` |
| **6** | `integration/`에 포트 인터페이스 도입 | 중간 | 외부 API 없이 단위 테스트 가능해짐 |
| **7** | 프론트 `features/` 재편 | 중간 | `npm run build` + 화면 수동 확인 |
| **8** | ArchUnit·ESLint 규칙 CI 추가 | 낮음 | CI 실패로 강제 |
| **9** | `09-quality-testing.md §9`, `02-architecture.md §2.2` 갱신, `00-changelog.md` 기록 | — | |

> **2단계가 핵심이다** (Strangler Fig, Fowler). Phase 16~19가 아직 미구현이므로, 새 구조를 **새 코드에서 먼저 검증**한 뒤 기존 코드를 옮긴다. 새 구조가 불편하다는 게 드러나면 기존 코드를 건드리기 전에 되돌릴 수 있다.

### 5.1 배포 관련 주의

- 각 단계 완료 시 `origin`(GitHub) **및 `synology`(Gitea)** 에 push해야 운영 반영된다(`CURRENT-STATE.md §4`). `synology` push는 즉시 운영 배포를 트리거하므로(`ADR-005` 리스크), 구조 변경 PR은 **로컬 `docker compose up --build` 검증 후** push한다.
- 패키지 이동은 Flyway·DB와 무관하므로 스키마 리스크는 없다. 단 `@Entity` 클래스 이동 시 `ddl-auto: validate`가 테이블명을 계속 찾도록 `@Table(name=...)`이 명시돼 있는지 확인한다.

---

## 6. 별도 ADR이 필요한 결정

이 계획에 포함하지 않고 따로 논의해야 하는 항목이다.

| 항목 | 쟁점 |
|---|---|
| `TargetRef` 값 객체 추출 | 9개 테이블의 `(target_type, target_id)` 쌍. 원시 타입 집착 해소 vs 변경 범위 |
| 감사 로그를 도메인 이벤트로 전환 | 모든 기능 → `audit` 직접 의존을 이벤트로 역전. 트랜잭션 경계 주의 |
| `ProjectMemberService.requireRole`을 `shared/security/`로 이동 | 인가는 횡단 관심사 vs `project` 도메인 지식 |
| GitHub PAT / Jenkins 토큰 암호화 | `CURRENT-STATE.md §6` 미해결 리스크. Jasypt 도입 시 `shared/security/` 배치 |
| Flyway `validate-on-migrate=false` 해제 | `CURRENT-STATE.md §6`. 체크섬 정합성 회복 계획 필요 |

---

## 7. 완료 조건 (DoD)

- [ ] `mvnw test`, `mvnw verify`(`*IT.java` 포함) 통과
- [ ] `npm run build` 통과 + 주요 화면 수동 확인
- [ ] `shared/`에 도메인 단어가 없다
- [ ] `util`/`common`/`helper`/`manager`/`Impl` 이름의 파일이 0개다
- [ ] 기능 패키지 간 순환 의존 0 (ArchUnit)
- [ ] 파일명만 보고 역할을 알 수 있다 — "어떤 ~?" 질문이 남는 파일명 0개
- [ ] `09-quality-testing.md §9`, `02-architecture.md §2.2` 갱신 완료
- [ ] `00-changelog.md`, `CURRENT-STATE.md` 갱신 완료
- [ ] 이 문서를 `Status: superseded`로 변경하거나 삭제
