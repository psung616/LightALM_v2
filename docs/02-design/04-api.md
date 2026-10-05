> Owner: architect | Status: current | Last-reviewed: 2026-10-05
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 4. REST API 명세

### 4.1 공통 규칙
- Base path: `/api`
- 요청/응답 포맷: JSON (`Content-Type: application/json`)
- 인증: Spring Security 세션 쿠키(`JSESSIONID`). 프론트엔드는 axios `withCredentials: true` 필수
- CSRF: Spring Security 기본 CSRF 보호 활성화, 쿠키 기반 토큰(`XSRF-TOKEN`) 발급 → 프론트는 `X-XSRF-TOKEN` 헤더로 전송 (단, `/api/webhooks/**` 경로는 CSRF 예외 처리 — 외부 시스템 호출이므로 서명 검증으로 대체)
- 에러 응답 포맷(공통):
```json
{
  "timestamp": "2026-07-26T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "title은 필수입니다.",
  "path": "/api/projects/1/requirements"
}
```
  - **[2026-10-05 구현 각주]** 요청 본문을 DTO로 읽지 못하는 경우(본문 누락, JSON 문법 오류, enum에 없는 값 — 예: `targetType:"FOO"`·소문자 `"requirement"`·`decision:"FOO"`)와 경로 변수/쿼리 파라미터 타입 변환 실패(예: `/baselines/abc`), 필수 쿼리 파라미터 누락도 `400 VALIDATION_ERROR`로 응답한다(`GlobalExceptionHandler`의 `HttpMessageNotReadableException`·`MethodArgumentTypeMismatchException`·`MissingServletRequestParameterException` 핸들러). enum 값 오류는 `message`에 필드 경로와 허용값을 담는다(예: `"itemRefs[0].targetType: 허용되지 않는 값입니다(FOO). 허용값: REQUIREMENT, ISSUE, TEST_CASE"`). 이전에는 전역 catch-all로 떨어져 500이었다(qa-tester Phase 16 반려).
- 페이지네이션(목록 API 공통 쿼리 파라미터): `page`(0-base, default 0), `size`(default 20), `sort`(예: `createdAt,desc`)
- 목록 응답 공통 포맷:
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```
- 권한 표기: `[인증필요]` `[ADMIN]`(시스템 관리자) `[PROJECT_ADMIN+]`(해당 프로젝트 관리자 이상) `[MEMBER+]`(해당 프로젝트 멤버 이상, 즉 VIEWER 제외) `[VIEWER+]`(해당 프로젝트 멤버라면 누구나, VIEWER 포함) `[공개]`(인증 불필요, Webhook 전용)

### 4.2 인증 (Auth)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/auth/login` | `{username, password}` → 로그인, 세션 발급 | 공개 |
| POST | `/api/auth/logout` | 세션 종료 | 인증필요 |
| GET | `/api/auth/me` | 현재 로그인 사용자 정보 조회 | 인증필요 |

### 4.3 사용자 관리 (시스템 관리자 전용)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/users` | 사용자 목록 (페이지네이션) | ADMIN |
| POST | `/api/users` | 사용자 생성 `{username,password,email,fullName,systemRole}` | ADMIN |
| GET | `/api/users/{id}` | 사용자 상세 | ADMIN |
| PUT | `/api/users/{id}` | 사용자 정보 수정 | ADMIN |
| DELETE | `/api/users/{id}` | 사용자 비활성화(soft: enabled=false) | ADMIN |
| PUT | `/api/users/me/password` | 본인 비밀번호 변경 `{oldPassword,newPassword}` | 인증필요 |

### 4.4 프로젝트 (Project)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects` | 내가 속한 프로젝트 목록 (ADMIN은 전체) — 페이지네이션(`page`,`size`)만 지원, `keyword` 검색은 **미구현**(추후 추가 시 백엔드 확장 필요) | 인증필요 |
| POST | `/api/projects` | 프로젝트 생성 `{projectKey,name,description}` — 생성자는 자동으로 PROJECT_ADMIN 등록 | 인증필요 |
| GET | `/api/projects/{projectId}` | 프로젝트 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}` | 프로젝트 정보 수정 | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}` | 프로젝트 삭제 | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/integrations/github` | GitHub 연동 설정 `{repoOwner,repoName,accessToken,webhookSecret}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/integrations/jenkins` | Jenkins 연동 설정 `{baseUrl,jobName,apiUser,apiToken}` | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/members` | 멤버 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/members` | 멤버 추가 `{userId,role}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/members/{userId}` | 멤버 역할 변경 `{role}` | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/members/{userId}` | 멤버 제거 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/dashboard/summary` | 대시보드 요약(요구사항/이슈 상태별 카운트, 최근 활동) | VIEWER+ |

### 4.5 요구사항 (Requirement)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/requirements` | 목록, 쿼리: `status,type,priority,parentId,assignedTo,keyword,requirementLevel,rootOnly`(`requirementLevel`=`PRD`\|`SRS`, `rootOnly`=boolean — `parent_requirement_id IS NULL`인 것만. 둘 다 신규·선택값, ADR-013, 05-frontend.md §5.22 사이드바 트리에서 사용) | VIEWER+ |
| POST | `/api/projects/{projectId}/requirements` | 생성 `{title,description,type,priority,parentRequirementId,assignedTo,dueDate,requirementLevel}`(`dueDate`,`requirementLevel`은 선택값 — `requirementLevel` 생략 시 서버가 `'SRS'`로 저장, ADR-013) | MEMBER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/requirements/{reqId}` | 수정(`requirementLevel` 포함, ADR-013) | MEMBER+ |
| PATCH | `/api/projects/{projectId}/requirements/{reqId}/status` | 상태 변경 `{status}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/requirements/{reqId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/children` | 하위 요구사항 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/links` | 이 요구사항과 연결된 이슈/요구사항 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/traceability-tree` | **(신규)** 이 요구사항 기준 상위(조상) 체인 전체 + 하위(자손, 재귀) 요구사항 트리 + 트리의 각 노드에 연결된 이슈까지 한 번에 반환(05-frontend.md §5.4, 응답 예시는 §4.7 하단) | VIEWER+ |

> **(ADR-013, 2026-10-03)** 사이드바 "작업 항목" 트리 패널(`WorkItemTreePanel`, 05-frontend.md §5.22)은 PRD/SRS 섹션에서 위 `requirementLevel`/`rootOnly` 쿼리와 `.../requirements/{reqId}/children`(변경 없음)을 그대로 사용하고, Defect 섹션은 §4.6의 `GET .../issues?type=BUG`를, TestCase 섹션은 §4.12의 `GET .../test-cases`를 **변경 없이** 그대로 재사용한다 — 신규 엔드포인트를 추가하지 않는다.

### 4.6 이슈 (Issue)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/issues` | 목록, 쿼리: `status,type,priority,assigneeId,keyword` | VIEWER+ |
| POST | `/api/projects/{projectId}/issues` | 생성 `{title,description,type,priority,assigneeId,dueDate}`(`dueDate`는 신규, 선택값) | MEMBER+ |
| GET | `/api/projects/{projectId}/issues/{issueId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/issues/{issueId}` | 수정 | MEMBER+ |
| PATCH | `/api/projects/{projectId}/issues/{issueId}/status` | 상태 변경 `{status}` (DONE 전이 시 resolved_at 자동 세팅) | MEMBER+ |
| DELETE | `/api/projects/{projectId}/issues/{issueId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/issues/{issueId}/git-links` | 연결된 커밋/PR 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/issues/{issueId}/builds` | 연결된 Jenkins 빌드 목록 | VIEWER+ |

### 4.7 추적성 (Traceability)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/traceability/matrix` | 요구사항 x 이슈 매트릭스 데이터 반환 (아래 응답 예시) | VIEWER+ |
| POST | `/api/projects/{projectId}/traceability/links` | 링크 생성 `{sourceType,sourceId,targetType,targetId,linkType}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/traceability/links/{linkId}` | 링크 삭제 | MEMBER+ |

매트릭스 응답 예시:
```json
{
  "requirements": [{"id": 1, "reqKey": "LALM-R1", "title": "로그인 기능"}],
  "issues": [{"id": 10, "issueKey": "LALM-101", "title": "로그인 API 구현"}],
  "links": [{"id": 5, "requirementId": 1, "issueId": 10, "linkType": "IMPLEMENTS"}]
}
```

**상위/하위 추적성 트리 응답 예시** (`GET /api/projects/{projectId}/requirements/{reqId}/traceability-tree`, 05-frontend.md §5.4):
```json
{
  "ancestors": [
    {"id": 1, "reqKey": "LALM-R1", "title": "로그인 기능"},
    {"id": 3, "reqKey": "LALM-R3", "title": "사용자 인증 상위 요구사항"}
  ],
  "self": {"id": 5, "reqKey": "LALM-R5", "title": "폼 로그인", "status": "IMPLEMENTED"},
  "descendants": [
    {
      "id": 8, "reqKey": "LALM-R8", "title": "로그인 실패 처리", "status": "APPROVED",
      "linkedIssues": [{"id": 10, "issueKey": "LALM-101", "title": "로그인 API 구현", "linkType": "IMPLEMENTS", "status": "IN_PROGRESS"}],
      "children": []
    }
  ]
}
```
`ancestors`는 루트까지의 조상 체인(가까운 순), `descendants`는 하위 요구사항을 재귀적으로 담되 각 노드에 직접 연결된 이슈(`linkedIssues`)도 함께 포함한다. 구현은 PostgreSQL 재귀 CTE(`WITH RECURSIVE`)로 조상/자손을 각각 조회한 뒤, 자손 트리의 각 노드마다 `traceability_links`를 조인해 `linkedIssues`를 채운다.

### 4.8 댓글 (Comment)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/comments` | 댓글 목록 (`targetType` = `requirements`\|`issues`) | VIEWER+ |
| POST | `/api/projects/{projectId}/{targetType}/{targetId}/comments` | 댓글 작성 `{content}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/comments/{commentId}` | 댓글 삭제(작성자 본인 또는 PROJECT_ADMIN+) | MEMBER+ |

### 4.9 GitHub 연동
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/{targetType}/{targetId}/git-links` | 수동 연결: 커밋 SHA 또는 PR 번호 입력 → GitHub API로 메타데이터 조회 후 저장 `{source, commitSha 또는 prNumber}` | MEMBER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/git-links` | 연결 목록 | VIEWER+ |
| DELETE | `/api/projects/{projectId}/git-links/{linkId}` | 연결 삭제 | MEMBER+ |
| POST | `/api/webhooks/github/{projectId}` | GitHub Webhook 수신 엔드포인트 (`push`, `pull_request` 이벤트) — 커밋 메시지에서 `LALM-101` 형태의 이슈/요구사항 키 패턴을 정규식으로 파싱하여 자동으로 `git_links` 생성 | 공개(서명 검증) |

**GitHub 자동 연동 규칙**: 커밋 메시지 또는 PR 제목/본문에 `{PROJECT_KEY}-\d+`(이슈) 또는 `{PROJECT_KEY}-R\d+`(요구사항) 패턴이 포함되면 자동으로 해당 대상에 `git_links` 레코드를 생성한다.

**Webhook 서명 검증**: GitHub Webhook은 `X-Hub-Signature-256` 헤더에 `sha256=` prefix + HMAC-SHA256(payload, project.github_webhook_secret) 값을 담아 전송한다. 서버는 이 값을 재계산하여 일치하지 않으면 `401 Unauthorized`를 반환한다.

### 4.10 Jenkins 연동
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/jenkins/trigger` | Jenkins 빌드 트리거 `{targetType,targetId}` — Jenkins API 호출(`POST {jenkinsBaseUrl}/job/{jobName}/build`, Basic Auth: apiUser/apiToken) | MEMBER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/builds` | 연결된 빌드 목록 | VIEWER+ |
| POST | `/api/webhooks/jenkins/{projectId}` | Jenkins 빌드 완료 Webhook 수신 (Jenkins Post-build Action에서 HTTP POST) — payload에 포함된 커스텀 파라미터(`targetType`,`targetId`,`jobName`,`buildNumber`,`status`,`buildUrl`)로 `jenkins_builds` 레코드 생성/갱신 | 공개(공유 시크릿 검증) |

**Jenkins Webhook 인증**: URL 쿼리 파라미터 또는 헤더로 `?token={project.github_webhook_secret 재사용 또는 별도 jenkins_webhook_secret}`을 전달받아 검증한다. (단순화를 위해 Jenkins 쪽은 헤더 `X-Jenkins-Token` 사용을 표준으로 한다.)

**Jenkins 연동 방식(권장 구성)**: Jenkins Job의 Post-build Action에 "Post build task" 또는 "HTTP Request Plugin"을 추가하여 빌드 종료 시 `POST /api/webhooks/jenkins/{projectId}`로 빌드 결과 JSON을 전송하도록 구성한다(08-dev-phases.md Phase 9에서 예시 payload 제공).

### 4.11 개인화된 대시보드 (My Dashboard) — 신규

기존 `/api/projects/{projectId}/dashboard/summary`(§4.4)는 프로젝트 하나 기준이라, 여러 프로젝트에 걸쳐 있는 사용자가 "지금 나한테 급한 게 뭔지"를 보려면 프로젝트를 하나씩 들어가봐야 한다. 이를 보완하는 프로젝트 횡단(cross-project) 개인화 대시보드 API를 추가한다.

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/me/dashboard` | 내가 속한 모든 프로젝트를 통틀어 나에게 할당된 요구사항/이슈 현황을 집계해 반환(아래 응답 예시) | 인증필요 |

응답 예시:
```json
{
  "assignedIssuesByStatus": {"TODO": 3, "IN_PROGRESS": 2, "IN_REVIEW": 1, "DONE": 0, "CLOSED": 0},
  "assignedRequirementsByStatus": {"DRAFT": 1, "APPROVED": 2, "IN_PROGRESS": 1, "IMPLEMENTED": 0, "VERIFIED": 0, "REJECTED": 0},
  "overdue": [
    {"type": "ISSUE", "id": 10, "key": "LALM-101", "title": "로그인 API 구현", "projectKey": "LALM", "dueDate": "2026-07-20", "status": "IN_PROGRESS"}
  ],
  "dueSoon": [
    {"type": "REQUIREMENT", "id": 5, "key": "LALM-R5", "title": "폼 로그인", "projectKey": "LALM", "dueDate": "2026-08-05", "status": "APPROVED"}
  ],
  "byProject": [
    {"projectId": 1, "projectKey": "LALM", "projectName": "Light ALM", "assignedIssueCount": 6, "assignedRequirementCount": 4}
  ]
}
```
- `overdue`: `due_date < 오늘` 이면서 상태가 종료 상태(이슈의 `DONE`/`CLOSED`, 요구사항의 `IMPLEMENTED`/`VERIFIED`/`REJECTED`)가 아닌 항목.
- `dueSoon`: `due_date`가 오늘부터 7일 이내(마찬가지로 종료 상태 제외).
- `byProject`: 내가 속한 프로젝트별로 나에게 할당된 항목 개수만 요약(프로젝트 목록·바로가기용).
- 구현은 서비스 레이어에서 `project_members`로 내가 속한 프로젝트 id 목록을 먼저 구한 뒤, 그 프로젝트들 범위에서 `assignee_id`/`assigned_to`가 나인 `issues`/`requirements`를 조회해 집계한다(단일 쿼리든 여러 쿼리 조합이든 방식은 자유, DoD는 08-dev-phases.md Phase 6 참고).

---

### 4.12 테스트케이스 (Test Case)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/test-cases` | 목록, 쿼리: `requirementId,status,priority,keyword` | VIEWER+ |
| POST | `/api/projects/{projectId}/test-cases` | 생성 `{title,description,preconditions,steps,expectedResult,priority,requirementId}` | MEMBER+ |
| GET | `/api/projects/{projectId}/test-cases/{tcId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/test-cases/{tcId}` | 수정 | MEMBER+ |
| DELETE | `/api/projects/{projectId}/test-cases/{tcId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/test-cases` | 요구사항에 연결된 테스트케이스 목록 | VIEWER+ |

### 4.13 테스트 실행 (Test Run)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/test-runs` | 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/test-runs` | 생성 `{name, releaseId?}` | MEMBER+ |
| GET | `/api/projects/{projectId}/test-runs/{runId}` | 상세(포함된 테스트케이스 + 결과 목록) | VIEWER+ |
| POST | `/api/projects/{projectId}/test-runs/{runId}/cases` | 테스트런에 테스트케이스 추가 `{testCaseIds:[]}` | MEMBER+ |
| PATCH | `/api/projects/{projectId}/test-runs/{runId}/results/{testCaseId}` | 실행 결과 기록 `{result,actualResult}` | MEMBER+ |
| PATCH | `/api/projects/{projectId}/test-runs/{runId}/status` | 실행 상태 변경 (PLANNED→IN_PROGRESS→COMPLETED) | MEMBER+ |

### 4.14 릴리스 (Release)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/releases` | 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/releases` | 생성 `{version,name,releaseDate,description}` | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/releases/{releaseId}` | 상세(포함 항목 + 요약 통계) | VIEWER+ |
| PUT | `/api/projects/{projectId}/releases/{releaseId}` | 수정 | PROJECT_ADMIN+ |
| PATCH | `/api/projects/{projectId}/releases/{releaseId}/status` | 상태 변경 | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/releases/{releaseId}/items` | 항목 추가 `{targetType,targetId}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/releases/{releaseId}/items/{itemId}` | 항목 제거 | MEMBER+ |
| GET | `/api/projects/{projectId}/releases/{releaseId}/notes` | 릴리스 노트 자동 생성(포함된 요구사항/이슈를 타입별로 정리한 마크다운 텍스트 반환) | VIEWER+ |

### 4.15 변경 이력 / 감사 로그 (Audit Log)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/audit-logs` | 프로젝트 범위 전체 조회, 쿼리: `targetType,targetId,actorId,fromDate,toDate` | PROJECT_ADMIN+ (민감정보 성격이라 조회 권한을 상향) |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/audit-logs` | 특정 대상 하나의 변경 이력만 (상세 화면 "이력" 탭용) | VIEWER+ |

생성/수정/삭제 API는 없다 — append-only이며, 서비스 레이어에서 자동 기록된다.

### 4.16 승인 워크플로우 (Approval)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/requirements/{reqId}/approval-requests` | 승인 요청 생성 `{requestedStatus}` (요구사항이 DRAFT 상태일 때만 가능, 서비스 레벨 검증) | MEMBER+ |
| GET | `/api/projects/{projectId}/approval-requests` | 승인함 목록, 쿼리: `status` | PROJECT_ADMIN+ |
| PATCH | `/api/projects/{projectId}/approval-requests/{approvalId}/decision` | 승인/반려 `{decision: 'APPROVE'|'REJECT', comment}` (승인 시 대상 요구사항 status를 requestedStatus로 전이 + audit_logs에 APPROVE/REJECT 기록을 자동 생성) | PROJECT_ADMIN+ |

---

## v3 확장 API (2026-08-08, 03-data-model.md §3.18~3.24 참고, 아직 미구현 — ADR-008)

### 4.17 리뷰 사이클 (Review Cycle)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/{targetType}/{targetId}/review-cycles` | 리뷰 사이클 생성 `{name, participantUserIds:[]}` | MEMBER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/review-cycles` | 대상의 리뷰 사이클 목록(참여자 결정 현황 포함) | VIEWER+ |
| PATCH | `/api/projects/{projectId}/review-cycles/{cycleId}/participants/me` | 본인 결정 기록 `{decision, comment}` | 해당 사이클의 participant 본인만 |
| PATCH | `/api/projects/{projectId}/review-cycles/{cycleId}/status` | 사이클 닫기(`CLOSED`) — 대상 status는 변경하지 않음(§3.19 원칙) | MEMBER+ |

### 4.18 베이스라인 (Baseline)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/baselines` | 베이스라인 생성 `{name, description, itemRefs:[{targetType,targetId}]}` — 생성 시점 각 항목의 주요 필드를 스냅샷으로 저장 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/baselines` | 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/baselines/{baselineId}` | 상세(포함 항목 스냅샷 목록) | VIEWER+ |
| GET | `/api/projects/{projectId}/baselines/{baselineId}/diff` | 스냅샷 vs 현재 값 필드 단위 비교 결과 반환 | VIEWER+ |

> **[2026-10-05 구현 각주 — Phase 16, §4.17]** 컨트롤러 `com.lightalm.review.api.ReviewCycleController`, 서비스 `ReviewCycleCommandService`(생성/결정/닫기) · `ReviewCycleQueryService`(목록) — docs/CLAUDE.md 신규 코드 규칙(Command/Query 분리, 요청 DTO는 record)을 따른다. 위 표에 없던 동작을 구현 중 다음과 같이 확정했다.
> - `{targetType}` 경로 세그먼트는 `requirements` 또는 `issues`만 허용(그 외 `test-cases` 등은 400 `VALIDATION_ERROR`). 대상 존재/프로젝트 소속은 `PolymorphicTargetValidator`로 검증(404).
> - 응답 `ReviewCycleDetailResponse`(단건 조회 API가 없고 목록도 참여자 현황을 포함해야 하므로 목록 원소·생성·결정·닫기 응답이 모두 이 Detail 형태 — 별도 Summary DTO 없음): `{id, targetType, targetId, name, status, createdById, createdByName, createdAt, closedAt, participants:[{id, userId, username, fullName, decision, comment, decidedAt}]}`. POST·GET 목록(배열, `createdAt` 내림차순)·두 PATCH 모두 이 모양으로 응답한다.
> - 생성: `name` 필수(최대 150), `participantUserIds` 최소 1명(중복 ID는 1명으로 합침). 존재하지 않는 사용자 ID → 400. **프로젝트 멤버가 아닌 사용자(시스템 ADMIN 제외)를 참여자로 지정하면 400** — 비멤버는 결정 기록 API의 프로젝트 접근 검사(VIEWER+)를 통과하지 못해 영원히 PENDING으로 남기 때문에 생성 시점에 막는다(설계 표에 없던 입력 검증).
> - 결정 기록(`participants/me`): body `{decision: 'APPROVE'|'REJECT'|'COMMENT_ONLY', comment?}`. 참여자가 아니면 **역할과 무관하게 403**(PROJECT_ADMIN·시스템 ADMIN 포함). `decision: 'PENDING'` → 400. **CLOSED 사이클에 기록 → 400**. 재기록은 덮어쓰기(마지막 값 유지).
> - 닫기(`status`): 요청 body는 받지 않는다(무시됨 — 항상 CLOSED로만 전이, 재오픈 API 없음). 이미 CLOSED → 400. 대상 status는 변경하지 않는다.
> - 감사 로그(§4.15)에는 기록하지 않는다(03-data-model.md §3.21 구현 각주 참고).
>
> **[2026-10-05 구현 각주 — Phase 16, §4.18]** 컨트롤러 `com.lightalm.baseline.api.BaselineController`, 서비스 `BaselineCommandService`(생성) · `BaselineQueryService`(목록·상세) · `BaselineDiffService`(diff, 11-structure-migration-plan.md §2 목표 구조의 클래스명), 공용 컴포넌트 `BaselineSnapshotFactory`(현재 값 추출) · `BaselineSnapshotJsonCodec`(스냅샷 JSON 변환).
> - 생성 요청: `{name(필수, 최대 150), description?, itemRefs:[{targetType:'REQUIREMENT'|'ISSUE'|'TEST_CASE', targetId}](최소 1개)}`. 같은 `(targetType,targetId)` 중복은 1건으로 합친다(UNIQUE 위반 500 방지). 대상이 없거나 다른 프로젝트 소속이면 404이고 베이스라인 자체가 저장되지 않는다(전체 롤백).
> - POST(201)·GET 상세 응답 `BaselineDetailResponse`: `{id, name, description, createdById, createdByName, createdAt, items:[{id, targetType, targetId, snapshot:{필드:값}, capturedAt}]}`. `snapshot` 필드 집합은 03-data-model.md §3.21 구현 각주 참고.
> - GET 목록 응답: `[{id, name, description, createdById, createdByName, createdAt, itemCount}]`(`createdAt` 내림차순).
> - GET diff 응답 `BaselineDiffResponse`: `{baselineId, baselineName, baselineCreatedAt, comparedAt, unchangedCount, modifiedCount, deletedCount, items:[{targetType, targetId, key, title, changeType, changes:[{field, changeKind, before, after}]}]}`.
>   - `changeType`: `UNCHANGED`(모든 필드 동일, `changes` 빈 배열) / `MODIFIED`(값이 다른 필드가 1개 이상) / `DELETED`(대상이 삭제됐거나 더 이상 이 프로젝트 소속이 아님 — 스냅샷에서 값이 있던 모든 필드가 `REMOVED`로 나열됨).
>   - `changeKind`: `ADDED`(스냅샷엔 없음/null → 현재 값 있음) / `REMOVED`(값 있음 → 현재 없음/null) / `MODIFIED`(다른 값으로 변경). `before`/`after`는 JSON 값(문자열·숫자·null).
>   - `key`/`title`은 대상이 남아 있으면 현재 값, `DELETED`면 스냅샷 값.
> - diff는 저장하지 않고 호출할 때마다 다시 계산한다. 이 API들도 대상 엔티티를 변경하지 않는다(읽기 전용).

### 4.19 위험 관리 (Risk)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/risks` | 목록, 쿼리: `status,likelihood,impact` | VIEWER+ |
| POST | `/api/projects/{projectId}/risks` | 생성 `{title,description,likelihood,impact,mitigationPlan,ownerId}` | MEMBER+ |
| GET | `/api/projects/{projectId}/risks/{riskId}` | 상세(riskScore = likelihood × impact 계산값 포함) | VIEWER+ |
| PUT | `/api/projects/{projectId}/risks/{riskId}` | 수정 | MEMBER+ |
| PATCH | `/api/projects/{projectId}/risks/{riskId}/status` | 상태 변경 | MEMBER+ |

위험을 요구사항/이슈에 연결하는 것은 별도 API가 아니라 §4.7 추적성 링크 API를 그대로 사용한다(`source_type` 또는 `target_type`에 `'RISK'` 값을 허용, §3.22 참고).

### 4.20 요구사항 문서 뷰 & 변형 관리
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/requirements/document-view` | 요구사항을 상위/하위 순서(order_index) 기준 문서 목차 형태로 정렬해서 반환, 쿼리: `variantId`(지정 시 해당 변형에서 EXCLUDED인 항목 제외) | VIEWER+ |
| PATCH | `/api/projects/{projectId}/requirements/{reqId}/order` | 같은 부모 내 표시 순서 변경 `{orderIndex}` | MEMBER+ |
| GET | `/api/projects/{projectId}/variants` | 변형 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/variants` | 변형 생성 `{variantKey,name,description}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/requirements/{reqId}/variants/{variantId}` | 요구사항-변형 매핑 설정 `{applicability,note}` | MEMBER+ |

### 4.21 대시보드 위젯 & 리포트 내보내기
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/me/dashboard-widgets` | 내 대시보드 위젯 설정 조회(project_id 없는 것 = 개인화 대시보드용) | 로그인 사용자 |
| PUT | `/api/me/dashboard-widgets` | 위젯 설정 일괄 저장 `{widgets:[{widgetType,position,config}]}` | 로그인 사용자 |
| GET | `/api/projects/{projectId}/dashboard-widgets` | 프로젝트 대시보드 위젯 설정 조회 | VIEWER+ |
| PUT | `/api/projects/{projectId}/dashboard-widgets` | 프로젝트 대시보드 위젯 설정 저장 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/traceability/export?format=xlsx\|pdf` | 추적성 매트릭스 내보내기 | VIEWER+ |
| GET | `/api/projects/{projectId}/test-runs/{runId}/export?format=xlsx\|pdf` | 테스트 실행 결과 내보내기 | VIEWER+ |
| GET | `/api/projects/{projectId}/audit-logs/export?format=xlsx` | 감사 로그 내보내기 | PROJECT_ADMIN+ |

내보내기는 별도 저장 없이 요청 시점에 서비스 레이어가 실시간 생성해서 스트리밍 응답한다(Content-Disposition: attachment).

---

## v4 확장 API (2026-10-03~05, 03-data-model.md §3.25~3.30 참고, 아직 미구현 — ADR-011·ADR-012·ADR-014)

### 4.22 회원가입 (Self-Signup)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/auth/signup` | 회원가입 `{username,email,fullName,password,passwordConfirm}` — `systemRole`은 항상 `USER`, `enabled`은 항상 `true`로 서버가 고정. 가입 성공해도 자동 로그인하지 않음(별도로 `/api/auth/login` 호출 필요) | 공개 |

검증 규칙은 기존 `POST /api/users`(§4.3 `CreateUserRequest`)와 동일하게 맞춘다: `username`(NotBlank, max 50), `email`(NotBlank, Email, max 120), `fullName`(NotBlank, max 100), `password`(NotBlank, 8~100자), `passwordConfirm`(서비스 레이어에서 `password`와 일치 검증, 불일치 시 `400 VALIDATION_ERROR`).

라이센스 연동 — 가입 직전 `LicenseEnforcementService.requireActiveLicense()`/`requireSeatAvailable()` 통과 필요(§4.23 참고):

| 상황 | HTTP | error 코드 |
|---|---|---|
| 활성 라이센스 없음/만료 | 403 | `LICENSE_INVALID` |
| 시트 한도 초과(`enabled=true` 사용자 수 ≥ `seat_limit`) | 403 | `LICENSE_SEAT_LIMIT_EXCEEDED` |

### 4.23 라이센스 관리 (License)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/admin/licenses` | 라이센스 파일 업로드(multipart, `file` 파트) → 파싱(JSON)/HMAC-SHA256 서명 검증/적용. 기존 `ACTIVE` 행을 `SUPERSEDED`로 전환 후 신규 `ACTIVE` 삽입(단일 트랜잭션) | ADMIN |
| GET | `/api/admin/licenses/current` | 현재 활성 라이센스 상세 + 시트 사용량(`seatsUsed`,`seatsRemaining`) | ADMIN |
| GET | `/api/admin/licenses` | 업로드 이력(페이지네이션) | ADMIN |
| GET | `/api/public/license-status` | `{signupAllowed: boolean, reason: string\|null}` — 시트 수/라이센스 키 등 민감 정보는 내려주지 않음, `/signup` 화면에서 폼 노출 전 안내용 | 공개 |

업로드 실패 응답: `400 LICENSE_SIGNATURE_INVALID`(서명 불일치, 저장하지 않음), `400 LICENSE_ALREADY_EXPIRED`(업로드 시점 기준 이미 만료된 파일), `400 LICENSE_FILE_TOO_LARGE`(16KB 초과).

적용 게이트는 두 개로 분리된다(§게이트 A/B, 상세는 ADR-011 §2.3):
- **게이트 A(라이센스 유효성)**: 활성 라이센스가 없거나 만료 — 모든 비-ADMIN 로그인(로그인 성공 처리 직후 검사, 실패 시 세션 무효화 + `403 LICENSE_INVALID`) + 모든 신규 계정 생성(가입·관리자의 `POST /api/users` 양쪽 모두)에 적용. 시스템 `ADMIN`의 로그인은 예외(복구 경로).
- **게이트 B(시트 한도)**: `enabled=true` 사용자 수 ≥ `seat_limit` — 신규 계정 생성(가입·관리자 생성)에만 적용. 기존 로그인에는 영향 없음(라이센스를 더 작은 시트로 교체해도 이미 만들어진 계정은 계속 로그인 가능).

### 4.24 프로젝트 커스텀 필드 (Custom Field)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/custom-fields?targetType=` | 필드 정의 목록(ACTIVE+DEPRECATED, 설정화면용) | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/custom-fields` | 필드 생성 `{targetType,fieldKey,label,dataType,enumerationSetId?,required,defaultValue}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/config/custom-fields/{fieldId}` | 필드 수정(`label`/`required`/`defaultValue`/`displayOrder`만 — `dataType`/`fieldKey`는 생성 후 불변) | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/config/custom-fields/{fieldId}` | 소프트 삭제(`status=DEPRECATED`), 하드 삭제 없음 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/custom-fields?targetType=` | 활성 필드 정의만(생성/수정 폼 렌더링용) | VIEWER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/custom-field-values` | 대상의 커스텀 필드 값 전체 조회(대상 검증은 `PolymorphicTargetValidator` 재사용) | VIEWER+ |
| PUT | `/api/projects/{projectId}/{targetType}/{targetId}/custom-field-values` | 값 일괄 저장 `{values:[{fieldId,value}]}` | MEMBER+ |

### 4.25 프로젝트 폼 레이아웃 (Form Layout)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/form-layouts/{targetType}` | 레이아웃 조회(섹션+필드 트리, 설정화면용) | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/config/form-layouts/{targetType}` | 레이아웃 전체 치환(섹션/필드 순서 일괄 저장) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/form-layouts/{targetType}` | 폼 렌더링용 조회(미설정 시 표준 필드 기본 순서 반환 — 하위 호환) | VIEWER+ |

### 4.26 프로젝트 열거형 (Enumeration)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/enumerations` | 집합 목록 | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/enumerations` | 집합 생성 `{enumKey,baseEnum?,name}` — `baseEnum`은 `PRIORITY` 또는 생략만 허용(상태값 확장은 거부, §3.28 참고). **[2026-10-05 qa 반려 수정]** `enumKey=PRIORITY` ⇔ `baseEnum=PRIORITY`가 아니면 400(ADR-012 §C 각주) | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/enumerations/{id}/values` | 값 추가 `{valueKey,label,displayOrder}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/config/enumerations/{id}/values/{valueId}` | 값 수정 | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/config/enumerations/{id}/values/{valueId}` | 소프트 삭제(`is_system_default=true`면 거부) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/enumerations/{enumKey}/values` | 폼 렌더링용 활성 값 목록(커스텀 필드 SELECT 옵션, PRIORITY 드롭다운 등) | VIEWER+ |

### 4.27 프로젝트 워크플로우 전이 규칙 (Workflow Transition Rule)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/workflow-rules?targetType=` | 규칙 목록(없으면 "자유 전이 모드"임을 함께 응답) | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/workflow-rules` | 규칙 추가 `{targetType,fromStatus,toStatus,allowedRole?}` | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/config/workflow-rules/{ruleId}` | 규칙 삭제(전부 삭제 시 해당 target_type은 자유 전이 모드로 복귀) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/workflow-rules/{targetType}/{fromStatus}` | 특정 상태에서 전이 가능한 다음 상태 목록("상태 변경" 드롭다운 구성용) | VIEWER+ |

### 4.28 시스템 테마 설정 (Theme) — ADR-014
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/public/theme` | `{ colorPreset: "RED" }` — 색상 프리셋 코드만 반환(민감 정보 없음). 앱 부트스트랩이 비인증 상태로 호출 — `/login`·`/signup` 화면에도 동일하게 적용됨 | 공개 |
| GET | `/api/admin/theme-settings` | 현재 설정 상세(`colorPreset`,`updatedBy`,`updatedAt`) + 선택 가능한 프리셋 전체 목록(코드/라벨/대표 색상 — 관리자 화면 스와치 미리보기용) | ADMIN |
| PUT | `/api/admin/theme-settings` | 본문 `{ "colorPreset": "BLUE" }` — 프리셋 변경. `DEFAULT`/`RED`/`BLUE`/`GREEN`/`PURPLE` 외 값은 `400 VALIDATION_ERROR` | ADMIN |

라이트/다크 모드는 서버 API가 없다 — 클라이언트 전용(`localStorage`, 05-frontend.md §5.3·§5.23 참고). 변경 이력 조회 API는 두지 않는다(단일 설정의 현재값만 의미가 있음 — 과설계 방지, ADR-014 §4). 조회 행이 없는 극단 상황(마이그레이션 시드 실패 등)에서는 `GET /api/public/theme`이 500 대신 `DEFAULT`로 폴백한다(로그인 화면 자체가 깨지는 것을 방지).

기존 `PATCH .../requirements/{reqId}/status`·`PATCH .../issues/{issueId}/status`(§4.5·§4.6)는 변경되지 않지만, 내부적으로 `WorkflowTransitionPolicy.requireAllowedTransition(...)` 훅이 추가된다. 화이트리스트 모드가 아닌 프로젝트(규칙 미등록)는 기존과 동일하게 동작한다(하위 호환).
