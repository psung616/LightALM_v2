> Owner: orchestrator (용어 추가·변경 제안은 requirements-analyst / architect) | Status: current | Last-reviewed: 2026-09-06
> 상위 문서: [SPEC.md](SPEC.md)

## 이 문서의 목적

Light ALM의 **유비쿼터스 언어(Ubiquitous Language)** 사전이다. 같은 개념을 코드·DB·API·화면·문서에서 각각 다른 단어로 부르면 검색이 끊기고 AI 코딩 세션마다 새 동의어가 생긴다.

근거: Evans, *Domain-Driven Design*(2003) / Deissenboeck & Pizka, *Concise and Consistent Naming*(2006)의 두 규칙 — **하나의 이름은 하나의 개념만 가리키고(일관성), 하나의 개념은 하나의 이름만 갖는다(간결성).**

**사용 규칙**
1. 아래 "금지 동의어"에 있는 단어는 코드·DTO·화면명·커밋 메시지에 등장시키지 않는다.
2. 새 개념이 생기면 코드를 쓰기 전에 이 표에 먼저 추가한다.
3. AI 코딩 세션에 이 문서를 컨텍스트로 함께 넣는다.

---

## 1. 핵심 도메인 개념

| 도메인 개념 | 채택 용어 (Java) | DB 테이블 | 금지 동의어 | 비고 |
|---|---|---|---|---|
| 시스템 사용자 | `User` | `users` | member, account, person | 프로젝트 참여자는 `ProjectMember`로 구분 |
| 프로젝트 | `Project` | `projects` | workspace, space, team | |
| 프로젝트 참여자 | `ProjectMember` | `project_members` | participant, collaborator | `User`와 혼용 금지 |
| 요구사항 | `Requirement` | `requirements` | spec, feature, story, need | `Issue`의 `STORY` 타입과 다른 개념 |
| 이슈 | `Issue` | `issues` | ticket, task, card, work item | `TASK`는 `Issue.type` 값 |
| 추적성 링크 | `TraceabilityLink` | `traceability_links` | relation, connection, mapping, link(단독) | |
| 댓글 | `Comment` | `comments` | reply, note, remark | |
| Git 연결 | `GitLink` | `git_links` | commitLink, prLink, vcsLink | 커밋·PR을 모두 포괄 |
| Jenkins 빌드 | `JenkinsBuild` | `jenkins_builds` | build(단독), ciResult, pipeline | |
| 테스트케이스 | `TestCase` | `test_cases` | testSpec, scenario, tc(약어) | |
| 테스트 실행 | `TestRun` | `test_runs` | execution, testCycle, session | |
| 테스트 실행 결과 | `TestRunResult` | `test_run_results` | result(단독), verdict | |
| 릴리스 | `Release` | `releases` | version, build, deployment | `JenkinsBuild`와 혼용 금지 |
| 릴리스 항목 | `ReleaseItem` | `release_items` | scopeItem, content | |
| 감사 로그 | `AuditLog` | `audit_logs` | history, changeLog, activity, trail | **append-only** |
| 승인 요청 | `ApprovalRequest` | `approval_requests` | approval(단독), request(단독), gate | `DRAFT→APPROVED` 1건 전용 |
| 리뷰 사이클 (v3) | `ReviewCycle` | `review_cycles` | review(단독), inspection | `ApprovalRequest`와 **별개 기능** |
| 리뷰 참여자 (v3) | `ReviewParticipant` | `review_participants` | reviewer(단독), approver | |
| 베이스라인 (v3) | `Baseline` | `baselines` | snapshot, tag, freeze | |
| 베이스라인 항목 (v3) | `BaselineItem` | `baseline_items` | snapshotItem | |
| 위험 (v3) | `Risk` | `risks` | hazard, threat, concern | |
| 변형 (v3) | `Variant` | `variants` | product line, edition, flavor | |
| 요구사항-변형 적용 (v3) | `RequirementVariant` | `requirement_variants` | variantMapping | |
| 대시보드 위젯 설정 (v3) | `DashboardWidgetConfig` | `dashboard_widget_configs` | widget(단독), layout, preference | |

## 2. 다형 연관 공통 개념

`(target_type, target_id)` 쌍을 쓰는 테이블은 **동일 개념이 아니라 세 그룹으로 나뉜다**(2026-09-06 실제 코드 대조로 확인, 이전 "9개 테이블이 동일 개념을 공유한다"는 서술은 부정확했다):

- `traceability_links`, `comments` — `REQUIREMENT` / `ISSUE` / `TEST_CASE`
- `release_items`, `approval_requests`, `git_links`, `jenkins_builds` — `REQUIREMENT` / `ISSUE`만
- `audit_logs` — 별개 enum `AuditTargetType`(§3 참고), 값 집합 자체가 다름

| 개념 | 채택 용어 | 금지 |
|---|---|---|
| 대상 종류 | `TargetType` (enum) | entityType, refType, objectType |
| 대상 참조 | `TargetRef` (값 객체: type + id) | ref, target(단독), pointer |
| 링크 출발지 | `sourceType` / `sourceId` | fromType, originType |

> **검증 로직 중복은 ADR-010으로 해결 완료.** `PolymorphicTargetValidator`(`com.lightalm.service.support`, `ensureExists(Long projectId, TargetType targetType, Long targetId)`)가 위 첫 두 그룹(대상 존재 여부·프로젝트 소속 검증)을 통합한다. **`audit_logs`는 대상이 아니며 포함할 수 없다** — `AuditTargetType`은 값 집합이 다른 별개 enum이기 때문이다.
>
> `TargetRef` 값 객체 추출(원시 타입 집착 해소)은 여전히 향후 과제로 남아 있다. 대상은 `TargetType`을 쓰는 위 두 그룹뿐이며, `audit_logs`(`AuditTargetType`)는 이 리팩터링의 대상이 아니다. 착수 전 ADR을 먼저 쓴다.

## 3. Enum 값 (DB CHECK 제약과 1:1)

프론트엔드 상수도 반드시 동일 문자열을 쓴다. 한글 라벨은 화면 계층에서만 매핑한다.

| Enum | 값 |
|---|---|
| `SystemRole` (`users.system_role`) | `ADMIN`, `USER` |
| `ProjectRole` (`project_members.role`) | `PROJECT_ADMIN`, `MEMBER`, `VIEWER` |
| `ProjectStatus` | `ACTIVE`, `ARCHIVED` |
| `RequirementType` | `FUNCTIONAL`, `NON_FUNCTIONAL`, `BUSINESS` |
| `RequirementStatus` | `DRAFT`, `APPROVED`, `IN_PROGRESS`, `IMPLEMENTED`, `VERIFIED`, `REJECTED` |
| `IssueType` | `BUG`, `TASK`, `STORY`, `IMPROVEMENT` |
| `IssueStatus` | `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `CLOSED` |
| `Priority` (요구사항·이슈·테스트케이스 공용) | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `TestCaseStatus` | `DRAFT`, `READY`, `DEPRECATED` |
| `LinkType` | `IMPLEMENTS`, `TESTS`, `DEPENDS_ON`, `RELATES_TO`, `DUPLICATES` |
| `TargetType` (`com.lightalm.domain.TargetType`) | `REQUIREMENT`, `ISSUE`, `TEST_CASE` |
| `AuditTargetType` (`audit_logs` 전용) | `REQUIREMENT`, `ISSUE`, `TEST_CASE`, `RELEASE`, `PROJECT`, `USER`, `TRACEABILITY_LINK` |
| `AuditAction` | `CREATE`, `UPDATE`, `STATUS_CHANGE`, `DELETE`, `APPROVE`, `REJECT` |
| `ApprovalStatus` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `RiskLikelihood` / `RiskImpact` (v3) | `LOW`, `MEDIUM`, `HIGH` |
| `RiskStatus` (v3) | `OPEN`, `MITIGATED`, `ACCEPTED`, `CLOSED` |
| `Applicability` (v3) | `INCLUDED`, `EXCLUDED`, `MODIFIED` |

> `Priority`는 세 엔티티가 공유하므로 **하나의 enum을 공용 패키지에 둔다.** `RequirementPriority`/`IssuePriority`로 나누지 않는다(같은 개념 = 같은 이름).
> `IN_PROGRESS`는 `RequirementStatus`와 `IssueStatus`에 모두 존재하지만 **서로 다른 enum**이다. 문자열로 비교하지 말고 타입으로 구분한다.
> **`TargetType`과 `AuditTargetType`은 별개다. 합치지 않는다.** 테이블별 허용 범위가 다르다:
> - `traceability_links`, `comments` — `REQUIREMENT` / `ISSUE` / `TEST_CASE`
> - `release_items`, `approval_requests`, `git_links`, `jenkins_builds` — `REQUIREMENT` / `ISSUE`만
> - `audit_logs` — `AuditTargetType` (별개 enum)
> `PolymorphicTargetValidator`는 `audit_logs`를 포함하지 않으며, 포함할 수 없다.

## 4. 엔티티 키 접두 규칙

| 대상 | 형식 | 예시 | 채번 카운터 |
|---|---|---|---|
| 프로젝트 | `^[A-Z][A-Z0-9_]{2,19}$` | `LALM`, `TEAM_A_V2` | — (ADR-009) |
| 요구사항 | `{projectKey}-R{n}` | `LALM-R7` | `projects.requirement_seq` |
| 이슈 | `{projectKey}-{n}` | `LALM-101` | `projects.issue_seq` |
| 테스트케이스 | `{projectKey}-TC{n}` | `LALM-TC12` | `projects.test_case_seq` |
| 위험 (v3) | `{projectKey}-RISK{n}` | `LALM-RISK3` | 신설 필요 |

> ⚠️ **알려진 비일관성**: 이슈만 문자 접두가 없다(`LALM-101` vs `LALM-R7`/`LALM-TC12`). Deissenboeck의 일관성 규칙 위반이지만, 이미 운영 DB에 데이터가 있고 `09-quality-testing.md`의 커밋 메시지 규칙(`LALM-101 feat: ...`)과 GitHub 커밋 키 파싱 정규식이 이 형식을 전제하므로 **변경하지 않는다.** 새로 추가하는 엔티티는 반드시 문자 접두를 붙인다(`-RISK{n}`, `-BL{n}` 등).

## 5. 동사 어휘 (메서드·API·커밋 공용)

| 의미 | 채택 동사 | 금지 |
|---|---|---|
| 단건 조회, 없으면 `Optional` | `find` | fetch, retrieve, load, select |
| 단건 조회, 없으면 예외 | `get` | require, mustGet |
| 다건 조회 | `findAll`, `search` | list, query, getAll |
| 생성 | `create` | add, insert, register, make |
| 수정 | `update` | modify, edit, change |
| 상태 변경 | 도메인 동사 (`approve`, `reject`, `close`) | setStatus, changeStatus |
| 삭제 | `delete` | remove, destroy |
| 연결 생성 | `link` | attach, connect, bind, associate |
| 연결 해제 | `unlink` | detach, disconnect |
| 키 채번 | `allocate` | generate, issue, next |
| 감사 기록 | `record` | log, write, save, track |
| 외부 API 호출 | `request`, `trigger` | call, do, execute |
| 검증, 실패 시 예외 | `verify`, `require` | check, validate(불리언과 혼동) |
| 검증, 불리언 반환 | `is`, `can`, `has` | check |
| 변환 | `to` / `from` | convert, as, parse |

> `06-auth.md`가 이미 `ProjectMemberService.requireRole(...)`을 쓰고 있으므로 권한 검사 동사는 `require`로 고정한다.

## 6. 화면·UI 용어 (한글 ↔ 영문)

| 화면 개념 | 한글 라벨 | 영문 식별자 | 관련 문서 |
|---|---|---|---|
| 개인화 대시보드 | 내 작업 | `MyWork` | 05-frontend.md §5.2 |
| 상위/하위 추적성 트리 뷰 | 추적성 트리 | `TraceabilityTree` | §5.4 |
| 상태 Workflow 차트 | 상태 흐름도 | `StatusWorkflowChart` | §5.5 |
| 추적성 매트릭스 | 추적성 매트릭스 | `TraceabilityMatrix` | §4.7 |
| 승인함 | 승인함 | `ApprovalInbox` | §5.10 |
| 변경 이력 뷰어 | 변경 이력 | `AuditLogViewer` | §5.9 |
| 위험 관리 보드 (v3) | 위험 관리 | `RiskBoard` | §5.13 |
| 요구사항 문서 뷰 (v3) | 문서 보기 | `RequirementDocumentView` | §5.14 |

> `02-competitive-reference.md` 원칙 4에 따라, 특정 상용 제품의 브랜드화된 기능명은 이 표에 넣지 않는다.

## 갱신 이력
- 2026-09-04: 최초 작성. 03-data-model.md §3.1~3.24, 01-scope.md §1.4, 04-api.md, 05-frontend.md에서 용어 추출.
- 2026-09-06: 실제 코드 대조 결과 반영. §3 `TargetType` 행이 `TargetType`/`AuditTargetType` 두 개 별개 enum을 합쳐놨던 오류를 분리. §2 "9개 테이블이 동일 개념" 서술을 3그룹 분류로 정정하고 ADR-010(`PolymorphicTargetValidator`) 해결 완료 사실 반영.
